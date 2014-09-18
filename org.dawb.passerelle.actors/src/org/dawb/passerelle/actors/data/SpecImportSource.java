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
import java.io.FileInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.dawb.passerelle.common.DatasetConstants;
import org.dawb.passerelle.common.actors.AbstractDataMessageSource;
import org.dawb.passerelle.common.message.DataMessageException;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.IVariable.VARIABLE_TYPE;
import org.dawb.passerelle.common.message.IVariableProvider;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.passerelle.common.message.Variable;
import org.dawb.passerelle.common.parameter.ParameterUtils;
import org.dawnsci.io.spec.MultiScanDataEvent;
import org.dawnsci.io.spec.MultiScanDataListener;
import org.dawnsci.io.spec.MultiScanDataParser;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.dawnsci.analysis.api.message.DataMessageComponent;
import org.eclipse.dawnsci.analysis.dataset.impl.Dataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.data.BooleanToken;
import ptolemy.data.expr.Parameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Settable;
import uk.ac.diamond.scisoft.analysis.io.LoaderFactory;

import com.isencia.passerelle.actor.InitializationException;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.actor.TerminationException;
import com.isencia.passerelle.core.PasserelleException;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.MessageException;
import com.isencia.passerelle.message.MessageFactory;
import com.isencia.passerelle.resources.actor.IResourceActor;
import com.isencia.passerelle.resources.actor.ResourceObject;
import com.isencia.passerelle.resources.util.ResourceUtils;
import com.isencia.passerelle.util.EnvironmentUtils;
import com.isencia.passerelle.util.ptolemy.ResourceParameter;
import com.isencia.util.StringConvertor;

/**
 * Reads a multi scan spec file and sends each scan as a mesage
 * 
 * @author gerring
 *
 */
public class SpecImportSource extends AbstractDataMessageSource implements IResourceActor, IVariableProvider {

	private static final Logger logger = LoggerFactory.getLogger(SpecImportSource.class);
	
	// Read internally
	protected Parameter             folderParam;
	protected Parameter             relativePathParam;
	protected boolean               isPathRelative = true;
		
	private final ResourceParameter path;
	private MultiScanDataParser          scanParser;
	private List<MultiScanDataEvent>     scanQueue;
 
	/**
	 * 
	 */
	private static final long serialVersionUID = -851384753061854424L;
	
	public SpecImportSource(CompositeEntity container, String name) throws IllegalActionException, NameDuplicationException {
        this(container, name, false);
	}
	
	/**
	 * 
	 * @param container
	 * @param name
	 * @throws IllegalActionException
	 * @throws NameDuplicationException
	 */
	protected SpecImportSource(CompositeEntity container, String name, boolean isFolder) throws IllegalActionException, NameDuplicationException {
		
		super(container, ResourceUtils.findUniqueActorName(container, name));
		
		relativePathParam = new Parameter(this, "Relative Path", new BooleanToken(true));
		registerConfigurableParameter(relativePathParam);

		folderParam = new Parameter(this, "Folder", new BooleanToken(isFolder));
		folderParam.setVisibility(Settable.NONE);
				
		path = new ResourceParameter(this, "Path", "Data files", LoaderFactory.getSupportedExtensions().toArray(new String[0]));
		try {
			URI baseURI = new File(StringConvertor.convertPathDelimiters(EnvironmentUtils.getApplicationRootFolder())).toURI();
			path.setBaseDirectory(baseURI);
		} catch (Exception e) {
			logger.error("Cannot set base directory for "+getClass().getName(), e);
		}
		registerConfigurableParameter(path);

		
	}
	
	/**
	 *  @param attribute The attribute that changed.
	 *  @exception IllegalActionException   */
	public void attributeChanged(Attribute attribute) throws IllegalActionException {

		if(logger.isTraceEnabled()) logger.trace(getInfo()+" :"+attribute);
		if (attribute == path) {
		} else if (attribute == relativePathParam) {
			isPathRelative = ((BooleanToken)relativePathParam.getToken()).booleanValue();
		} else if (attribute == folderParam) {
			// You cannot change this, it is set in the constuctor and is fixed.
		}
		
		super.attributeChanged(attribute);
	}
	
	@Override
	public void doPreInitialize() {
		scanQueue      = null;
	}

	@Override
	protected void doInitialize() throws InitializationException {
	
		super.doInitialize();
		
		scanQueue = new ArrayList<MultiScanDataEvent>(89);
		if (!isTriggerConnected()) {
		    startScanParsing(null); // Otherwise the trigger will call create on the iterator.
		}
	}
	
	private ManagedMessage lastTriggered;
	/**
	 * triggerMsg may be null
	 * @param triggerMsg
	 */
	private void startScanParsing(final ManagedMessage triggerMsg) {
		
		lastTriggered = triggerMsg;
		if (getManager()!=null) {
			try {
				scanParser = new MultiScanDataParser(new FileInputStream(getSourceFile()), new MultiScanDataListener() {				
					@Override
					public boolean specDataPerformed(MultiScanDataEvent evt) {
						if (isFinishRequested()) return false;
						evt.setUserObject(triggerMsg); // May be null
						scanQueue.add(evt);
						return true;
					}
				});
		
				scanParser.start();
				
			} catch (Exception e) {
				logger.error("Cannot start scan parser!", e);
			}
		}
	}

