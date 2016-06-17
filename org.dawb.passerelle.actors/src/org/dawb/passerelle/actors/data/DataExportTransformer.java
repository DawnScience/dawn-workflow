/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.actors.data;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dawb.common.util.io.FileUtils;
import org.dawb.common.util.io.IFileUtils;
import org.dawb.common.util.python.PythonUtils;
import org.dawb.passerelle.common.actors.AbstractDataMessageTransformer;
import org.dawb.passerelle.common.actors.AbstractPassModeTransformer;
import org.dawb.passerelle.common.io.DatWriter;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.IVariable.VARIABLE_TYPE;
import org.dawb.passerelle.common.message.IVariableProvider;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.passerelle.common.message.Variable;
import org.dawb.passerelle.common.parameter.ParameterUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.dawnsci.analysis.api.dataset.IDataset;
import org.eclipse.dawnsci.analysis.api.message.DataMessageComponent;
import org.eclipse.dawnsci.analysis.dataset.impl.Dataset;
import org.eclipse.dawnsci.hdf.object.HierarchicalDataFactory;
import org.eclipse.dawnsci.hdf.object.IHierarchicalDataFile;
import org.eclipse.dawnsci.hdf.object.Nexus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Settable;
import uk.ac.diamond.scisoft.analysis.io.DataHolder;
import uk.ac.diamond.scisoft.analysis.io.JavaImageSaver;

import com.isencia.passerelle.actor.InitializationException;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.actor.TerminationException;
import com.isencia.passerelle.util.ptolemy.ResourceParameter;
import com.isencia.passerelle.util.ptolemy.StringChoiceParameter;

/**
 * NOTE This is not a sink because it does output the file path.
 * 
 * @author gerring
 *
 */
public class DataExportTransformer extends AbstractDataMessageTransformer implements IVariableProvider{

	private static final Logger logger = LoggerFactory.getLogger(DataExportTransformer.class);
	protected static final List<String> WRITING_CHOICES;
	static {
		WRITING_CHOICES = new ArrayList<String>(3);
		WRITING_CHOICES.add("Append to file referenced by Output");
		WRITING_CHOICES.add("Replace file referenced by Output");
		WRITING_CHOICES.add("Create new file for each evaluation using ${file_name}");
		WRITING_CHOICES.add("Create new file using ${file_name} then use that for everything");
		WRITING_CHOICES.add("Create new file using ${file_name} overwrite it.");
	}
	protected static final List<String> FILE_TYPES;
	static {
		FILE_TYPES = new ArrayList<String>(3);
		FILE_TYPES.add("hdf5");
		FILE_TYPES.add("dat (ascii)");
		FILE_TYPES.add("dat (ascii) separate files");
		FILE_TYPES.add("jpg (8-bit)");
		FILE_TYPES.add("png (16-bit)");
		FILE_TYPES.add("tiff (16-bit)");
		FILE_TYPES.add("tiff (33-bit)");
	}
	protected static final List<String> CALIBRATION_TYPES;
	static {
		CALIBRATION_TYPES = new ArrayList<String>(3);
		CALIBRATION_TYPES.add("None");
	}
	
	private Parameter         fileFormatParam;
	private Parameter         fileWriteParam;
	private Parameter         calibParam;
	private Parameter         datasetName;
	private ResourceParameter filePathParam;
	private String            fileFormat, filePath, fileWriteType;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8257060666137254610L;

