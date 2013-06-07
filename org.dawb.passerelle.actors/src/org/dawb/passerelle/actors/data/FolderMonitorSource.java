/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
// This class was coded at Diamond Light Source Ltd. copyright is owned by them
package org.dawb.passerelle.actors.data;

import java.io.File;
import java.io.FileFilter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dawb.common.util.io.FileUtils;
import org.dawb.common.util.io.SortingUtils;
import org.dawb.passerelle.common.actors.AbstractDataMessageSource;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.IVariable.VARIABLE_TYPE;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.passerelle.common.message.Variable;
import org.dawb.passerelle.common.parameter.ParameterUtils;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.gmf.runtime.common.core.util.StringMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.data.BooleanToken;
import ptolemy.data.IntToken;
import ptolemy.data.expr.Parameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;

import com.isencia.passerelle.actor.InitializationException;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.util.EnvironmentUtils;
import com.isencia.passerelle.util.ptolemy.RegularExpressionParameter;
import com.isencia.passerelle.util.ptolemy.ResourceParameter;
import com.isencia.util.StringConvertor;

/**
 * This source will look at a directory, if files appear in this directory with a 
 * newer timestamp than when the monitor was started, their paths will be fired
 * into the pipeline. The class does a listFiles() and with a filter every Checking Frequency
 * (default 100ms) therefore listFiles() should be fairly fast on the monitored directory.
 * 
 * Normally the source simply passes file_path, file_dir and file_name onwards for each
 * new file seen. If no new files the source will sit waiting for ever until the workflow
 * is stopped or the Inactive after time is reached.
 * 
 * @author fcp94556
 *
 */
public class FolderMonitorSource extends AbstractDataMessageSource {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7377188139156058630L;

	private static final Logger logger = LoggerFactory.getLogger(FolderMonitorSource.class);
	
	/**
	 * 
	 */
	private final ResourceParameter  folderPath;

	/**
	 * Realtive path or no
	 */
	protected Parameter           relativePathParam;
	
	/**
	 * The data will not be fired into the pipeline greater than this rate.
	 */
	private Parameter sourceFreq;

	/**
	 * After this period the source will exit and stop checking
	 */
	private Parameter inactiveFreq;
	
	/**
	 * Parameter to filter the file names
	 */
	private RegularExpressionParameter filterParam;
	
	/**
	 * Queue of files for pipeline
	 */
	private List<TriggerObject> fileQueue;

    /**
     * Trigger message
     */
	private ManagedMessage triggerMsg;

	/**
	 * Time the directory started monitoring.
	 */
	private long initTime;

	/**
	 * Files already reported. This memory leaks a little during the running 
	 * of the pipeline if many files with the same name are overwritten.
	 */
	private Set<FileObject> reportedFiles;
	

	public FolderMonitorSource(CompositeEntity container, String name) throws Exception {	
		
		super(container, name);
		
		folderPath = new ResourceParameter(this, "Folder", "Folder to Monitor");
		folderPath.setResourceType(IResource.FOLDER);
		try {
			URI baseURI = new File(StringConvertor.convertPathDelimiters(EnvironmentUtils.getApplicationRootFolder())).toURI();
			folderPath.setBaseDirectory(baseURI);
		} catch (Exception e) {
			logger.error("Cannot set base directory for "+getClass().getName(), e);
		}
		registerConfigurableParameter(folderPath);

		this.sourceFreq = new Parameter(this, "Checking Frequency", new IntToken(100));
		registerConfigurableParameter(sourceFreq);

		this.inactiveFreq = new Parameter(this, "Inactive After", new IntToken(-1));
		registerExpertParameter(inactiveFreq);
       
		this.relativePathParam = new Parameter(this, "Relative Path", new BooleanToken(true));
		registerConfigurableParameter(relativePathParam);
		
		this.filterParam  = new RegularExpressionParameter(this, "File Filter", true);
		registerConfigurableParameter(filterParam);

	}

	protected void acceptTriggerMessage(ManagedMessage triggerMsg) {
		this.triggerMsg = triggerMsg;
		appendQueue(triggerMsg);
	}
	
	@Override
	public void doPreInitialize() {
		fileQueue      = null;
		initTime       = -1;
		reportedFiles  = null;
	}

