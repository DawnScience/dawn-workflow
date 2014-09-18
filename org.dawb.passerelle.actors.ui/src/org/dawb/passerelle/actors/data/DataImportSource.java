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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import ncsa.hdf.object.Dataset;

import org.dawb.common.python.PythonUtils;
import org.dawb.common.util.io.FileUtils;
import org.dawb.common.util.io.SortingUtils;
import org.dawb.passerelle.actors.data.config.ISliceInformationProvider;
import org.dawb.passerelle.actors.data.config.SliceParameter;
import org.dawb.passerelle.common.DatasetConstants;
import org.dawb.passerelle.common.actors.AbstractDataMessageSource;
import org.dawb.passerelle.common.actors.ActorUtils;
import org.dawb.passerelle.common.message.DataMessageException;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.IVariableProvider;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.passerelle.common.parameter.ParameterUtils;
import org.dawnsci.io.h5.H5Loader;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.dawnsci.hdf5.HierarchicalDataFactory;
import org.eclipse.dawnsci.hdf5.IHierarchicalDataFile;
import org.eclipse.dawnsci.slicing.api.system.DimsDataList;
import org.eclipse.dawnsci.slicing.api.system.SliceSource;
import org.eclipse.dawnsci.slicing.api.util.SliceUtils;
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
import uk.ac.diamond.scisoft.analysis.dataset.IDataset;
import uk.ac.diamond.scisoft.analysis.dataset.ILazyDataset;
import uk.ac.diamond.scisoft.analysis.io.IDataHolder;
import uk.ac.diamond.scisoft.analysis.io.LoaderFactory;
import uk.ac.diamond.scisoft.analysis.io.SliceObject;
import uk.ac.diamond.scisoft.analysis.message.DataMessageComponent;
import uk.ac.diamond.scisoft.analysis.metadata.IMetadata;

import com.isencia.passerelle.actor.InitializationException;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.actor.TerminationException;
import com.isencia.passerelle.core.ErrorCode;
import com.isencia.passerelle.core.PasserelleException;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.MessageException;
import com.isencia.passerelle.message.MessageFactory;
import com.isencia.passerelle.resources.actor.IResourceActor;
import com.isencia.passerelle.resources.actor.ResourceObject;
import com.isencia.passerelle.util.EnvironmentUtils;
import com.isencia.passerelle.util.ptolemy.IAvailableChoices;
import com.isencia.passerelle.util.ptolemy.IAvailableMap;
import com.isencia.passerelle.util.ptolemy.RegularExpressionParameter;
import com.isencia.passerelle.util.ptolemy.ResourceParameter;
import com.isencia.passerelle.util.ptolemy.StringChoiceParameter;
import com.isencia.passerelle.util.ptolemy.StringMapParameter;
import com.isencia.passerelle.workbench.model.utils.ModelUtils;
import com.isencia.util.StringConvertor;

/**
 * Reads a file or directory using LoaderFactory and makes the data available to subsequent nodes
 * which can request certain data sets for use.
 * <p>
 * When an individual file is selected as data source, the actor can be configured with a slicing definition to generate a sequence of messages, 
 * each one representing an individual slice.
 * When a folder is selected as data source, the actor will generate a sequence of messages where each one represents an individual file.
 * </p>
 * 
 * @author gerring
 *
 */
public class DataImportSource extends AbstractDataMessageSource implements IResourceActor, IVariableProvider, ISliceInformationProvider {

	private static final Logger logger = LoggerFactory.getLogger(DataImportSource.class);	
	
	private static final String[] DATA_TYPES  = new String[] {"Complete data as numerical arrays", "Complete data as numerical arrays including scalars", "Just path and file name"};
	private static final String[] SLICE_TYPES = new String[] {"Unique name for each slice", "Same name for each slice"};
	
	// Read internally
	public Parameter           folderParam;

	public Parameter           relativePathParam;
	protected boolean          isPathRelative = true;
	
	public Parameter           metaParam;
	protected boolean          isMetaRequired = false;
	
	public final RegularExpressionParameter filterParam;

	public final ResourceParameter       path;
	public final StringChoiceParameter   names;
	public final StringMapParameter      rename;
	public final StringParameter         dataType;
	public final StringParameter         sliceNameType;
	public final SliceParameter          slicing;
	
