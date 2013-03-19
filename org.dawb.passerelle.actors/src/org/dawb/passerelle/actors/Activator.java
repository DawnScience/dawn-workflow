/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.actors;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.framework.ServiceRegistration;
import ptolemy.kernel.util.NamedObj;

import com.isencia.constants.IPropertyNames;
import com.isencia.passerelle.ext.ModelElementClassProvider;
import com.isencia.passerelle.validation.version.VersionSpecification;



/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	private static final Logger logger = LoggerFactory.getLogger(Activator.class);

	// The plug-in ID
	public static final String PLUGIN_ID = "org.dawb.passerelle.actors";

	// The shared instance
	private static Activator plugin;
	
	private ServiceRegistration apSvcReg;

	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		System.setProperty(IPropertyNames.APP_HOME, ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString());
		logger.debug("Registring service for org.dawb.passerelle.actors");
		apSvcReg = context.registerService(ModelElementClassProvider.class.getName(), new ModelElementClassProvider() {
			public Class<? extends NamedObj> getClass(String className, VersionSpecification versionSpec) throws ClassNotFoundException {
				return (Class<? extends NamedObj>) this.getClass().getClassLoader().loadClass(className);
			}
		}, null);
		
		try {
			final Bundle bundle = Platform.getBundle("org.dawb.gda.extensions");
			if (bundle==null) throw new Exception("Cannot load bundle org.dawb.gda.extensions!");
			bundle.start();
		} catch (Exception e) {
			logger.error("Cannot override plot server in GDA!", e);
		}
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return getImageDescriptor(PLUGIN_ID, path);
	}
	public static ImageDescriptor getImageDescriptor(String plugin,String path) {
		return imageDescriptorFromPlugin(plugin, path);
	}

}
