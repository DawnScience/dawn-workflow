package org.dawb.passerelle.actors.data;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.dawb.passerelle.actors.data.config.ISliceInformationProvider;
import org.dawb.passerelle.actors.data.config.SliceParameter;
import org.dawb.passerelle.common.DatasetConstants;
import org.dawb.passerelle.common.actors.AbstractDataMessageSource;
import org.dawb.passerelle.common.actors.ActorUtils;
import org.dawb.passerelle.common.message.DataMessageException;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.IVariable.VARIABLE_TYPE;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.passerelle.common.message.Variable;
import org.dawb.passerelle.common.parameter.ParameterUtils;
import org.dawnsci.io.h5.H5Loader;
import org.dawnsci.slicing.api.system.DimsData;
import org.dawnsci.slicing.api.system.DimsDataList;
import org.eclipse.swt.SWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.data.BooleanToken;
import ptolemy.data.IntToken;
import ptolemy.data.expr.Parameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import uk.ac.diamond.scisoft.analysis.dataset.ILazyDataset;
import uk.ac.diamond.scisoft.analysis.io.IDataHolder;
import uk.ac.diamond.scisoft.analysis.io.LoaderFactory;
import uk.ac.diamond.scisoft.analysis.message.DataMessageComponent;

import com.isencia.passerelle.actor.InitializationException;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.core.ErrorCode;
import com.isencia.passerelle.core.PasserelleException;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.MessageException;
import com.isencia.passerelle.message.MessageFactory;
import com.isencia.passerelle.util.EnvironmentUtils;
import com.isencia.passerelle.util.ptolemy.IAvailableChoices;
import com.isencia.passerelle.util.ptolemy.ResourceParameter;
import com.isencia.passerelle.util.ptolemy.StringChoiceParameter;
import com.isencia.util.StringConvertor;

/**
 * This class is designed to compute the string slices over
 * an HDF5 file or stack with a given chunk and send the slice 
 * as a string correct for the file referenced.
 * 
 * It outputs a string 'slice' which is the chunk and a string
 * 'file_name' and 'file_path' for the file.
 * 
 * @author fcp94556
 *
 */
public class DataChunkSource extends AbstractDataMessageSource implements ISliceInformationProvider {

	private static final Logger logger = LoggerFactory.getLogger(DataChunkSource.class);
	
	public final ResourceParameter       path;
	public final StringChoiceParameter   datasetName;
	public final Parameter               relativePathParam;
	public final SliceParameter          slicing;
	public final Parameter               chunkSize;

	private final DataImportDelegate      delegate;
	
	private List<SliceBean> sliceQueue;
	
	// a counter for indexing each generated message in the complete sequence that this source generates
	private long msgCounter;
	// a unique sequence identifier for each execution of this actor within a single parent workflow execution
	private long msgSequenceID;

	/**
	 * 
	 */
	private static final long serialVersionUID = -5057873158019792804L;

	public DataChunkSource(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		
		super(container, name);
		
		relativePathParam = new Parameter(this, "Relative Path", new BooleanToken(true));
		registerConfigurableParameter(relativePathParam);
		setDescription(relativePathParam, Requirement.ESSENTIAL, VariableHandling.NONE, "Tick to set wether you will provide a path to the data as absolute or relative to the workspace (recommended).");

		path = new ResourceParameter(this, "Path", "Data file", H5Loader.EXT.toArray(new String[H5Loader.EXT.size()]));
		try {
			URI baseURI = new File(StringConvertor.convertPathDelimiters(EnvironmentUtils.getApplicationRootFolder())).toURI();
			path.setBaseDirectory(baseURI);
		} catch (Exception e) {
			logger.error("Cannot set base directory for "+getClass().getName(), e);
		}
		registerConfigurableParameter(path);

		datasetName = new StringChoiceParameter(this, "Dataset Name", new IAvailableChoices() {		
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
		}, SWT.SINGLE);
		
		registerConfigurableParameter(datasetName);
		
		// Now the delegate
		delegate = new DataImportDelegate(path, datasetName, relativePathParam, null);

		slicing = new SliceParameter(this, "Data Set Slice");
		registerConfigurableParameter(slicing);
		setDescription(slicing, Requirement.OPTIONAL, VariableHandling.NONE, "Slicing can only be done if one dataset is being exctracted from the data at a time. Set the '"+datasetName.getDisplayName()+"' attribute first. You can use expands inside the slicing dialog.");
	
		
		chunkSize = new Parameter(this, "Chunk Size");
		chunkSize.setToken(new IntToken(8));
		registerConfigurableParameter(chunkSize);
		

	}

	@Override
	public void doPreInitialize() {
		delegate.clear();
	}
	
	public void attributeChanged(Attribute attribute) throws IllegalActionException {
		getLogger().trace("{} : {}",getFullName(), attribute);

		if (attribute == path) {
			delegate.clear();
		}
		super.attributeChanged(attribute);
	}


