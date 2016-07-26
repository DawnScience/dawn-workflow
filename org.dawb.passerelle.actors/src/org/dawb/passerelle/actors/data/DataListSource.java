package org.dawb.passerelle.actors.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.dawb.common.util.python.PythonUtils;
import org.dawb.passerelle.common.DatasetConstants;
import org.dawb.passerelle.common.actors.AbstractDataMessageSource;
import org.dawb.passerelle.common.message.DataMessageException;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.passerelle.common.parameter.ParameterUtils;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.dawnsci.analysis.api.io.IDataHolder;
import org.eclipse.dawnsci.analysis.api.message.DataMessageComponent;
import org.eclipse.january.dataset.IDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.data.BooleanToken;
import ptolemy.data.expr.Parameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import uk.ac.diamond.scisoft.analysis.io.LoaderFactory;

import com.isencia.passerelle.actor.InitializationException;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.core.ErrorCode;
import com.isencia.passerelle.core.PasserelleException;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.MessageException;
import com.isencia.passerelle.message.MessageFactory;
import com.isencia.passerelle.util.EnvironmentUtils;
import com.isencia.passerelle.util.ptolemy.ResourceParameter;
import com.isencia.util.StringConvertor;


/**
 * This source is used to read a pipeline of datasets from different files
 * where the information for each message is one line in a csv file.
 * 
 * It is used for spectroscopy pipelines that want to process files already chosen
 * by the user.
 * 
 * @author Matthew Gerring
 *
 */
public class DataListSource extends AbstractDataMessageSource {

	private static Logger logger = LoggerFactory.getLogger(DataListSource.class);
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6195973241085873774L;
	private Parameter relativePathParam;
	private ResourceParameter path;
	
	private List<DataListItem> fileQueue;

	public DataListSource(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		
		super(container, name);
		
		relativePathParam = new Parameter(this, "Relative Path", new BooleanToken(true));
		registerConfigurableParameter(relativePathParam);

		path = new ResourceParameter(this, "Data List", "List of data", "cvs");
		try {
			URI baseURI = new File(StringConvertor.convertPathDelimiters(EnvironmentUtils.getApplicationRootFolder())).toURI();
			path.setBaseDirectory(baseURI);
		} catch (Exception e) {
			logger.error("Cannot set base directory for "+getClass().getName(), e);
		}
		registerConfigurableParameter(path);

	}
	@Override
	protected void doInitialize() throws InitializationException {
	
		super.doInitialize();
		
		fileQueue = new ArrayList<DataListItem>(89);
		if (!isTriggerConnected()) {
		    appendDataList(null); // Otherwise the trigger will call create on the iterator.
		}
	}

	private void appendDataList(ManagedMessage message) throws InitializationException {
		
		final String csvFile = getSourcePath(message);
		try {
			
			final BufferedReader reader = new BufferedReader(new FileReader(csvFile));
			try {
				String line = null;
				while((line = reader.readLine())!=null) {
					final String[] columns = line.split(",");
					fileQueue.add(new DataListItem(columns[0], Arrays.copyOfRange(columns, 1, columns.length)));
				}
			} finally {
				reader.close();
			}
		} catch (Exception ne) {
			throw new InitializationException(ErrorCode.ACTOR_INITIALISATION_ERROR, "Cannot read csv file "+csvFile, this, ne);
		}
	}
	
	public String getSourcePath(final ManagedMessage manMsg) {

		String sourcePath=null;
		try {
			final DataMessageComponent comp = manMsg!=null ? MessageUtils.coerceMessage(manMsg) : null;
			sourcePath = ParameterUtils.getSubstituedValue(this.path, comp);
		} catch (Exception e) {
			// Can happen when they have an expand in the parameter that
			// is not resolved until run time.
			logger.info("Cannot substitute parameter "+path, e);
		}

		if (isPathRelative()) {
			try {
				final IResource res = ResourcesPlugin.getWorkspace().getRoot().findMember(sourcePath, true);
				if (res==null) return  null;
				sourcePath = res.getLocation().toOSString();
			} catch (NullPointerException ne) {
				return null;
			}
		}
		
        //getStandardMessageHeaders().put(ManagedMessage.SystemHeader.HEADER_SOURCE_INFO,sourcePath);

		return sourcePath;
	}

	private boolean isPathRelative() {
		try {
			return ((BooleanToken)relativePathParam.getToken()).booleanValue();
		} catch (Exception ne) {
			logger.error("Cannot find out if relative path");
			return false;
		}
	}

	@Override
	protected ManagedMessage getDataMessage() throws ProcessingException {
		
		if (fileQueue == null)   return null;
		if (fileQueue.isEmpty()) return null;
		
		if (isFinishRequested()) {
			fileQueue.clear();
			return null;
		}
		
		ManagedMessage msg = MessageFactory.getInstance().createMessage();
		final DataListItem file = fileQueue.remove(0);
		try {
			final IDataHolder dh = LoaderFactory.getData(file.getFilePath());
			final DataMessageComponent despatch = new DataMessageComponent();
			for (String name : file.getDatasetNames()) {
				if (name==null || "".equals(name)) continue;
				final String pythonName = PythonUtils.getLegalVarName(name, null);
				final IDataset data     = dh.getDataset(name);
				if (data == null) {
					throw new IllegalArgumentException("Could not load " + name + " from " + file.getFilePath());
				}
				despatch.addList(pythonName, data);
			}
			msg.setBodyContent(despatch, DatasetConstants.CONTENT_TYPE_DATA);
	
		} catch (MessageException e) {
			logger.error("Cannot set map of data in message body!", e);
			msg = MessageFactory.getInstance().createErrorMessage(new PasserelleException("Cannot set map of data in message body!", "application/x-data", e));
			fileQueue.clear();

		} catch (Exception ne) {
			fileQueue.clear();
			throw new DataMessageException("Cannot read data from '"+file.getFilePath()+"'", this, ne);
		}
			
		try {
			msg.setBodyHeader("TITLE", (new File(file.getFilePath())).getName());
		} catch (MessageException e) {
			msg = MessageFactory.getInstance().createErrorMessage(new PasserelleException("Cannot set header in message!", "application/x-data", e));
		}

		return msg;

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
		try {
			appendDataList(triggerMsg);
		} catch (InitializationException e) {
			logger.error("Cannot trigger!", e);
		}
	}
	
	private class DataListItem {
		private String filePath;
		private String[] datasetNames;
		public DataListItem(String filePath, String[] datasetNames) {
			this.filePath     = filePath;
			this.datasetNames = datasetNames;
		}
		public String getFilePath() {
			return filePath;
		}
		public void setFilePath(String filePath) {
			this.filePath = filePath;
		}
		public void setDatasetNames(String[] datasetNames) {
			this.datasetNames = datasetNames;
		}
		public String[] getDatasetNames() {
			return datasetNames;
		}
	}
}
