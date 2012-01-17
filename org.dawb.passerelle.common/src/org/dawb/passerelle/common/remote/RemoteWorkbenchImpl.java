/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.common.remote;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.dawb.common.services.IHardwareService;
import org.dawb.common.services.IUserInputService;
import org.dawb.common.services.ServiceManager;
import org.dawb.common.ui.plot.IPlottingSystemSelection;
import org.dawb.common.ui.util.EclipseUtils;
import org.dawb.common.ui.views.ImageMonitorView;
import org.dawb.passerelle.common.Activator;
import org.dawb.passerelle.common.utils.ModelListener;
import org.dawb.workbench.jmx.IRemoteWorkbench;
import org.dawb.workbench.jmx.RemoveWorkbenchPart;
import org.dawb.workbench.jmx.UserInputBean;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.ui.progress.UIJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.isencia.passerelle.workbench.model.editor.ui.editor.PasserelleModelMultiPageEditor;
/**
 * 
 * Class to interact with various plugins running in the workbench UI.
 * 
 * @author gerring
 *
 */
public class RemoteWorkbenchImpl implements IRemoteWorkbench {


	private static final Logger logger = LoggerFactory.getLogger(RemoteWorkbenchImpl.class);
	
	@Override
	public void executionStarted() {
		ModelListener.notifyExecutionStarted();
	}
	@Override
	public void executionTerminated(final int returnCode) {
		ModelListener.notifyExecutionTerminated(returnCode);
	}
	
