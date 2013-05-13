package org.dawnsci.workflow.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {

	private static final String PLUGIN_ID = "org.dawnsci.workflow.ui";
	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
	}

	
	  /**
	   * Returns an image descriptor for the image file at the given plug-in relative path
	   * 
	   * @param path
	   *          the path
	   * @return the image descriptor
	   */
	  public static ImageDescriptor getImageDescriptor(String path) {
	    return imageDescriptorFromPlugin(PLUGIN_ID, path);
	  }

}
