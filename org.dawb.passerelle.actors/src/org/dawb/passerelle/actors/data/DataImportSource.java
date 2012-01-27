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
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dawb.common.python.PythonUtils;
import org.dawb.common.ui.slicing.DimsDataList;
import org.dawb.common.ui.slicing.SliceUtils;
import org.dawb.common.util.io.FileUtils;
import org.dawb.common.util.io.SortingUtils;
import org.dawb.gda.extensions.loaders.H5LazyDataset;
import org.dawb.gda.extensions.loaders.H5Loader;
import org.dawb.gda.extensions.util.DatasetTitleUtils;
import org.dawb.passerelle.actors.data.config.SliceParameter;
import org.dawb.passerelle.common.DatasetConstants;
import org.dawb.passerelle.common.actors.AbstractDataMessageSource;
import org.dawb.passerelle.common.message.AbstractDatasetProvider;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.DataMessageException;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.IVariable.VARIABLE_TYPE;
import org.dawb.passerelle.common.message.IVariableProvider;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.passerelle.common.message.Variable;
import org.dawb.passerelle.common.parameter.ParameterUtils;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.gmf.runtime.common.core.util.StringMatcher;
import org.eclipse.swt.SWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.data.BooleanToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Settable;
import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.ILazyDataset;
import uk.ac.diamond.scisoft.analysis.io.DataHolder;
import uk.ac.diamond.scisoft.analysis.io.IMetaData;
import uk.ac.diamond.scisoft.analysis.io.LoaderFactory;
import uk.ac.diamond.scisoft.analysis.io.SliceObject;
import uk.ac.gda.util.map.MapUtils;

import com.isencia.passerelle.actor.InitializationException;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.actor.TerminationException;
import com.isencia.passerelle.core.PasserelleException;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.MessageException;
import com.isencia.passerelle.message.MessageFactory;
import com.isencia.passerelle.util.EnvironmentUtils;
import com.isencia.passerelle.util.ptolemy.IAvailableChoices;
import com.isencia.passerelle.util.ptolemy.IAvailableMap;
import com.isencia.passerelle.util.ptolemy.RegularExpressionParameter;
import com.isencia.passerelle.util.ptolemy.ResourceParameter;
import com.isencia.passerelle.util.ptolemy.StringChoiceParameter;
import com.isencia.passerelle.util.ptolemy.StringMapParameter;
import com.isencia.passerelle.workbench.model.actor.IResourceActor;
import com.isencia.passerelle.workbench.model.actor.ResourceObject;
import com.isencia.passerelle.workbench.model.utils.ModelUtils;
import com.isencia.util.StringConvertor;

/**
 * Reads a file or directory using LoaderFactory and makes the data available to subsequent nodes
 * which can request certain data sets for use.
 * 
 * @author gerring
 *
 */
public class DataImportSource extends AbstractDataMessageSource implements IResourceActor, IVariableProvider {

	private static final Logger logger = LoggerFactory.getLogger(DataImportSource.class);
	static {
		try {
			H5Loader.setLoaderInFactory();
		} catch (Exception e) {
			logger.error("Cannot replace H5Loader", e);
		}
	}
	
	
	private static final String[] DATA_TYPES = new String[] {"Complete data as numerical arrays", "Just path and file name"};
	
	// Read internally
	protected Parameter           folderParam;

	protected Parameter           relativePathParam;
	protected boolean             isPathRelative = true;
	
	protected Parameter           metaParam;
	protected boolean             isMetaRequired = false;
	
	private final RegularExpressionParameter filterParam;
	private String                fileFilter;

	private final ResourceParameter       path;
	private final StringChoiceParameter   names;
	private final StringMapParameter      rename;
	private final StringParameter         dataType;
	private final SliceParameter          slicing;
	
	private List<TriggerObject> fileQueue;

 
	/**
	 * 
	 */
	private static final long serialVersionUID = -851384753061854424L;
	
	public DataImportSource(CompositeEntity container, String name) throws IllegalActionException, NameDuplicationException {
        this(container, name, false);
	}
	