	/**
	 * Call with full file path on visible disk.
	 */
	@Override
	public boolean openFile(final String fullPath) {
		
		final List<Object> obs = new ArrayList<Object>(1);

		logger.debug("openFile @ "+fullPath);
		
		if (!PlatformUI.isWorkbenchRunning()) return false;
		
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				try {
					
					// Try to open file in project, otherwise open external file.
					final String workspacePath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
					logger.debug("workspace path is "+workspacePath);

					IResource res = ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(fullPath));				
					if (res==null) {
						String localPath;
						try {
							localPath = fullPath.substring(workspacePath.length());
						} catch (StringIndexOutOfBoundsException ne) {
							localPath = fullPath;
						}
                        res = ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(localPath));
					}
					if (res==null) {
                        res = ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(workspacePath+fullPath));
					}
					
					Object ob;
					logger.debug("resource to open requested: "+res);
					if (res!=null && res instanceof IFile) {
						logger.debug("opening IFile editor for "+fullPath);
						final IEditorInput input = new FileEditorInput((IFile)res);
						ob = EclipseUtils.getPage().findEditor(input);
						if (ob==null) {
							ob = EclipseUtils.openEditor((IFile)res);
						}
						
					} else {
						String externalPath = fullPath;
						File file = new File(externalPath);
						if (!file.exists()) {
							logger.debug(externalPath+" not found, trying "+workspacePath+externalPath);
							if (new File(workspacePath+externalPath).exists()) {
								file = new File(workspacePath+externalPath);
								externalPath = workspacePath+externalPath;
								logger.debug(externalPath+" found ");
							}
						}
						
						logger.debug("opening external editor for "+externalPath);
						final IFileStore   externalFile = EFS.getLocalFileSystem().fromLocalFile(file);
						final IEditorInput input        = new FileStoreEditorInput(externalFile);
						ob = EclipseUtils.getPage().findEditor(input);
						if (ob==null) {
							ob = EclipseUtils.openExternalEditor(externalPath);
						}
					}
					obs.add(ob);
					
					// If the Editor part is a PlotDataEditor or the selected page is,
					// we plot everything in the file as normally the user wants to see 
					// everything 1D plotted
					if (ob != null) {
						if (ob instanceof IPlottingSystemSelection) {
							final IPlottingSystemSelection prov = (IPlottingSystemSelection)ob;
							final UIJob selectEverything = new UIJob("Show data") {
								@Override
								public IStatus runInUIThread( IProgressMonitor monitor) {
									prov.setAll1DSelected(false);
									return Status.OK_STATUS;
								}
							};
							selectEverything.schedule(50);
						}
					}
					
				} catch (PartInitException e) {
					logger.error("Cannot open file "+fullPath, e);
				}
			}

		});
        return !obs.isEmpty();
	}

	@Override
	public boolean monitorDirectory(final String fullPath, final boolean monitoring) {
		
		final List<Object> obs = new ArrayList<Object>(1);

		logger.debug("monitorDirectory called "+fullPath);
		
		if (!PlatformUI.isWorkbenchRunning()) return false;
		
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				try {
					// Try to open file in project, otherwise open external file.
					final ImageMonitorView view = (ImageMonitorView)EclipseUtils.getActivePage().showView(ImageMonitorView.ID);
					if (view!=null) {
						view.setDirectoryPath(fullPath);
						view.setMonitoring(monitoring);
						obs.add(fullPath);
					}
					
				} catch (PartInitException e) {
					logger.error("Cannot monitor directory "+fullPath, e);
				}
			}

		});
        return !obs.isEmpty();
	}

	@Override
	public boolean refresh(final String projectName, final String resourcePath) {
		
		logger.debug("refresh project called "+resourcePath);
		IResource res = (IResource)ResourcesPlugin.getWorkspace().getRoot().findMember(resourcePath);
		if (res == null) res = (IResource)ResourcesPlugin.getWorkspace().getRoot().findMember(projectName);
        if (res!=null) {
        	try {
        		if (!res.isAccessible()) return false;
        		res.refreshLocal(IResource.DEPTH_INFINITE, null);
	        	return true;
			} catch (CoreException e) {
				logger.error("Cannot refresh project "+res, e);
	        	return false;
			}
        }
    	return false;
	}
	
	@Override
	public boolean showMessage(final String title, final String message, final int type) {
		
		if (!PlatformUI.isWorkbenchRunning()) {
			if (type == MessageDialog.ERROR)       logger.error(title+"> "+message);
			if (type == MessageDialog.WARNING)     logger.warn(title+"> "+message);
			if (type == MessageDialog.INFORMATION) logger.info(title+"> "+message);
			return true;
		}
		
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
		        MessageDialog.open(type, PlatformUI.getWorkbench().getDisplay().getActiveShell(), title, message, SWT.NONE);
			}
		});
    	return true;
	}
	
	@Override
	public void logStatus(final String pluginId, final String message, final Throwable throwable) {
		
		if (!PlatformUI.isWorkbenchRunning()) return;
		
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				try {
					if (EclipseUtils.getActivePage().findView("org.eclipse.pde.runtime.LogView")==null) {
					    EclipseUtils.getActivePage().showView("org.eclipse.pde.runtime.LogView");
					}
				} catch (PartInitException e) {
					// Ignored.
				}
				// This is logged in the error log of the eclipse workbench
				Activator.getDefault().getLog().log(new Status(Status.WARNING, pluginId, message, throwable));
				
				// Clear the actor 
				final IWorkbenchPage     page  = EclipseUtils.getActivePage();
			    final IEditorReference[] parts = page.getEditorReferences();
			    for (IEditorReference ied : parts) {
					if (ied.getId().equals(PasserelleModelMultiPageEditor.ID)) {
						final IEditorPart part = ied.getEditor(false);
						if (part!=null) ((PasserelleModelMultiPageEditor)part).clearActorSelections();
					}
				}
				
			}
		});
    	return;
	}
	
	@Override
	public Map<String,String> createUserInput(final UserInputBean bean) throws Exception {
		
		if (!PlatformUI.isWorkbenchRunning()) return bean.getScalarValues();
		
		if (bean.isSilent()) return bean.getScalarValues();
		
		
		final BlockingQueue<Map<String,String>> queue = new LinkedBlockingQueue<Map<String,String>>(1);
		
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				try {
					
					final IUserInputService service = (IUserInputService)ServiceManager.getService(IUserInputService.class);
					final RemoveWorkbenchPart part  = service.openUserInputPart(bean.getPartName(), bean.isDialog());
					
					if (bean.getPartName()!=null) part.setPartName(bean.getPartName());
					part.setQueue(queue);
					part.setConfiguration(bean.getConfigurationXML());
					part.setValues(bean.getScalarValues());
					
					if (part instanceof Dialog) {
						Dialog dialog = (Dialog)part;
						dialog.open();
						
						if (dialog.getReturnCode()!=Dialog.OK) {
							queue.add(Collections.EMPTY_MAP);
						}
					}
					
				    return; // Queue will be notified when they have chosen
				    
				} catch (PartInitException e) {
					// Ignored.
				} catch (Exception e) {
					logger.error("Cannot open editor ", e);
					queue.add(Collections.EMPTY_MAP);
					return;
				}
				
				// If we drop through to here do nothing
				// adding this to the queue makes the take()
				// call return straight away.
				if (bean.getScalarValues()!=null) {
					queue.add(bean.getScalarValues());
				} else {
					queue.add(Collections.EMPTY_MAP);
				}
			}
		});
		
		final Map<String,String> newValues = queue.take(); // Blocks until user edits this!
    	return newValues;
	}


	@Override
	public boolean setActorSelected(final String  fullPath, 
			                        final String  actorName,
			                        final boolean isSelected,
			                        final int     colorCode) throws Exception {
		

		if (!PlatformUI.isWorkbenchRunning()) return false;
		
		if (Platform.getBundle("org.dawb.workbench.ui")!=null) {
			final ScopedPreferenceStore store = new ScopedPreferenceStore(InstanceScope.INSTANCE,"org.dawb.workbench.ui");
			final boolean isSel = store.getBoolean("org.dawb.actor.highlight.choice");
			if (!isSel) return false;
		}
				
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				
				if (fullPath==null) {
					logger.error("Cannot open editor because fullPath is null");
					return;
				}
				try {
					
					final String workspaceLoc = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
					String resourcePath = fullPath;
					if (fullPath.startsWith(workspaceLoc)) resourcePath = fullPath.substring(workspaceLoc.length());
					
					final IFile          file = (IFile)ResourcesPlugin.getWorkspace().getRoot().findMember(resourcePath);
				    final IEditorInput   input= new FileEditorInput(file);
					final IWorkbenchPage page = EclipseUtils.getActivePage();
				    final PasserelleModelMultiPageEditor ed = (PasserelleModelMultiPageEditor)page.findEditor(input);
                    if (ed!=null) ed.setActorSelected(actorName, isSelected, colorCode);
				    
				} catch (Exception e) {
					logger.error("Cannot open editor ", e);
				}
			}
		});
		
		// TODO fix this to return if the actor was found.
		return true;
	}

	@Override
	public Object getMockMotorValue(String motorName) {
		IHardwareService service;
		try {
			service = (IHardwareService)ServiceManager.getService(IHardwareService.class);
		} catch (Exception e) {
			logger.error("Cannot get the hardware service!", e);
			return null;
		}
		if (service==null) return null;
		return service.getMockValue(motorName);
	}
	@Override
	public void setMockMotorValue(String motorName, Object value) {
		IHardwareService service;
		try {
			service = (IHardwareService)ServiceManager.getService(IHardwareService.class);
		} catch (Exception e) {
			logger.error("Cannot get the hardware service!", e);
			return;
		}
		if (service==null) return;
		service.setMockValue(motorName, value);
	}
	@Override
	public void notifyMockCommand(String motorName, String message, String value) {
		
		IHardwareService service;
		try {
			service = (IHardwareService)ServiceManager.getService(IHardwareService.class);
		} catch (Exception e) {
			logger.error("Cannot get the hardware service!", e);
			return;
		}
		if (service==null) return;
		service.notifyMockCommand(motorName, message, value);
	}
}