	public DataExportTransformer(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		super(container, name);
		
		fileFormatParam = new StringParameter(this,"File Format") {
			private static final long serialVersionUID = 3209529425483671733L;
			public String[] getChoices() {
				return FILE_TYPES.toArray(new String[FILE_TYPES.size()]);
			}
		};
		registerConfigurableParameter(fileFormatParam);
		fileFormatParam.setExpression(FILE_TYPES.get(0));

		fileWriteParam = new StringChoiceParameter(this, "Writing Type", WRITING_CHOICES, 1 << 2 /*SWT.SINGLE*/);
		registerConfigurableParameter(fileWriteParam);
		fileWriteParam.setExpression(WRITING_CHOICES.get(3));
		
		filePathParam = new ResourceParameter(this, "Output");
		filePathParam.setResourceType(IResource.FOLDER);
		filePathParam.setExpression("/${project_name}/output/");
		registerConfigurableParameter(filePathParam);
		
		calibParam = new StringParameter(this,"Calibration") {
			private static final long serialVersionUID = 7033872585443953608L;
			public String[] getChoices() {
				return CALIBRATION_TYPES.toArray(new String[CALIBRATION_TYPES.size()]);
			}
		};
		registerExpertParameter(calibParam);
        calibParam.setVisibility(Settable.NONE);
		calibParam.setExpression(CALIBRATION_TYPES.get(0));
		
		datasetName = new StringParameter(this, "Dataset Name");
		registerConfigurableParameter(datasetName);
		
		memoryManagementParam.setVisibility(Settable.NONE);
		dataSetNaming.setVisibility(Settable.NONE);
	}
	
	public void attributeChanged(Attribute attribute) throws IllegalActionException {

		if (attribute == fileFormatParam) {
			fileFormat = fileFormatParam.getExpression();
		} else if (attribute == filePathParam) {
			filePath = filePathParam.getExpression();
		} else if (attribute == fileWriteParam) {
			fileWriteType = fileWriteParam.getExpression();
			if (WRITING_CHOICES.get(0).equals(fileWriteType)||WRITING_CHOICES.get(1).equals(fileWriteType)) {
				filePathParam.setResourceType(IResource.FILE); 
			} else {
			    filePathParam.setResourceType(IResource.FOLDER);
			}
		}
		super.attributeChanged(attribute);
	}

	@Override
	protected DataMessageComponent getTransformedMessage(List<DataMessageComponent> cache) throws ProcessingException {
		
		String filePath = null;
		try {
			final Map<String,String> scalar = MessageUtils.getScalar(cache);
			final String fileName = scalar!=null ? scalar.get("file_name") : null;
			
			final Object  output   = getOutputPath(fileName);
			if (output instanceof File) {
				filePath = ((File)output).getAbsolutePath();
			} else {
				filePath = ((IFile)output).getLocation().toOSString();
			}
			
			final DataMessageComponent comp = new DataMessageComponent();
			comp.addScalar(MessageUtils.getScalar(cache));
			comp.putScalar("file_path", filePath);
			comp.putScalar("file_dir",  FileUtils.getDirectoryAbsolutePath(filePath));
			
			final File targetFile = new File(filePath);
			comp.putScalar("file_name", targetFile.getName());
			comp.putScalar("file_basename", FileUtils.getFileNameNoExtension(targetFile));

			// The write process may add scalars passed on.
			if (FILE_TYPES.get(1).equals(fileFormat) || FILE_TYPES.get(2).equals(fileFormat)) { // 
				writeAscii(filePath, cache, comp);
			} else if (FILE_TYPES.get(0).equals(fileFormat)) { // hdf5
				writeH5(filePath, cache, comp);
			} else { // Write an image
				writeImage(filePath, cache, comp);
			}

			if (output instanceof IResource) {
				AbstractPassModeTransformer.refreshResource((IResource)output);
			}
			
			return comp;
			
		} catch (Exception ne) {
			throw createDataMessageException("Cannot write to "+filePath, ne);
		} 
	}
	