	@Override
	protected void doInitialize() throws InitializationException {
	
		super.doInitialize();
		
		this.initTime  = System.currentTimeMillis();
		this.fileQueue = new ArrayList<TriggerObject>(89);
		this.reportedFiles = new HashSet<FileObject>(89);
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
				final File[]     fa       = getNewerFileList(file);
				if (fa==null||fa.length<1) return;
				
				final List<File> fileList = SortingUtils.getSortedFileList(fa, SortingUtils.DEFAULT_COMPARATOR);
				for (File f : fileList) {				
					if (!isFileLegal(f)) continue;
					final TriggerObject ob = new TriggerObject();
					ob.setTrigger(triggerMsg);
					ob.setFile(f);
					fileQueue.add(ob);
				}
			} else {
				throw new RuntimeException("Must have a directory not "+file);
			}
		}
	}
	
	private File[] getNewerFileList(File file) {
		return file.listFiles(new FileFilter() {		
			@Override
			public boolean accept(File child) {
				if (reportedFiles.contains(new FileObject(child))) return false;
				final long lm = child.lastModified();
				return lm>initTime;
			}
		});
	}

	private boolean isFileLegal(File file) {
		
		if (file.isDirectory())                  return false;
		if (file.isHidden())                     return false;
		if (file.getName().startsWith("."))      return false;
	    if (!isRequiredFileName(file.getName())) return false;		   
        return true;
   }
	
   private boolean isRequiredFileName(String fileName) {
	   
	    final String fileFilter = filterParam.getExpression();
    	if (fileFilter==null || "".equals(fileFilter)) return true;
		if (filterParam.isJustWildCard()) {
			final StringMatcher matcher = new StringMatcher(fileFilter, false, false);
		    return matcher.match(fileName);
		} else {
			return fileName.matches(fileFilter);
		}
	}

	
	@Override
	protected ManagedMessage getDataMessage() throws ProcessingException {
		
		try {

			if (fileQueue == null)   return null;
			if (fileQueue.isEmpty()) return null;
			
			if (isFinishRequested()) {
				fileQueue.clear();
				return null;
			}
			
			final DataMessageComponent  ret = new DataMessageComponent();
			final TriggerObject        file = fileQueue.remove(0);
			reportedFiles.add(new FileObject(file.getFile()));
			ret.putScalar("file_path", file.getFile().getAbsolutePath());
			ret.putScalar("file_name", file.getFile().getName());
			ret.putScalar("file_dir",  file.getFile().getParent());
			
			if (triggerMsg!=null) {
				try {
					final DataMessageComponent c = MessageUtils.coerceMessage(triggerMsg);
					ret.addScalar(c.getScalar());
				} catch (Exception ignored) {
					logger.info("Trigger for "+getName()+" is not DataMessageComponent, no data added.");
				}
			}
			
			return MessageUtils.getDataMessage(ret, null);
			
		} catch (Exception ne) {
			throw createDataMessageException("Cannot extract shared memory", ne);
		
		}
	}

	public boolean hasNoMoreMessages() {
	    if (fileQueue == null)   return true;
        return fileQueue.isEmpty() && super.hasNoMoreMessages();
    }

	protected boolean doPostFire() throws ProcessingException {
		try {
			if (isFinishRequested()) return super.doPostFire();
			
			int totalTime = ((IntToken)inactiveFreq.getToken()).intValue();
			final int freq = ((IntToken)sourceFreq.getToken()).intValue();
			
			// 0 or less means wait for ever
			if (totalTime<0 && System.getProperty("org.dawb.test.session")!=null) totalTime = 1000;
			if (totalTime<0) totalTime = Integer.MAX_VALUE;
			int waitedTime  = 0;
			while (waitedTime<totalTime && !isFinishRequested() && fileQueue!=null && fileQueue.isEmpty()) { 
				try {
					Thread.sleep(freq);
					waitedTime+=freq;

			        appendQueue(triggerMsg);
				} catch (InterruptedException ne) {
					break;
				}
			}
			
		} catch (Exception e) {
			throw new ProcessingException("Cannot wait for source frequency time after last data!", this, e);
		}
	    return super.doPostFire();
	}
	
	
	@Override
	protected String getExtendedInfo() {
		return "Actor to monitor a directory for new files.";
	}
	

	
	@Override
	public List<IVariable> getOutputVariables() {
		
		try {
		    final List<IVariable>    ret  = super.getOutputVariables();
		    
		    String sourcePath = getSourcePath(null);
			ret.add(new Variable("file_path", VARIABLE_TYPE.PATH,   sourcePath, String.class));
			ret.add(new Variable("file_name", VARIABLE_TYPE.SCALAR, sourcePath != null ? new File(sourcePath).getName()+"/*" : "Monitored file...", String.class));
			ret.add(new Variable("file_dir",  VARIABLE_TYPE.PATH, sourcePath != null ? FileUtils.getDirectory(getSourcePath(null)) : "Monitored directory", String.class));
		
			return ret;
			
		} catch (Exception e) {
			logger.error("Cannot read variables", e);
			return null;
		}

	}
	
	private String getSourcePath(final ManagedMessage manMsg) {

		String sourcePath=null;
		try {
			final DataMessageComponent comp = manMsg!=null ? MessageUtils.coerceMessage(manMsg) : null;
			sourcePath = ParameterUtils.getSubstituedValue(this.folderPath, comp);
		} catch (Exception e) {
			logger.error("Cannot substitute parameter "+folderPath, e);
		}

		boolean isPathRelative = false;
		try {
			isPathRelative = ((BooleanToken)relativePathParam.getToken()).booleanValue();
		} catch (IllegalActionException e) {
			logger.error("Cannot deal with if path is relative! Assuming it is not and carrying on.");
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


	@Override
	protected boolean mustWaitForTrigger() {
		if (triggerMsg!=null) return false;
		return trigger.getWidth()>0;
	}
	
	private final class FileObject {
		
		private File file;
		private long lastMod;
		public FileObject(File file) {
			this.file    = file;
			this.lastMod = file.lastModified();
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((file == null) ? 0 : file.hashCode());
			result = prime * result + (int) (lastMod ^ (lastMod >>> 32));
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			FileObject other = (FileObject) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (file == null) {
				if (other.file != null)
					return false;
			} else if (!file.equals(other.file))
				return false;
			if (lastMod != other.lastMod)
				return false;
			return true;
		}
		private FolderMonitorSource getOuterType() {
			return FolderMonitorSource.this;
		}
	}
	
}