	/**
	 * 
	 * @param container
	 * @param name
	 * @throws IllegalActionException
	 * @throws NameDuplicationException
	 */
	protected DataImportSource(CompositeEntity container, String name, boolean isFolder) throws IllegalActionException, NameDuplicationException {
		
		super(container, ModelUtils.findUniqueActorName(container, name));
		
		relativePathParam = new Parameter(this, "Relative Path", new BooleanToken(true));
		registerConfigurableParameter(relativePathParam);

		folderParam = new Parameter(this, "Folder", new BooleanToken(isFolder));
		folderParam.setVisibility(Settable.NONE);
		
		metaParam = new Parameter(this, "Include Meta Data", new BooleanToken(false));
		registerConfigurableParameter(metaParam);
		
		filterParam  = new RegularExpressionParameter(this, "File Filter", true);
		registerConfigurableParameter(filterParam);

		path = new ResourceParameter(this, "Path", "Data files", LoaderFactory.getSupportedExtensions().toArray(new String[0]));
		try {
			URI baseURI = new File(StringConvertor.convertPathDelimiters(EnvironmentUtils.getApplicationRootFolder())).toURI();
			path.setBaseDirectory(baseURI);
		} catch (Exception e) {
			logger.error("Cannot set base directory for "+getClass().getName(), e);
		}
		registerConfigurableParameter(path);

		names = new StringChoiceParameter(this, "Data Sets", new IAvailableChoices() {		
			@Override
			public String[] getChoices() {
				return getAllDatasetsInFile();
			}
			@Override
			public Map<String,String> getVisibleChoices() {
				getAllDatasetsInFile();
			    return DatasetTitleUtils.getChoppedNames(cachedDatasets);
			}
		}, SWT.MULTI);
		
		registerConfigurableParameter(names);
		
		rename = new StringMapParameter(this, "Rename Data Sets", new IAvailableMap() {		
			@Override
			public Map<String,String> getMap() {
				return getDataSetsRenameName();
			}
			@Override
			public Map<String,String> getVisibleKeyChoices() {
				getAllDatasetsInFile();
			    return DatasetTitleUtils.getChoppedNames(cachedDatasets);
			}
			@Override
			public Collection<String> getSelectedChoices() {		
                final String[] ds     = names.getValue();
				return ds!=null ? Arrays.asList(ds) : null;
			}
		});
		
		registerConfigurableParameter(rename);
		
		slicing = new SliceParameter(this, "Data Set Slice");
		registerConfigurableParameter(slicing);

		
		dataType = new StringParameter(this,"Data Type") {
			public String[] getChoices() {
				return DATA_TYPES;
			}
		};
		dataType.setExpression(DATA_TYPES[0]);
		registerConfigurableParameter(dataType);
		
	}
	
	/**
	 *  @param attribute The attribute that changed.
	 *  @exception IllegalActionException   */
	public void attributeChanged(Attribute attribute) throws IllegalActionException {

		if(logger.isTraceEnabled()) logger.trace(getInfo()+" :"+attribute);
		if (attribute == path) {
			cachedDatasets = null;
			cachedShapes   = null;
		} else if (attribute == relativePathParam) {
			isPathRelative = ((BooleanToken)relativePathParam.getToken()).booleanValue();
		} else if (attribute == metaParam) {
			isMetaRequired = ((BooleanToken)metaParam.getToken()).booleanValue();
		} else if (attribute == folderParam) {
			// You cannot change this, it is set in the constuctor and is fixed.
		} else if (attribute == filterParam) {
			fileFilter = filterParam.getExpression();
		} else if (attribute == names) {
			logger.trace("Data set names changed to : " + names.getExpression());
			
		}
		
		super.attributeChanged(attribute);
	}
	
	@Override
	public void doPreInitialize() {
		fileQueue      = null;
		cachedDatasets = null;
		cachedShapes   = null;
	}