	private void writeAscii(final String filePath, final List<DataMessageComponent> cache, final DataMessageComponent ret) throws Exception {
		
		final List<IDataset> sets = MessageUtils.getDatasets(cache);
	    final File  suggestedFile = new File(filePath);
		
		final Map<String,String>    scal = MessageUtils.getScalar(cache);
		final DatWriter writer = new DatWriter();
		writer.setMeta(scal);
		writer.setFile(suggestedFile);
		writer.setWriteIndex(false);

		final boolean isMultipleFiles = FILE_TYPES.get(2).equals(fileFormat);
		
		int i = 1;
		for (IDataset set : sets) {
			
			final String name = FileUtils.getLegalFileName(set.getName())+".dat"; // May cause an error
			if (isMultipleFiles) { // New file
			    final File  dir   = suggestedFile.getParentFile();
				final File   file = new File(dir, name);
				writer.setFile(file);
				
			}
			
			writer.addData(set);
			
			if (isMultipleFiles) { // New file
			    writer.write();
			    writer.clear();
				ret.putScalar("data_file_name"+i, name);
			}
			
			++i;
		}
		
		if (!isMultipleFiles) { // One file
			ret.putScalar("data_file_name", suggestedFile.getName());
		    writer.write();
		}
	}
	
	private void writeImage(final String filePath, final List<DataMessageComponent> cache, final DataMessageComponent ret) throws Exception {

		boolean wroteSomething = false;
		final List<IDataset> sets = MessageUtils.getDatasets(cache);
		
		int ifound = 1;

        for (IDataset set : sets) {
			
        	if (set.getShape().length==2) {
        		
        		final File parent = (new File(filePath)).getParentFile();
        		if (!parent.exists()) parent.mkdirs();
        		ret.putScalar("dir_path", parent.getAbsolutePath());
        		
        		final String dataName;
        		final File slice;
        		if (WRITING_CHOICES.get(1).equals(fileWriteParam.getExpression())||
        		    WRITING_CHOICES.get(4).equals(fileWriteParam.getExpression())) {
        			slice = new File(filePath);
        			final String name = slice.getName();
        			dataName = PythonUtils.getLegalVarName(name.substring(0, name.indexOf('0')), null);

        		} else if(WRITING_CHOICES.get(2).equals(fileWriteParam.getExpression()) ) {
        			slice = new File(filePath);

        		} else {
        			dataName = PythonUtils.getLegalVarName(set.getName(), null);
       			    slice = new File(parent, dataName);
        		}
        		String     slicePath = slice.getAbsolutePath();
        		if (slice.getName().indexOf('.')<0) slicePath = slicePath+"."+getExtension();
        				
        		final JavaImageSaver saver = new JavaImageSaver(slicePath, getExtension(), getBits(), true);
        		final DataHolder     dh    = new DataHolder();
        		dh.addDataset(set.getName(), set);
        		saver.saveFile(dh);
        		
        		ret.addList("image"+ifound, set);
        		++ifound;
        		
        		wroteSomething = true;
        	}
		}
        
        if (!wroteSomething) {
        	throw new Exception("No 2D data sets found! Cannot write "+fileFormat+" with this data!");
        }
	}

	private IHierarchicalDataFile cachedFile;
	
