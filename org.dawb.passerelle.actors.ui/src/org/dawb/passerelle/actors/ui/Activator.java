package org.dawb.passerelle.actors.ui;

import org.dawb.passerelle.actors.data.DataAppendTransformer;
import org.dawb.passerelle.actors.data.DataChunkSource;
import org.dawb.passerelle.actors.data.DataImportSource;
import org.dawb.passerelle.actors.data.Expression;
import org.dawb.passerelle.actors.data.FolderImportSource;
import org.dawb.passerelle.actors.data.Scalar;
import org.dawb.passerelle.actors.data.config.SliceParameter;
import org.dawb.passerelle.actors.flow.ExpressionParameter;
import org.dawb.passerelle.actors.flow.If;
import org.dawb.passerelle.actors.flow.TimerSource;
import org.dawb.passerelle.actors.net.TelnetTransformer;
import org.dawb.passerelle.actors.roi.ROISource;
import org.dawb.passerelle.actors.ui.config.FieldParameter;
import org.dawb.passerelle.actors.ui.file.SubstituteTransformer;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.isencia.passerelle.ext.ModelElementClassProvider;
import com.isencia.passerelle.ext.impl.DefaultModelElementClassProvider;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.dawb.passerelle.actors.ui"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

  private ServiceRegistration<?> apSvcReg;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@SuppressWarnings("unchecked")
  public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
    apSvcReg = context.registerService(ModelElementClassProvider.class.getName(), 
        new DefaultModelElementClassProvider(
            DataAppendTransformer.class,
            DataChunkSource.class,
            DataImportSource.class,
            Expression.class,
            FolderImportSource.class,
            Scalar.class,
            SliceParameter.class,
            ExpressionParameter.class,
            If.class,
            TimerSource.class,
            org.dawb.passerelle.actors.ifdynaport.ExpressionParameter.class,
            org.dawb.passerelle.actors.ifdynaport.If.class,
            TelnetTransformer.class,
            ROISource.class,
            MessageSink.class,
            UserInputSource.class,
            UserModifyTransformer.class,
            UserPlotTransformer.class,
            FieldParameter.class,
            SubstituteTransformer.class
            ), null);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
    apSvcReg.unregister();
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

}