	@Override
	protected void doInitialize() throws InitializationException {
	
		super.doInitialize();
		
		fileQueue = new ArrayList<TriggerObject>(89);
		if (!isTriggerConnected()) {
		    appendQueue(null); // Otherwise the trigger will call create on the iterator.
		}
	}
	
	/**
	 * triggerMsg may be null
	 * @param triggerMsg
	 */
	private void appendQueue(final ManagedMessage triggerMsg) {
		
		if (getManager()!=null) {
			final File file = new File(getSourcePath(triggerMsg));
			if (file.isDirectory()) {
				final List<File> fileList = SortingUtils.getSortedFileList(file);
				for (File f : fileList) {				
					if (!isFileLegal(f)) continue;
					final TriggerObject ob = new TriggerObject();
					ob.setTrigger(triggerMsg);
					ob.setFile(f);
					fileQueue.add(ob);
				}
			} else {
				if (isFileLegal(file)) {
					
					if (isH5Slicing(triggerMsg)) {
						try {
							final IMetaData meta  = LoaderFactory.getMetaData(file.getAbsolutePath(), null);
	                        final int[]    shape  = meta.getDataShapes().get(getDataSetNames()[0]);
							final List<SliceObject> slices = SliceUtils.getExpandedSlices(shape, slicing.getBeanFromValue(DimsDataList.class));
						    int index = 0;
							for (SliceObject sliceObject : slices) {
								final TriggerObject ob = new TriggerObject();
								ob.setTrigger(triggerMsg);
								ob.setFile(file);
								ob.setSlice(sliceObject);
								ob.setIndex(index);
								fileQueue.add(ob);
								index++;
							}
							
						} catch (Exception ne) {
							// This is the end!
							logger.error("Problem reading slices in data import.", ne);
							requestFinish();
						}
					} else {
						final TriggerObject ob = new TriggerObject();
						ob.setTrigger(triggerMsg);
						ob.setFile(file);
						fileQueue.add(ob);
					}
				}
			}
		}
	}
	
	private boolean isH5Slicing(final ManagedMessage triggerMsg) {
		final String  ext      = FileUtils.getFileExtension(getSourcePath(triggerMsg));
		final boolean isH5File = LoaderFactory.getLoaderClass(ext) == H5Loader.class;
		
		return isH5File && this.slicing.getExpression()!=null && getDataSetNames()!=null && getDataSetNames().length==1;
	}

	private boolean isFileLegal(File file) {
		
		if (file.isDirectory())                  return false;
		if (file.isHidden())                     return false;
		if (file.getName().startsWith("."))      return false;
	    if (!isRequiredFileName(file.getName())) return false;		   
        return true;
	}

	public boolean hasNoMoreMessages() {
	    if (fileQueue == null)   return true;
        return fileQueue.isEmpty() && super.hasNoMoreMessages();
    }
	
	protected ManagedMessage getDataMessage() throws ProcessingException {
		
		if (fileQueue == null)   return null;
		if (fileQueue.isEmpty()) return null;
		
		if (isFinishRequested()) {
			fileQueue.clear();
			return null;
		}
		
		ManagedMessage msg = MessageFactory.getInstance().createMessage();
		final TriggerObject file = fileQueue.remove(0);
		try {
			msg.setBodyContent(getData(file), DatasetConstants.CONTENT_TYPE_DATA);
	
		} catch (MessageException e) {
			logger.error("Cannot set map of data in message body!", e);
			msg = MessageFactory.getInstance().createErrorMessage(new PasserelleException("Cannot set map of data in message body!", "application/x-data", e));
			fileQueue.clear();

		} catch (Exception ne) {
			fileQueue.clear();
			throw new DataMessageException("Cannot read data from '"+getSourcePath(msg)+"'", this, ne);
		}
			
		try {
			msg.setBodyHeader("TITLE", file.getFile().getName());
		} catch (MessageException e) {
			msg = MessageFactory.getInstance().createErrorMessage(new PasserelleException("Cannot set header in message!", "application/x-data", e));
		}

		return msg;
	}