	private synchronized void writeH5(final String filePath, final List<DataMessageComponent> cache, final DataMessageComponent ret) throws Exception {

		IHierarchicalDataFile file = null;
		try {
			// Will create one if not there.
			if (isWritingSingleFile()) {
				if (cachedFile == null) {
					if (WRITING_CHOICES.get(1).equals(fileWriteType)) {
						final File iof = new File(filePath);
						if (iof.exists()) iof.delete();
					}
					cachedFile = HierarchicalDataFactory.getWriter(filePath);
				}
				file = cachedFile;
			} else {
			    file = HierarchicalDataFactory.getWriter(filePath);
			}
			
			final Map<String,String>       scal = MessageUtils.getScalar(cache);
			final List<IDataset>           sets = MessageUtils.getDatasets(cache);
			final Map<String,Serializable> rois = MessageUtils.getRois(cache);
			
			final String entry = file.group("entry");
			file.setNexusAttribute(entry, Nexus.ENTRY);

			if (scal!=null) {
				final String dict = file.group("dictionary", entry);
				file.setNexusAttribute(dict, Nexus.DATA);
				createStringEntries(file, dict, scal);
			}
			
			if (rois!=null && !rois.isEmpty()) {
				final String roiGroup = file.group("regions", entry);
				file.setNexusAttribute(roiGroup, Nexus.DATA);
				createStringEntries(file, roiGroup, rois);
			}


			final String datasetNameStr = getDatasetName(cache);
			boolean separateSets = (datasetNameStr==null || datasetNameStr.endsWith("/"));
			if (separateSets) {
				String group=entry;
				if (datasetNameStr!=null && datasetNameStr.endsWith("/")) {
					final String[]  paths = datasetNameStr.split("/");
					if (paths.length>0) {
						for (int i = 0; i < paths.length; i++) {
	                        final String path = paths[i];
							group = file.group(path, group);
							if (i<(paths.length-1)) file.setNexusAttribute(group, Nexus.ENTRY);
						}
					
					} else {
						group = file.group("data", entry);
					}
				} else {
					group = file.group("data", entry);
				}
				
				file.setNexusAttribute(group, Nexus.DATA);
				createDatasets(null, file, group, sets, true);
				
			} else {
				String group=entry;
				final String[]  path = datasetNameStr.split("/");
				if (path.length>2) {
					for (int i = 0; i < (path.length-2); i++) {
						group = file.group(path[i], group);
						file.setNexusAttribute(group, Nexus.ENTRY);
					}
					group = file.group(path[path.length-2], group);
					file.setNexusAttribute(group, Nexus.DATA);
				} else  if (path.length==2) {
					
					group = file.group(path[path.length-2], group);
					file.setNexusAttribute(group, Nexus.DATA);
					
				}
				
				final String name = path[path.length-1];
				createDatasets(name, file, group, sets, false);
			}
			
						
		} finally {
			try {
				if (file!=null) {
					if (!isWritingSingleFile()) {
						file.close();
					}
				}
			} catch (Exception e) {
				throw createDataMessageException("Cannot close "+filePath, e);
			}
		}
	}
	
	/**
	 * Saves string representations of ROIs
	 * @param file
	 * @param parent
	 * @param rois
	 * @throws Exception 
	 */
	private void createStringEntries(IHierarchicalDataFile file, String parent, Map<String, ? extends Serializable> entries) throws Exception {
		
		if (entries==null || entries.isEmpty()) return;
		for (String name : entries.keySet()) {
			final String s = file.createStringDataset(name, entries.get(name).toString(), parent);
			file.setNexusAttribute(s, Nexus.SDS);
		}
	}

	private void createDatasets(final String         name,
			                    final IHierarchicalDataFile file, 
			                    final String          group,
			                    final List<IDataset> sets, 
			                    boolean              isCreate) throws Exception {
		
		if (sets!=null) for (IDataset set : sets) {
			if (set == null) continue;
			final String s;
			if (isCreate) {
			    s = file.createDataset(set.getName(),  set, group);
			} else {
				s = file.appendDataset(name,  set, group);
			}
			file.setNexusAttribute(s, Nexus.SDS);
		}			
	}

	private String getDatasetName(final List<DataMessageComponent> cache) throws Exception {
		return ParameterUtils.getSubstituedValue(datasetName, cache);
	}
	
	private boolean isWritingSingleFile() {
		this.fileWriteType = fileWriteParam.getExpression();
		if (WRITING_CHOICES.get(0).equals(fileWriteType) || WRITING_CHOICES.get(1).equals(fileWriteType) || (WRITING_CHOICES.get(3).equals(fileWriteType))) { 
            return true;
		}
        return false;
	}

