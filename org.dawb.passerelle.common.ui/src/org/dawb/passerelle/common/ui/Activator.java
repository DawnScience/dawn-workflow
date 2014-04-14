package org.dawb.passerelle.common.ui;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.dawb.passerelle.common.remote.WorkbenchServiceManager;

public class Activator extends AbstractUIPlugin {

	private BundleContext context;
	private static Activator     activator;

	public Activator() {
		// TODO Auto-generated constructor stub
	}

	public static Activator getDefault() {
		return activator;
	}
	
    public void start(BundleContext context) throws Exception {
        super.start(context);
		this.context = context;
		activator = this;
    }
    
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
		this.context = null;
		activator = null;
		WorkbenchServiceManager.stopWorkbenchService();
    }

}