	private List<TriggerObject> fileQueue;
	
	// a counter for indexing each generated message in the complete sequence that this source generates
	private long msgCounter;
	// a unique sequence identifier for each execution of this actor within a single parent workflow execution
	private long msgSequenceID;

	private final DataImportDelegate delegate;

	/**
	 * Required because sometimes the uk.ac.diamond.scisoft.analysis.osgi
	 * does not run in the workflow run configuration.
	 */
	static {
		try {
		    for (String ext : H5Loader.EXT) {
			    LoaderFactory.registerLoader(ext, H5Loader.class,0);
			}
		} catch (Exception ne) {
			logger.error("Cannot ensure that H5Loader is the default!", ne);
		}
	}
 
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
		
		super(container, ModelUtils.findUniqueActorName(container, ModelUtils.getLegalName(name)));
		
		relativePathParam = new Parameter(this, "Relative Path", new BooleanToken(true));
		registerConfigurableParameter(relativePathParam);
		setDescription(relativePathParam, Requirement.ESSENTIAL, VariableHandling.NONE, "Tick to set wether you will provide a path to the data as absolute or relative to the workspace (recommended).");

		folderParam = new Parameter(this, "Folder", new BooleanToken(isFolder));
		folderParam.setVisibility(Settable.NONE);
		
		metaParam = new Parameter(this, "Include Metadata", new BooleanToken(false));
		registerConfigurableParameter(metaParam);
		setDescription(metaParam, Requirement.OPTIONAL, VariableHandling.NONE, "Tick to send any meta data that the loader service can extract into the pipeline data message.");
		
		filterParam  = new RegularExpressionParameter(this, "File Filter", true);
		registerConfigurableParameter(filterParam);
		setDescription(filterParam, Requirement.OPTIONAL, VariableHandling.EXPAND, "A comma separated list of filters of the form '*.<extension>'. Regular expressions are not supported.");