	private String getOutputPath() throws Exception {
		
		Object path = null;
		// Attempt to read file_name from previous nodes, if it will read.
		final List<IVariable> vars = getInputVariables();
		for (IVariable input : vars) {
			if (input != null && input.getVariableName().equals("file_name")) {
				path = getOutputPath(input.getExampleValue().toString());
				break;
			}
		}

		if (path==null) path = getOutputPath("new_data_file.h5");
		if (path instanceof File) {
			return ((File)path).getAbsolutePath();
		} else {
			return ((IFile)path).getLocation().toOSString();
		}
	}

	public void doPreInitialize() throws InitializationException {
	  super.doPreInitialize();
		fileWritingTo=null;
	}

	private IFile fileWritingTo;

	private Object getOutputPath(String fileName) throws Exception {
				
		if (fileName==null) fileName = "new_data_file.h5";
		this.fileWriteType = fileWriteParam.getExpression();
		this.filePath = filePathParam.getExpression();
		this.filePath = ParameterUtils.substitute(filePath, this);
				
		if (WRITING_CHOICES.get(0).equals(fileWriteType) || WRITING_CHOICES.get(1).equals(fileWriteType)) { //Append to file referenced by Output
			IFile file = (IFile)ResourcesPlugin.getWorkspace().getRoot().findMember(filePath, true);
			if (file == null) {
				
				// Make directories
				try {
					final File fullFile = new File(getProject().getParent().getLocation().toOSString()+"/"+filePath);
					if (!fullFile.exists() && !fullFile.getParentFile().exists()) {
						fullFile.getParentFile().mkdirs();
						fullFile.createNewFile();
						getProject().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
					}

				} catch (Exception ne) {
					logger.trace("Error refreshing", ne);
					// Just carry on.
				}
				
				final File f = new File(filePath);
				final String name = f.getName();
				final String path = f.getParent();
				
	            IContainer cont = IFileUtils.getContainer(path, getProject().getName(), "output");
				if (cont instanceof IProject) {
					file = ((IProject)cont).getFile(name);
							
				} else {
					file = ((IFolder)cont).getFile(name);
				}
			}

			return file;
		
		} else if (WRITING_CHOICES.get(2).equals(fileWriteType)) { //Create new file for each evaluation using ${file_name}
			
			if (fileName==null) throw createDataMessageException("Inputs to '"+getName()+"' must contain scalar value 'file_name' to determine h5 output name.", null);
			final String rootName = fileName.substring(0, fileName.lastIndexOf("."));
            final IContainer folder = IFileUtils.getContainer(filePath, getProject().getName(), "output");
			      IFile      file   = folder instanceof IFolder
					                ? ((IFolder)folder).getFile(rootName)
					                : ((IProject)folder).getFile(rootName);
            if (file.exists()) file = IFileUtils.getUniqueIFile(folder, rootName, getExtension());
			return file;
			
		} else if (WRITING_CHOICES.get(3).equals(fileWriteType)||
				   WRITING_CHOICES.get(4).equals(fileWriteType)) { //Create new file using ${file_name} then use that for everything
			
			if (fileName==null) throw createDataMessageException("Inputs to '"+getName()+"' must contain scalar value 'file_name' to determine h5 output name.", null);
			
			try {
				final File dir = new File(filePath);
				if (dir.exists() && dir.isDirectory() && ResourcesPlugin.getWorkspace().getRoot().findMember(filePath, true)==null) {
					final String rootName = fileName.indexOf('.')>-1
		                      ? fileName.substring(0, fileName.lastIndexOf("."))
		                      : fileName;
					return FileUtils.getUnique(dir, rootName, getExtension());
				}
			} catch (Throwable ne) {
				// Ignored
			}
			
			if (fileWritingTo == null) {
				final String rootName = fileName.indexOf('.')>-1
				                      ? fileName.substring(0, fileName.lastIndexOf("."))
				                      : fileName;
	            IContainer folder = IFileUtils.getContainer(filePath, getProject().getName(), "output");
	            fileWritingTo     = IFileUtils.getUniqueIFile(folder, rootName, getExtension());
			}
			
			if (WRITING_CHOICES.get(4).equals(fileWriteType) && fileWritingTo.exists()) {
				fileWritingTo.delete(true, new NullProgressMonitor());
			}
			
			return fileWritingTo;
		}
		
		return null;
	}