	protected void doWrapUp() throws TerminationException {
		super.doWrapUp();
		if (isFinishRequested()) {
			fileQueue.clear();
			if (cachedDatasets!=null) cachedDatasets.clear();
			if (cachedShapes!=null)   cachedShapes.clear();
		}
	}
	
    private boolean isRequiredFileName(String fileName) {
    	if (fileFilter==null || "".equals(fileFilter)) return true;
		if (filterParam.isJustWildCard()) {
			final StringMatcher matcher = new StringMatcher(fileFilter, false, false);
		    return matcher.match(fileName);
		} else {
			return fileName.matches(fileFilter);
		}
	}

	private Collection<String>  cachedDatasets = null;	
	private Map<String,int[]>   cachedShapes   = null;	
	
    protected String[] getAllDatasetsInFile() {
		if (cachedDatasets==null && getSourcePath(null)!=null) {
			try {
				String path = getSourcePath(null);
				File   file = new File(path);
				if (!file.exists()) return null;
				
				// For directories we assume that all files contain the same data sets.
				if (file.isDirectory()) {
					final File[] files = file.listFiles();
					for (int i = 0; i < files.length; i++) {
						if (!files[i].isDirectory()) {
							file = files[i];
							break;
						}
					}
				}
				
				final IMetaData meta  = LoaderFactory.getMetaData(file.getAbsolutePath(), null);
				if (meta!=null && meta.getDataNames()!=null) {
				    cachedDatasets = meta.getDataNames();
				    cachedShapes   = meta.getDataShapes();
				}
			} catch (Exception e) {
				logger.error("Cannot get data set names from "+getSourcePath(null), e);
				cachedDatasets = Collections.emptyList();
				cachedShapes   = Collections.emptyMap();
			}
		}
		if (cachedDatasets!=null&&!cachedDatasets.isEmpty()) {
			return cachedDatasets.toArray(new String[cachedDatasets.size()]);
		}
		return null;
	}

	private Map<String, String> getDataSetsRenameName() {
		
		final String[]           sets = getAllDatasetsInFile();
		if (sets == null || sets.length<1) return null;
		
		String rootName = DatasetTitleUtils.getRootName(cachedDatasets);
        if (rootName==null) rootName = "";
        
		final Map<String,String> ret  = new LinkedHashMap<String,String>(7);
		for (String setName : sets) {
			try {
				ret.put(setName, PythonUtils.getLegalVarName(setName.substring(rootName.length()), ret.values()));
			} catch (Exception e) {
				ret.put(setName, setName);
			}
		}
		
		if (rename.getExpression()==null) {
			return ret;
		}
		
		final Map<String,String> existing = MapUtils.getMap(rename.getExpression());
		if (existing!=null) {
			existing.keySet().retainAll(ret.keySet());
			ret.putAll(existing);
		}
		
		return ret;
	}
	
	protected DataMessageComponent getData(final TriggerObject trigOb) throws Exception {
		
		final File                 file      = trigOb.getFile();
		final ManagedMessage       triggerMsg= trigOb.getTrigger();
		final String               filePath  = file.getAbsolutePath();
        
		final Map<String,Serializable>   datasets;
		if (trigOb.getSlice()!=null) {
			final SliceObject slice = trigOb.getSlice();
			slice.setPath(file.getAbsolutePath());
			slice.setName(getDataSetNames()[0]);
			
			final String sliceName = getDataSetNames()[0]+"_slice_"+trigOb.getIndex();
			final String pyName    = PythonUtils.getLegalVarName(sliceName, null);
				
			final AbstractDataset set = SliceUtils.getSlice(slice, null);
			set.setName(pyName);
			datasets = new HashMap<String,Serializable>(1);
			datasets.put(pyName, set);
			
		} else {
			datasets = getDatasets(filePath);
		}
		
		final DataMessageComponent comp = new DataMessageComponent();
		// Add messages from upsteam, if any.
		if (triggerMsg!=null) {
			try {
				final DataMessageComponent c = MessageUtils.coerceMessage(triggerMsg);
			    comp.addScalar(c.getScalar());
			} catch (Exception ignored) {
				logger.info("Trigger for "+getName()+" is not DataMessageComponent, no data added.");
			}
		}
		
		if (datasets!=null) comp.setList(datasets);
		
		if (isMetaRequired) {
			IMetaData meta =  LoaderFactory.getMetaData(filePath, null);
			comp.setMeta(meta);
		}
		comp.putScalar("file_path", filePath);
		comp.putScalar("file_name", new File(filePath).getName());
		comp.putScalar("file_dir",  FileUtils.getDirectory(filePath));
		
		
		return comp;

	}
	