		path = new ResourceParameter(this, "Path", "Data files", LoaderFactory.getSupportedExtensions().toArray(new String[0]));
		setDescription(path, Requirement.ESSENTIAL, VariableHandling.EXPAND, "The path to the data to read. May be an external file (full path to file) or a file in the workspace ('relative' file) or a folder which will iterate over all contained files and use the filter.");
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
                Collection<String> names = delegate.getAllDatasetsInFile();
                if (names==null || names.isEmpty()) return null;
                return names.toArray(new String[names.size()]);
			}
			@Override
			public Map<String,String> getVisibleChoices() {
			    return delegate.getChoppedNames();
			}
		}, SWT.MULTI);
		setDescription(names, Requirement.OPTIONAL, VariableHandling.NONE, "A list of data names to pass on from the file. If not set, all the data will be read. Please set the path before setting the dataset names. If the path is an expand, use a temperary (but typical) file so that the name list can be determined in the builder.");

		registerConfigurableParameter(names);
		
		rename = new StringMapParameter(this, "Rename Data Sets", new IAvailableMap() {		
			@Override
			public Map<String,String> getMap() {
				return delegate.getDataSetsRenameName();
			}
			@Override
			public Map<String,String> getVisibleKeyChoices() {
			    return delegate.getChoppedNames();
			}
			@Override
			public Collection<String> getSelectedChoices() {		
                final String[] ds     = names.getValue();
				return ds!=null ? Arrays.asList(ds) : null;
			}
		});
		setDescription(rename, Requirement.OPTIONAL, VariableHandling.NONE, "A map of dataset name to variable name. The variable name may be entered and for instance be a legal python variable name to make the use of python actors easy.");
		
		registerConfigurableParameter(rename);
		
		slicing = new SliceParameter(this, "Data Set Slice");
		registerConfigurableParameter(slicing);
		setDescription(slicing, Requirement.OPTIONAL, VariableHandling.NONE, "Slicing can only be done if one dataset is being exctracted from the data at a time. Set the '"+names.getDisplayName()+"' attribute first. You can use expands inside the slicing dialog.");

		dataType = new StringParameter(this,"Data Type") {
			public String[] getChoices() {
				return DATA_TYPES;
			}
		};
		dataType.setExpression(DATA_TYPES[0]);
		registerConfigurableParameter(dataType);
		setDescription(dataType, Requirement.ESSENTIAL, VariableHandling.NONE, "Either import the paths to the data files or the actual data from the data files.");
	
		sliceNameType = new StringParameter(this,"Slice Name Type") {
			public String[] getChoices() {
				return SLICE_TYPES;
			}
		};
		sliceNameType.setExpression(SLICE_TYPES[0]);
		registerConfigurableParameter(sliceNameType);
		setDescription(sliceNameType, Requirement.OPTIONAL, VariableHandling.NONE, "The slice name is either fixed the same at the data set name (or mapped value). Or can be unique for each slice. In the unique case, the name will be '<fixed_name>_slice_<index>', where index is the slice index.");

		
		delegate = new DataImportDelegate(path, names, relativePathParam, rename);

		
		setDescription("This source imports datasets using the loader service produced by Diamond Light Source. This loader service allows datasets in many formats to be imported and used in the workflow like numpy arrays. HDF5 files are supported, in this case you will need the path to the dataset in HDF5. When importing directories every file in the folder will be imported.");
	}

  @Override
  protected Logger getLogger() {
    return logger;
  }

	/**
	 *  @param attribute The attribute that changed.
	 *  @exception IllegalActionException   */
	public void attributeChanged(Attribute attribute) throws IllegalActionException {
	  getLogger().trace("{} : {}",getFullName(), attribute);

	  if (attribute == path) {
			delegate.clear();
		} else if (attribute == relativePathParam) {
			isPathRelative = ((BooleanToken)relativePathParam.getToken()).booleanValue();
		} else if (attribute == metaParam) {
			isMetaRequired = ((BooleanToken)metaParam.getToken()).booleanValue();
		} else if (attribute == folderParam) {
			// You cannot change this, it is set in the constuctor and is fixed.
		} else if (attribute == names) {
		  getLogger().trace("Data set names changed to : {}", names.getExpression());
		}
		
		super.attributeChanged(attribute);
	}
	
	@Override
	public void doPreInitialize() {
		fileQueue = null;
		delegate.clear();
	}

	@Override
	protected void doInitialize() throws InitializationException {
		super.doInitialize();
		msgCounter = 0;
		msgSequenceID = MessageFactory.getInstance().createSequenceID();
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
		
		if ((getManager()!=null) && (delegate.getSourcePath(triggerMsg)!=null)){
			final File file = new File(delegate.getSourcePath(triggerMsg));
			if (file.isDirectory()) {
				final List<File> fileList = SortingUtils.getSortedFileList(file);
				for (File f : fileList) {				
					if (!isFileLegal(f, triggerMsg)) continue;
					final TriggerObject ob = new TriggerObject();
					ob.setTrigger(triggerMsg);
					ob.setFile(f);
					fileQueue.add(ob);
				}
			} else {
				if (isFileLegal(file, triggerMsg) && slicing.getExpression()!=null && !"".equals(slicing.getExpression())) {

					try {
						final IDataHolder holder  = LoaderFactory.getData(file.getAbsolutePath(), null);
						ILazyDataset      lz      = getDataSetNames()!=null && getDataSetNames()[0]!=null && !"".equals(getDataSetNames()[0])
								                  ? holder.getLazyDataset(getDataSetNames()[0])
								                  : holder.getLazyDataset(0);
								                  
						final SliceSource       data   = new SliceSource(holder, lz, lz.getName(), file.getAbsolutePath(), false);
						final List<SliceObject> slices = SliceUtils.getExpandedSlices(data, 
								                                                      (DimsDataList)slicing.getBeanFromValue(DimsDataList.class),
								                                                      delegate.createSliceRangeSubstituter(triggerMsg));
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
						getLogger().error("Problem reading slices in data import.", ne);
						requestFinish();
					}
				} else {
					final TriggerObject ob = new TriggerObject();
					ob.setTrigger(triggerMsg);
					ob.setFile(file);
					fileQueue.add(ob);
				}
			}
			
			if (fileQueue!=null && fileQueue.isEmpty()) {
				getLogger().info("No files found in '{}'. Filter is set to: {}",file.getAbsolutePath(),filterParam.getExpression());
			}
		}
	}

	private boolean isFileLegal(File file, final ManagedMessage triggerMsg) {
		if (file.isDirectory())                  return false;
		if (file.isHidden())                     return false;
		if (file.getName().startsWith("."))      return false;
	    if (!isRequiredFileName(file.getName(), triggerMsg)) return false;	
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
		
		// Stops data being loaded while a modal dialog is being shown to user.
		ActorUtils.waitWhileLocked();
		
		final TriggerObject file = fileQueue.remove(0);
        ManagedMessage msg = MessageFactory.getInstance().createMessageInSequence(msgSequenceID, msgCounter++, hasNoMoreMessages(), getStandardMessageHeaders());
		try {
			msg.setBodyContent(getData(file), DatasetConstants.CONTENT_TYPE_DATA);
		} catch (MessageException e) {
			logger.error("Cannot set map of data in message body!", e);
			msg = MessageFactory.getInstance().createErrorMessage(new PasserelleException(ErrorCode.MSG_CONSTRUCTION_ERROR, "Cannot set map of data in message body!", this, e));
			fileQueue.clear();
		} catch (Exception ne) {
			fileQueue.clear();
			throw new DataMessageException("Cannot read data from '"+delegate.getSourcePath(msg)+"'", this, ne);
		}
			
		try {
			msg.setBodyHeader("TITLE", file.getFile().getName());
		} catch (MessageException e) {
			msg = MessageFactory.getInstance().createErrorMessage(new PasserelleException(ErrorCode.MSG_CONSTRUCTION_ERROR, "Cannot set header in message!", this, e));
		}

		return msg;
	}

	protected void doWrapUp() throws TerminationException {
		super.doWrapUp();
		if (isFinishRequested()) {
			fileQueue.clear();
			delegate.clear();
		}
	}
	
	private boolean isRequiredFileName(String fileName, final ManagedMessage triggerMsg) {

		String fileFilter;
		try {
			fileFilter = ParameterUtils.getSubstituedValue(filterParam, MessageUtils.coerceMessage(triggerMsg));
		} catch (Throwable ne) {
			fileFilter = filterParam.getExpression();
		}
		if (fileFilter == null || "".equals(fileFilter))
			return true;
		if (filterParam.isJustWildCard()) {
			final StringMatcher matcher = new StringMatcher(fileFilter, true, false);
			return matcher.match(fileName);
		} else {
			return fileName.matches(fileFilter);
		}
	}
	
	protected DataMessageComponent getData(final TriggerObject trigOb) throws Exception {
		// Add everything non-data from upstream that we can, then decide on the details
		// like data slicing.
		final DataMessageComponent comp = new DataMessageComponent();
		if (trigOb.getTrigger()!=null) {
			try {
			    DataMessageComponent dmc = MessageUtils.coerceMessage(trigOb.getTrigger());
			    comp.addScalar(dmc.getScalar());
			    comp.addList(dmc.getList());
			    comp.addRois(dmc.getRois());
			    comp.addFunctions(dmc.getFunctions());
			} catch (Throwable ignored) {
				// triggers to not have to be DataMessageComponent
			}
		}
		
		final File                 file      = trigOb.getFile();
		final ManagedMessage       triggerMsg= trigOb.getTrigger();
		final String               filePath  = file.getAbsolutePath();
        
		final Map<String,Serializable>   datasets;
		if (trigOb.getSlice()!=null) {
			final SliceObject slice = trigOb.getSlice();
			slice.setPath(file.getAbsolutePath());
			
			final String datasetPath = getDataSetNames()[0];
			slice.setName(datasetPath);
			
			String sliceName = delegate.getMappedName(datasetPath);
			if (SLICE_TYPES[0].equals(sliceNameType.getExpression())) {
				sliceName = sliceName+"_slice_"+trigOb.getIndex();
			} 
			
			final String pyName    = PythonUtils.getLegalVarName(sliceName, null);
				
			final IDataHolder      dh  = LoaderFactory.getData(slice.getPath());
			ILazyDataset    ld  = dh.getLazyDataset(slice.getName());
			if (ld==null) ld = dh.getLazyDataset(0);
			final IDataset set = SliceUtils.getSlice(ld, slice, null);
			set.setName(pyName);
			datasets = new HashMap<String,Serializable>(1);
			datasets.put(pyName, set);
			
			pushSlice(comp, slice);
		} else {
			datasets = (DATA_TYPES[2].equals(dataType.getExpression())) ? null : delegate.getDatasets(filePath, trigOb);
		}
		
		// Add messages from upstream, if any.
		if (triggerMsg!=null) {
			try {
				final DataMessageComponent c = MessageUtils.coerceMessage(triggerMsg);
			  comp.addScalar(c.getScalar());
			} catch (Exception ignored) {
				logger.debug("Trigger for "+getName()+" is not DataMessageComponent, no data added.");
			}
		}
		
		if (datasets!=null) comp.addList(datasets);
		if (isMetaRequired) {
			IMetadata meta =  LoaderFactory.getMetadata(filePath, null);
			comp.setMeta(meta);
		}
		comp.putScalar("file_path", filePath);
		comp.putScalar("file_name", new File(filePath).getName());
		comp.putScalar("file_dir",  FileUtils.getDirectoryAbsolutePath(filePath));
		
		// Process any scalars in the HDF5 file if there are any
		if (DATA_TYPES[1].equals(dataType.getExpression()) && H5Loader.isH5(filePath)) {
			IHierarchicalDataFile hFile = null;
			try {
				hFile = HierarchicalDataFactory.getReader(filePath);
				final List<String> paths = hFile.getDatasetNames(IHierarchicalDataFile.SCALAR);
				for (String path : paths) {
					final Dataset set  = (Dataset)hFile.getData(path);
					final Object  val  = set.getData();
					String scalar=null;
			        if (val instanceof byte[]) {
			        	scalar = String.valueOf(((byte[])val)[0]);
			        } else if (val instanceof short[]) {
			        	scalar = String.valueOf(((short[])val)[0]);
			        } else if (val instanceof int[]) {
			        	scalar = String.valueOf(((int[])val)[0]);
			        } else if (val instanceof long[]) {
			        	scalar = String.valueOf(((long[])val)[0]);
			        } else if (val instanceof float[]) {
			        	scalar = String.valueOf(((float[])val)[0]);
			        } else if (val instanceof double[]) {
			        	scalar = String.valueOf(((double[])val)[0]);
			        } else if (val instanceof String[]) {
			        	scalar = String.valueOf(((String[])val)[0]);
			        }
			        if (scalar!=null) comp.putScalar(set.getName(), scalar);
				}
			} catch (Exception ne) {
				getLogger().error("Cannot read file "+filePath, ne);
			} finally {
				if (file!=null) hFile.close();
			}
		}
		
		return comp;
	}

  /**
   * TODO : check where this creation of a slice stack would best be located?
   * Should we make slice context explicit on the DataMessageComponent interface?
   * 
   * @param comp
   * @param slice
   */
  private void pushSlice(final DataMessageComponent comp, final SliceObject slice) {
    Stack<SliceObject> slices = null;
    try {
      slices = (Stack<SliceObject>) comp.getUserObject(DatasetConstants.SLICE_STACK_NAME);
    } catch (ClassCastException e) {
      getLogger().error("Invalid user object stored with reserved name '"+DatasetConstants.SLICE_STACK_NAME+"'", e);
    }
    if(slices==null) {
      slices = new Stack<SliceObject>();
      comp.addUserObject(DatasetConstants.SLICE_STACK_NAME, slices);
    }
    slices.push(slice);
  }
	
	public String[] getDataSetNames() {
		return names.getValue();
	}
	
	@Override
	protected String getExtendedInfo() {
		return "A source which uses  the GDA 'LoaderFactory' to read a DataHandler which can be used to access data";
	}
	
	public String getSourcePath() {
		return delegate.getSourcePath();
	}

	private Object getResource() {
		if (isPathRelative) {
			String sourcePath = this.path.getExpression();
			try {
				sourcePath = ParameterUtils.substitute(sourcePath, this);
			} catch (Exception e) {
				logger.error("Cannot ret resource "+sourcePath, e);
				return null;
			}
			return ResourcesPlugin.getWorkspace().getRoot().findMember(sourcePath, true);
		} else {
			return new File(delegate.getSourcePath());
		}
	}
	
	private String getResourceTypeName() {
		final File file = new File(delegate.getSourcePath());
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
		boolean isFullData = DATA_TYPES[0].equals(dataType.getExpression()) || DATA_TYPES[1].equals(dataType.getExpression());
		return delegate.getOutputVariables(isFullData);
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