	private String getExtension() {
		this.fileFormat = fileFormatParam.getExpression();
		if (FILE_TYPES.get(0).equals(fileFormat)) return "h5";
		if (FILE_TYPES.get(1).equals(fileFormat)) return "dat";
		if (FILE_TYPES.get(2).equals(fileFormat)) return "dat";
		if (FILE_TYPES.get(3).equals(fileFormat)) return "jpg";
		if (FILE_TYPES.get(4).equals(fileFormat)) return "png";
		if (FILE_TYPES.get(5).equals(fileFormat)) return "tiff";
		if (FILE_TYPES.get(6).equals(fileFormat)) return "tiff";
		return "h5";
	}
	

	private int getBits() {
		if (FILE_TYPES.get(3).equals(fileFormat)) return 8;
		if (FILE_TYPES.get(4).equals(fileFormat)) return 16;
		if (FILE_TYPES.get(5).equals(fileFormat)) return 16;
		if (FILE_TYPES.get(6).equals(fileFormat)) return 33;
		return 16;
	}


	@Override
	protected String getExtendedInfo() {
		return "Actor writes file to required format and then outputs file written path";
	}

	@Override
	public List<IVariable> getOutputVariables() {
		
		final List<IVariable> ret = super.getOutputVariables();
		try {
			// In ascii mode, the data set names get variables for their names
			final List<IVariable> in = getInputVariables();
			if (FILE_TYPES.get(1).equals(fileFormatParam.getExpression())) {
				int ifound = 1;
				if (in!=null) for (IVariable iVariable : in) {
					if (iVariable.getVariableType()==VARIABLE_TYPE.ARRAY) {
						ret.add(new Variable("data_file_name"+ifound, VARIABLE_TYPE.SCALAR, iVariable.getVariableName()+".dat", String.class));
						ifound++;
					}
				}
			} else if (FILE_TYPES.get(2).equals(fileFormatParam.getExpression())) {
				ret.add(new Variable("data_file_name", VARIABLE_TYPE.SCALAR, "${file_name}.dat", String.class));
			
			} else if (!FILE_TYPES.get(0).equals(fileFormatParam.getExpression())) {
				if (in!=null) for (IVariable iVariable : in) {
					int ifound = 1;
					if (iVariable.getVariableType()==VARIABLE_TYPE.ARRAY) {
						ret.add(new Variable("image"+ifound, VARIABLE_TYPE.ARRAY, iVariable.getVariableName(), String.class));
						ifound++;
					}
				}
			}
			
			ret.add(new Variable("file_path", VARIABLE_TYPE.PATH, getOutputPath(), String.class));
			ret.add(new Variable("file_name", VARIABLE_TYPE.SCALAR, new File(getOutputPath()).getName(), String.class));
			ret.add(new Variable("file_dir",  VARIABLE_TYPE.PATH, FileUtils.getDirectoryAbsolutePath(getOutputPath()), String.class));
			ret.add(new Variable("file_basename",  VARIABLE_TYPE.PATH, FileUtils.getFileNameNoExtension(new File(getOutputPath())), String.class));
			return ret;
			
		} catch (Exception ne) {
			logger.error("Cannot get output folder for "+getName(), ne);
			return ret;
		}
	}

	@Override
	protected String getOperationName() {
		return "export";
	}

	protected void doWrapUp() throws TerminationException {
		if (cachedFile!=null) {
			try {
				cachedFile.close();
			} catch (Exception e) {
				logger.error("Unable to close cached hdf5 file!", e);
			}
			cachedFile = null;
		}
		super.doWrapUp();
	}
}
