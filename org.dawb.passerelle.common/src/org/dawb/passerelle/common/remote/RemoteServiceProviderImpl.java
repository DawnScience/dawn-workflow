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

import org.dawb.workbench.jmx.IRemoteServiceProvider;
import org.dawb.workbench.jmx.IRemoteWorkbench;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/**
 * Dummy class with mostly returns null but gives a common interface between running a workflow
 * from eclipse and from a separate java instance.
 * 
 * @author gerring
 *
 */
public class RemoteServiceProviderImpl implements IRemoteServiceProvider {
	
	@Override 
	public int getStartPort(){
		final IPreferenceStore store = new ScopedPreferenceStore(InstanceScope.INSTANCE, "org.dawb.passerelle.common");
		return store.getInt("org.dawb.workbench.application.preferences.workbench.port");
	}

	public IRemoteWorkbench getRemoteWorkbench() throws Exception {
		return new RemoteWorkbenchImpl();
	}	
	
	@Override
	public String getWorkspacePath() {
		return null;
	}

	@Override
	public String getModelPath() {
		return null;
	}

	@Override
	public String getInstallationPath() {
		return null;
	}

	@Override
	public boolean getTangoSpecMockMode() {
		return true;
	}

	@Override
	public boolean getServiceTerminate() {
		return false;
	}

}