	@Override
	protected void doInitialize() throws InitializationException {
		super.doInitialize();
		msgCounter = 0;
		msgSequenceID = MessageFactory.getInstance().createSequenceID();
		sliceQueue = new ArrayList<SliceBean>(89);
		if (!isTriggerConnected()) {
		    appendQueue(null); // Otherwise the trigger will call create on the iterator.
		}
	}

	@Override
	protected ManagedMessage getDataMessage() throws ProcessingException {
		
		if (sliceQueue == null)   return null;
		if (sliceQueue.isEmpty()) return null;
		
		if (isFinishRequested()) {
			sliceQueue.clear();
			return null;
		}
		
		// Stops data being loaded while a modal dialog is being shown to user.
		ActorUtils.waitWhileLocked();	
		
		final SliceBean sliceBean = sliceQueue.remove(0);
System.out.println(sliceBean);
        ManagedMessage msg = MessageFactory.getInstance().createMessageInSequence(msgSequenceID, msgCounter++, hasNoMoreMessages(), getStandardMessageHeaders());
		
        try {
        	final DataMessageComponent comp = new DataMessageComponent();
           	comp.putScalar("slice", sliceBean.getSlice());
           	comp.putScalar("shape", sliceBean.getShape());
           	comp.putScalar("file_name", sliceBean.getFile().getName());
           	comp.putScalar("file_path", sliceBean.getFile().getAbsolutePath());
          	
			msg.setBodyContent(comp, DatasetConstants.CONTENT_TYPE_DATA);
			
		} catch (MessageException e) {
			logger.error("Cannot set map of data in message body!", e);
			msg = MessageFactory.getInstance().createErrorMessage(new PasserelleException(ErrorCode.MSG_CONSTRUCTION_ERROR, "Cannot set map of data in message body!", this, e));
			sliceQueue.clear();
		} catch (Exception ne) {
			sliceQueue.clear();
			throw new DataMessageException("Cannot read data from '"+delegate.getSourcePath(msg)+"'", this, ne);
		}
        
		try {
			msg.setBodyHeader("TITLE", sliceBean.getFile().getName());
		} catch (MessageException e) {
			msg = MessageFactory.getInstance().createErrorMessage(new PasserelleException(ErrorCode.MSG_CONSTRUCTION_ERROR, "Cannot set header in message!", this, e));
		}

		return msg;
	}

	@Override
	public List<IVariable> getOutputVariables() {
		
		final List<IVariable> ret = super.getOutputVariables();
		if (delegate.getSourcePath()==null)  {
			final String msg = "Invalid Path '"+path.getExpression()+"'";
			ret.add(new Variable("file_path", VARIABLE_TYPE.PATH,   msg, String.class));
			ret.add(new Variable("file_name", VARIABLE_TYPE.SCALAR, msg, String.class));
			ret.add(new Variable("slice",  VARIABLE_TYPE.SCALAR,   msg, String.class));
			ret.add(new Variable("shape",  VARIABLE_TYPE.SCALAR,   msg, String.class));
			return ret;
		}
		
		ret.add(new Variable("file_path", VARIABLE_TYPE.PATH, delegate.getSourcePath(), String.class));
		ret.add(new Variable("file_name", VARIABLE_TYPE.SCALAR, new File(delegate.getSourcePath()).getName(), String.class));
		try {
			ret.add(new Variable("slice",     VARIABLE_TYPE.SCALAR, "0:"+((IntToken)chunkSize.getToken()).intValue()+" ...", String.class));
		} catch (Throwable e) {
			ret.add(new Variable("slice",     VARIABLE_TYPE.SCALAR, "0:8 ...", String.class));
		}
		ret.add(new Variable("shape",     VARIABLE_TYPE.SCALAR,  "Example: \"[0:8, Y, X]\"", String.class));
		
		return ret;
	}

