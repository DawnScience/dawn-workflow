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

import org.eclipse.core.runtime.IBundleGroup;
import org.eclipse.core.runtime.IBundleGroupProvider;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.prefs.Preferences;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator implements BundleActivator {

	
	// The plug-in ID
	public static final String PLUGIN_ID = "org.dawb.passerelle.common";

	// The shared instance
	private static Activator     plugin;
	private static BundleContext context;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugins#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		//super.start(context);
		plugin       = this;
		this.context = context;
		
		if (System.getProperty("dawn.workbench.version")==null) {
			String version  = null;
			try {
				IBundleGroupProvider[] providers = Platform.getBundleGroupProviders();  
				if (providers != null) {  
					MAIN_LOOP: for (IBundleGroupProvider provider : providers) {  
						IBundleGroup[] bundleGroups = provider.getBundleGroups();  
						for (IBundleGroup group : bundleGroups) {  
							if (group.getIdentifier().equals("org.dawnsci.base.product.feature")) {  
								version = group.getVersion();  
								break MAIN_LOOP;
							}  
						}  
					}  
				}   
			} catch (Throwable ignored) {
				// Intentionally ignore any error
				version = null;
			}
			if (version==null) version = context.getBundle().getVersion().toString();
			System.setProperty("dawn.workbench.version", version);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		//super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	public static Object getService(Class<?> clazz) {
		if (plugin==null) return null;
		ServiceReference<?> ref = context.getServiceReference(clazz);
		if (ref==null) return null;
		return context.getService(ref);
	}
	

	public static Preferences getPreferences() {
		IPreferencesService service = Platform.getPreferencesService();
		Preferences store = service.getRootNode().node(InstanceScope.SCOPE).node(PLUGIN_ID);
		return store;
	}

}