	public boolean hasNoMoreMessages() {
	    if (scanQueue == null)   return true;
        return scanQueue.isEmpty() && super.hasNoMoreMessages();
    }
	
	protected ManagedMessage getDataMessage() throws ProcessingException {
		
		
		// We attempt to wait if the queue is empty and the parsing is
		// still happening.
		if (scanQueue!=null && scanQueue.isEmpty()) {
			if (scanParser!=null) {
				while(!scanParser.isParseComplete() && scanQueue.isEmpty()) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						logger.error("Cannot wait for scan queue to be populated.", e);
					}
				}
			}
		}
		
		if (scanQueue == null)   return null;
		if (scanQueue.isEmpty()) return null;
		
		if (isFinishRequested()) {
			scanQueue.clear();
			return null;
		}
		
		ManagedMessage      msg = MessageFactory.getInstance().createMessage();
		final MultiScanDataEvent evt = scanQueue.remove(0);
		try {
			msg.setBodyContent(getData(evt), DatasetConstants.CONTENT_TYPE_DATA);
	
		} catch (MessageException e) {
			logger.error("Cannot set map of data in message body!", e);
			msg = MessageFactory.getInstance().createErrorMessage(new PasserelleException("Cannot set map of data in message body!", "application/x-data", e));
			scanQueue.clear();

		} catch (Exception ne) {
			scanQueue.clear();
			throw new DataMessageException("Cannot read data from '"+getSourceFile()+"'", this, ne);
		}
			
		try {
			msg.setBodyHeader("TITLE", evt.getScanName());
		} catch (MessageException e) {
			msg = MessageFactory.getInstance().createErrorMessage(new PasserelleException("Cannot set header in message!", "application/x-data", e));
		}

		return msg;
	}

	protected void doWrapUp() throws TerminationException {
		super.doWrapUp();
		if (isFinishRequested()) {
			scanQueue.clear();
		}
	}
	
	protected DataMessageComponent getData(final MultiScanDataEvent evt) throws Exception {
				
		final DataMessageComponent comp = new DataMessageComponent();
		
		final ManagedMessage triggerMsg = (ManagedMessage)evt.getUserObject();
		// Add messages from upsteam, if any.
		if (triggerMsg!=null) {
			try {
				final DataMessageComponent c = MessageUtils.coerceMessage(triggerMsg);
			    comp.addScalar(c.getScalar());
			} catch (Exception ignored) {
				logger.info("Trigger for "+getName()+" is not DataMessageComponent, no data added.");
			}
		}
		
		final Collection<Dataset> sets = evt.getData();
		if (sets!=null) for (Dataset a : sets) {
			comp.addList(a.getName(), a);
		}
		
		comp.putScalar("file_path", getSourceFile().getAbsolutePath());
		comp.putScalar("file_name", getSourceFile().getName());
		comp.putScalar("file_dir",  getSourceFile().getParent());
		
		return comp;

	}
	
	
	@Override
	protected String getExtendedInfo() {
		return "A source which reads a spec file and sends each scan as a message.";
	}

	public File getSourceFile() {
		
		String sourcePath = getReplacedPath();
		if (isPathRelative) {
			final IResource res = ResourcesPlugin.getWorkspace().getRoot().findMember(sourcePath, true);
			if (res!=null) sourcePath = res.getLocation().toOSString();
		}
		
        getStandardMessageHeaders().put(ManagedMessage.SystemHeader.HEADER_SOURCE_INFO,sourcePath);

		return new File(sourcePath);
	}
	
	private Object getResource() {
		
		if (isPathRelative) {
			String sourcePath = getReplacedPath();
			final IResource res = ResourcesPlugin.getWorkspace().getRoot().findMember(sourcePath, true);
			if (res!=null) return res;
		} 
		return getSourceFile();
	}
	
	private String getReplacedPath() {
		
		String filePath = null;
		if (lastTriggered!=null) {
			
			try {
				DataMessageComponent comp = MessageUtils.coerceMessage(lastTriggered);
				filePath = ParameterUtils.getSubstituedValue(this.path, comp);
			} catch (Exception e) {
				logger.error("Cannot substitute parameter "+path, e);
			}
		} else {
			filePath = this.path.getExpression();
		}
		return filePath;
	}

	private String getResourceTypeName() {
		final File file = getSourceFile();
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
		
		final List<IVariable> ret = super.getOutputVariables();
		ret.add(new Variable("file_path", VARIABLE_TYPE.PATH, getSourceFile().getAbsolutePath(), String.class));
		ret.add(new Variable("file_name", VARIABLE_TYPE.SCALAR, getSourceFile().getName(), String.class));
		ret.add(new Variable("file_dir",  VARIABLE_TYPE.PATH, getSourceFile().getParent(), String.class));

		return ret;
	}
	
	private boolean triggeredOnce = false;

	@Override
	protected boolean mustWaitForTrigger() {
		if (!isTriggerConnected()) return false;
		if (!triggeredOnce)        return true;
		return scanQueue.isEmpty();
	}
	
	/**
	 * "callback"-method that can be overridden by TriggeredSource implementations,
	 * if they want to act e.g. on the contents of a received trigger message.
	 * 
	 * @param triggerMsg
	 */
	protected void acceptTriggerMessage(ManagedMessage triggerMsg) {
		triggeredOnce = true;
		startScanParsing(triggerMsg);
	}

}