	/**
	 * triggerMsg may be null
	 * @param triggerMsg
	 */
	private void appendQueue(final ManagedMessage triggerMsg)  throws InitializationException {
		
		if ((getManager()!=null) && (delegate.getSourcePath(triggerMsg)!=null)){
			
			final File file = new File(delegate.getSourcePath(triggerMsg));
			
			if (!isFileLegal(file, triggerMsg)) {
				throw new InitializationException(ErrorCode.FATAL, "Cannot slice directories at the moment!", this, null);
			}

			try {
				final IDataHolder holder  = LoaderFactory.getData(file.getAbsolutePath(), null);
				DataMessageComponent msg  = triggerMsg==null ? null : MessageUtils.coerceMessage(triggerMsg);
				final String      dsPath  = ParameterUtils.getSubstituedValue(datasetName, msg);
				final ILazyDataset lz     = holder.getLazyDataset(dsPath);
				
				if (lz == null) {
					throw new InitializationException(ErrorCode.FATAL, "Cannot get lazy dataset from "+datasetName.getExpression(), this, null);
				}
			
				DimsDataList ddl = (DimsDataList)slicing.getBeanFromValue(DimsDataList.class);
				// Check that only one range is defined
				DimsData range = null;
				for (DimsData dd : ddl.iterable()) {
					
					if (dd.getPlotAxis().isAdvanced())  throw new InitializationException(ErrorCode.FATAL, "Advanced dimensions are not supported!", this, null);
					if (dd.isTextRange()) {
						if (range!=null) throw new InitializationException(ErrorCode.FATAL, "Only one axis may be set as a range!", this, null);
						range = dd;
					}
				}
				
				if (range==null) throw new InitializationException(ErrorCode.FATAL, "One axis must be a range!", this, null);
			    
				final List<DimsData> exp = range.expand(lz.getShape()[range.getDimension()], delegate.createSliceRangeSubstituter(triggerMsg));
				// This is the full list of this dimension. However we want to create chunks from this.

				// Rather low level but if it works then we are away
		        int count     = 0;
		        int chunk     = ((IntToken)chunkSize.getToken()).intValue();
		        while(count<exp.size()) {
		        	int end = count+chunk-1;
		        	if (end>exp.size()) end = exp.size();
		        	String slice = count+":"+end;
		        	String shape = getShape(ddl, slice);
		        	
		        	sliceQueue.add(new SliceBean(slice, shape, file));
		        	
		        	count+=chunk;
		        }
		        
			} catch (Exception ne) {
				// This is the end!
				getLogger().error("Problem reading slices in data import.", ne);
				requestFinish();
			}
			
			if (sliceQueue!=null && sliceQueue.isEmpty()) {
				getLogger().info("No files found in '{}'. Filter is set to: {}",file.getAbsolutePath());
			}
		}
	}

	private String getShape(DimsDataList ddl, String slice) {
		final StringBuilder buf = new StringBuilder();
		buf.append("[");
		for (int i = 0; i < ddl.size(); i++) {
			DimsData dd = ddl.getDimsData(i);
			if (dd.isTextRange()) {
				buf.append(slice);
			} else {
				buf.append(dd.getShapeLabel());
			}
			if (i<ddl.size()-1) buf.append(", ");
		}
		buf.append("]");
		return null;
	}

	private boolean triggeredOnce = false;

	@Override
	protected boolean mustWaitForTrigger() {
		if (!isTriggerConnected()) return false;
		if (!triggeredOnce)        return true;
		return false;
	}
	
	protected void acceptTriggerMessage(ManagedMessage triggerMsg) {
		triggeredOnce = true;
		try {
			appendQueue(triggerMsg);
		} catch (InitializationException e) {
			logger.error("Cannot add slices to queue!", e);
			requestFinish();
		}
	}

	private boolean isFileLegal(File file, final ManagedMessage triggerMsg) {
		if (file.isDirectory())                  return false;
		if (file.isHidden())                     return false;
		if (file.getName().startsWith("."))      return false;
	    if (H5Loader.isH5(file.getPath())) return true;	
	    return false;
	}
	
	
	private class SliceBean {
		private String slice;
		private String shape;
		private File   file;
		public SliceBean(String slice, String shape, File file) {
			super();
			this.slice = slice;
			this.shape = shape;
			this.file = file;
		}
		public File getFile() {
			return file;
		}
		public void setFile(File file) {
			this.file = file;
		}
		public String getSlice() {
			return slice;
		}

		public void setSlice(String slice) {
			this.slice = slice;
		}
		public String getShape() {
			return shape;
		}
		public void setShape(String shape) {
			this.shape = shape;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((file == null) ? 0 : file.hashCode());
			result = prime * result + ((shape == null) ? 0 : shape.hashCode());
			result = prime * result + ((slice == null) ? 0 : slice.hashCode());
			return result;
		}
		@Override
		public String toString() {
			return "SliceBean [slice=" + slice + ", shape=" + shape + ", file="
					+ file + "]";
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SliceBean other = (SliceBean) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (file == null) {
				if (other.file != null)
					return false;
			} else if (!file.equals(other.file))
				return false;
			if (shape == null) {
				if (other.shape != null)
					return false;
			} else if (!shape.equals(other.shape))
				return false;
			if (slice == null) {
				if (other.slice != null)
					return false;
			} else if (!slice.equals(other.slice))
				return false;
			return true;
		}
		private DataChunkSource getOuterType() {
			return DataChunkSource.this;
		}
	}


	@Override
	public String[] getDataSetNames() {
		try {
			String dsPath = ParameterUtils.getSubstituedValue(datasetName.getExpression(), this, null);
			return new String[]{dsPath};
		} catch (Exception e) {
			return new String[]{datasetName.getExpression()};

		}
	}

	@Override
	public String getSourcePath() {
		return delegate.getSourcePath();
	}
}