	/**
	 * 
	 */
	public String[] getDataSetNames() {
		return names.getValue();
	}
	
	private Map<String,Serializable> getDatasets(String filePath) throws Exception {
		
		if (!DATA_TYPES[0].equals(dataType.getExpression())) return null; 
		
		final String[] ds     = names.getValue();

		// TODO Eclipse progress?
		Map<String, ILazyDataset> data = null;
		if (ds!=null&&ds.length>0) {
			data = LoaderFactory.getDataSets(filePath, Arrays.asList(ds), null);
		}
		
		if (data == null) {
			DataHolder dh = LoaderFactory.getData(filePath, null);
			data = dh.getMap();
			if (ds!=null&&ds.length>0) {
				data.keySet().retainAll(Arrays.asList(ds));
			}
		}
		
		final Map<String, Object> raw;
		if (ds==null) {
			raw = new LinkedHashMap<String, Object>();
			raw.putAll(data);
		} else {
			raw = new LinkedHashMap<String, Object>();
			for (String name : ds) {
				raw.put(name, data.get(name));
			}
		}
		
		
		final Map<String,String> nameMap = getDataSetNameMap();
		final Map<String,Serializable> ret = new HashMap<String,Serializable>(raw.size());
		
		// Set name and not to print data in string
		for (String name : raw.keySet()) {

			final ILazyDataset lazy = (ILazyDataset)raw.get(name);
			if (lazy==null) continue;
			
			if (nameMap!=null) {
				final String newName = nameMap.get(name);
				if (newName!=null && !"".equals(newName)) {
					name = newName;
				}
			}

			/**
			 * We load the data, this is an import actor
			 */
			final AbstractDataset set = getLoadedData(lazy);
			set.setStringPolicy(AbstractDataset.STRING_SHAPE);
			set.setName(name);
			
			ret.put(name, set);

		}
		
	    return ret;
	}

	private AbstractDataset getLoadedData(ILazyDataset lazy) throws Exception {
		
		if (lazy instanceof H5LazyDataset) {
		    return ((H5LazyDataset)lazy).getCompleteData(null);
		}
		return (AbstractDataset)lazy;
	}

	private Map<String, String> getDataSetNameMap() {
		final String map = this.rename.getExpression();
		if (map == null) return null;
		final Map<String,String> nameMap = MapUtils.getMap(map);
		if (nameMap==null || nameMap.isEmpty()) return null;
		return nameMap;
	}
	
	@Override
	protected String getExtendedInfo() {
		return "A source which uses  the GDA 'LoaderFactory' to read a DataHandler which can be used to access data";
	}
	
	public String getSourcePath() {
		return getSourcePath(null);
	}

	private String getSourcePath(final ManagedMessage manMsg) {

		String sourcePath=null;
		try {
			final DataMessageComponent comp = manMsg!=null ? MessageUtils.coerceMessage(manMsg) : null;
			sourcePath = ParameterUtils.getSubstituedValue(this.path, comp);
		} catch (Exception e) {
			logger.error("Cannot substitute parameter "+path, e);
		}

		if (isPathRelative) {
			try {
				final IResource res = ResourcesPlugin.getWorkspace().getRoot().findMember(sourcePath, true);
				if (res==null) return  null;
				sourcePath = res.getLocation().toOSString();
			} catch (NullPointerException ne) {
				return null;
			}
		}
		
        getStandardMessageHeaders().put(ManagedMessage.SystemHeader.HEADER_SOURCE_INFO,sourcePath);

		return sourcePath;
	}
	
