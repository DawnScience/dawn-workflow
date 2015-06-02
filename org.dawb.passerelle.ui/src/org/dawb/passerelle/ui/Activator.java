package org.dawb.passerelle.ui;

import java.util.Hashtable;

import org.dawb.common.services.IUserInputService;
import org.dawb.passerelle.ui.editors.UserInputService;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.dawb.passerelle.ui"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	private BundleContext context;
	
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
		this.context = context;
		
		Hashtable<String, String> props = new Hashtable<String, String>(1);
		props.put("description", "A service used to input tell the workbench to show input data dialog.");
		context.registerService(IUserInputService.class, new UserInputService(), props);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		this.context = null;
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

	/**
	 * Looks for OSGI service, used by ServiceManager
	 * 
	 * @param clazz
	 * @return
	 */
	public static Object getService(Class<?> clazz) {
		if (plugin.context==null) return null;
		ServiceReference<?> ref = plugin.context.getServiceReference(clazz);
		if (ref==null) return null;
		return plugin.context.getService(ref);
	}


}
