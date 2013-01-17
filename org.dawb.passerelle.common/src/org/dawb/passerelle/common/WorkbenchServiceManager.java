/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.common;

import org.dawb.common.services.IClassLoaderService;
import org.dawb.common.services.ServiceManager;
import org.dawb.passerelle.common.remote.RemoteServiceProviderImpl;
import org.dawb.workbench.jmx.RemoteWorkbenchAgent;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.PlatformUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a service manager for the service which runs giving workflow actors
 * the ability to interact with the UI.
 * 
 * @author fcp94556
 *
 */
public class WorkbenchServiceManager implements IStartup {

	private static Logger logger = LoggerFactory.getLogger(WorkbenchServiceManager.class);

	/**
	 * Required so that no more than 1 workbench service is started per workbench.
	 */
    private static boolean addedWorkbenchListener = false;
    
    /**
     * The current active agent.
     */
	private static RemoteWorkbenchAgent agent;

	@Override
	public void earlyStartup() {		
		WorkbenchServiceManager.startWorkbenchService();
	}
	
	public static void startWorkbenchService() {
		WorkbenchServiceManager.startWorkbenchService(true);
	}
	public static void startTestingWorkbenchService() {
		WorkbenchServiceManager.startWorkbenchService(false);
	}

	/**
	 * Starts workbench service if that is needed, otherwise does nothing.
	 */
	private static void startWorkbenchService(final boolean checkUI) {
		
		if (agent!=null) return;
		
		IClassLoaderService service=null;
		try {
			
			service = (IClassLoaderService)ServiceManager.getService(IClassLoaderService.class, false);
			
			if (checkUI) {
				if (!PlatformUI.isWorkbenchRunning())                      return;
				if (!WorkbenchServiceManager.isUserInterfaceApplication()) return;
			}

			if (service!=null) service.setDataAnalysisClassLoaderActive(true);
			
			logger.debug("Running workbench not workflow, starting workbench service.");

			if (System.getProperty("org.edna.workbench.application.no.service")==null) {

				final RemoteServiceProviderImpl prov = new RemoteServiceProviderImpl();
				try {
					agent = new RemoteWorkbenchAgent(prov);
					agent.start();
				} catch (java.io.IOException alreadyExisting) {
					logger.debug("The service for the workbench is already running.");
				} catch (Exception e) {
					logger.error("Cannot start workbench service!", e);
				}


				if (!addedWorkbenchListener) {

					addedWorkbenchListener = true;
					if (PlatformUI.isWorkbenchRunning()) PlatformUI.getWorkbench().addWorkbenchListener(new IWorkbenchListener() {

						@Override
						public void postShutdown(final IWorkbench workbench) {
							stopWorkbenchService();
						}

						@Override
						public boolean preShutdown(IWorkbench workbench,
								boolean forced) {
							return true;
						}
					});
				}

			}
		} catch (Exception ne) {
			logger.error("Cannot create workbench service!", ne);
			
		} finally {
			if (service!=null) service.setDataAnalysisClassLoaderActive(false);
		}

	}
	private static boolean isUserInterfaceApplication() {
				
		if (PlatformUI.getWorkbench()==null)  return false;
		if (PlatformUI.getWorkbench().getDisplay()==null) return false;
		
		final Boolean[] isShells = new Boolean[]{Boolean.FALSE};	
		PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

			@Override
			public void run() {
				if (PlatformUI.getWorkbench().getDisplay().getShells().length>0) {
					isShells[0] = Boolean.TRUE;
				}
				
			}	
		});
		if (!isShells[0])  return false;
		
		return true;
	}

	public static void stopWorkbenchService() {
		try {
			if (agent!=null) {
				agent.stop();
				agent = null;
			}
		} catch (Throwable ignored) {
			// No more exceptions! we are exiting.
		}
	}

}