	private Object getResource() {
		
		if (isPathRelative) {
			String sourcePath = this.path.getExpression();
			try {
				sourcePath = ModelUtils.substitute(sourcePath, this);
			} catch (Exception e) {
				logger.error("Cannot ret resource "+sourcePath, e);
				return null;
			}

			return ResourcesPlugin.getWorkspace().getRoot().findMember(sourcePath, true);
		} else {
			return new File(getSourcePath(null));
		}
	}
	
	private String getResourceTypeName() {
		final File file = new File(getSourcePath(null));
		return file!=null ? "'"+file.getName()+"'" : "";
	}
		
	@Override
	public int getResourceCount() {
		return 1;
	}
	
	@Override
	public ResourceObject getResource(int num) {
		if (num==0) {
			final ResourceObject ret = new ResourceObject();
			ret.setResource(getResource());
			ret.setResourceTypeName(getResourceTypeName());
			return ret;
		}
		return null;
	}
	
	@Override
	public void setMomlResource(IResource momlFile) {
		// Do nothing
	}
	
	@Override
	public List<IVariable> getOutputVariables() {
		
		final List<IVariable> ret = new ArrayList<IVariable>(7);
		if (getSourcePath(null)==null)  {
			final String msg = "Invalid Path '"+path.getExpression()+"'";
			ret.add(new Variable("file_path", VARIABLE_TYPE.PATH,   msg, String.class));
			ret.add(new Variable("file_name", VARIABLE_TYPE.SCALAR, msg, String.class));
			ret.add(new Variable("file_dir",  VARIABLE_TYPE.PATH,   msg, String.class));
			return ret;
		}
		
		ret.add(new Variable("file_path", VARIABLE_TYPE.PATH, getSourcePath(null), String.class));
		ret.add(new Variable("file_name", VARIABLE_TYPE.SCALAR, new File(getSourcePath(null)).getName(), String.class));
		ret.add(new Variable("file_dir",  VARIABLE_TYPE.PATH, FileUtils.getDirectory(getSourcePath(null)), String.class));
		
		if (DATA_TYPES[0].equals(dataType.getExpression())) {
			
			getAllDatasetsInFile();// Sets up cache of sizes, which means we can return VARIABLE_TYPE.IMAGE
			
			String[] ds     = names.getValue();
			if (ds==null||ds.length<1) ds = getAllDatasetsInFile();
			if (ds!=null&&ds.length>0) {
				for (int i = 0; i < ds.length; i++) {
					final Map<String,String> varNames = getDataSetNameMap();
					final String name  = varNames!=null&&varNames.containsKey(ds[i])
					                   ? varNames.get(ds[i])
					                   : ds[i];
					if (cachedShapes!=null) {
						final int[] shape = cachedShapes.get(ds[i]);
						final VARIABLE_TYPE type = shape!=null&&shape.length==2
						                         ? VARIABLE_TYPE.IMAGE
						                         : VARIABLE_TYPE.ARRAY;
						final AbstractDatasetProvider example = new AbstractDatasetProvider(shape);
					    ret.add(new Variable(name, type, example, AbstractDataset.class));
	
					} else {
					    ret.add(new Variable(name, VARIABLE_TYPE.ARRAY, new AbstractDatasetProvider(), AbstractDataset.class));
					}
				}
			}
		}
		
		return ret;
	}
	
	private boolean triggeredOnce = false;

	@Override
	protected boolean mustWaitForTrigger() {
		if (!isTriggerConnected()) return false;
		if (!triggeredOnce)        return true;
		return fileQueue.isEmpty();
	}
	
	/**
	 * "callback"-method that can be overridden by TriggeredSource implementations,
	 * if they want to act e.g. on the contents of a received trigger message.
	 * 
	 * @param triggerMsg
	 */
	protected void acceptTriggerMessage(ManagedMessage triggerMsg) {
		triggeredOnce = true;
		appendQueue(triggerMsg);
	}

}
