/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.editors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.dawb.common.ui.plot.AbstractPlottingSystem;
import org.dawb.common.ui.plot.EmptyWorkbenchPart;
import org.dawb.common.ui.plot.IPlottingSystem;
import org.dawb.common.ui.plot.PlotType;
import org.dawb.common.ui.plot.PlottingFactory;
import org.dawb.common.ui.plot.region.IRegion;
import org.dawb.common.ui.plot.region.RegionUtils;
import org.dawb.common.ui.plot.tool.IToolChangeListener;
import org.dawb.common.ui.plot.tool.IToolPage;
import org.dawb.common.ui.plot.tool.IToolPage.ToolPageRole;
import org.dawb.common.ui.plot.tool.IToolPageSystem;
import org.dawb.common.ui.plot.tool.ToolChangeEvent;
import org.dawb.common.ui.plot.trace.IImageTrace;
import org.dawb.common.ui.plot.trace.ILineTrace;
import org.dawb.common.ui.plot.trace.ITrace;
import org.dawb.common.ui.widgets.ActionBarWrapper;
import org.dawb.passerelle.actors.Activator;
import org.dawb.workbench.jmx.IDeligateWorkbenchPart;
import org.dawb.workbench.jmx.UserPlotBean;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.roi.ROIBase;
import uk.ac.gda.common.rcp.util.GridUtils;

public class UserPlotRemotePart implements IDeligateWorkbenchPart {

	private static Logger logger = LoggerFactory.getLogger(UserPlotRemotePart.class);
	
	private String                     partName;
	
	private Closeable                  closeable;
	private Queue<Object>              queue;
	private UserPlotBean               userPlotBean, originalUserPlotBean;
	private IPlottingSystem            system;
	private ActionBarWrapper           wrapper;
	
	private Composite                  plotComposite, toolComposite, main;
	private SashForm                   contents;
	private CLabel                     customLabel;

	public UserPlotRemotePart() {		
		try {
			system = PlottingFactory.createPlottingSystem();
		} catch (Exception e) {
			logger.error("Cannot create plotting system!", e);
		}
	}
	
	@Override
	public void createRemotePart(final Object container, Closeable closeable) {
		
		this.main  = new Composite((Composite)container, SWT.NONE);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		final GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.verticalSpacing = 0;
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		gridLayout.horizontalSpacing = 0;
		main.setLayout(gridLayout);
		
		this.customLabel  = new CLabel(main, SWT.WRAP);
		customLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		FontData fontDatas[] = customLabel.getFont().getFontData();
		FontData data = fontDatas[0];
		customLabel.setFont(new Font(Display.getCurrent(), data.getName(), data.getHeight(), SWT.BOLD));

		final Image image = Activator.getImageDescriptor("icons/information.gif").createImage();
		customLabel.setImage(image);
		GridUtils.setVisible(customLabel, false);
		
		this.contents = new SashForm(main, SWT.HORIZONTAL);
		contents.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		this.closeable = closeable;
				
		Composite plot = new Composite(contents, SWT.BORDER);
		plot.setLayout(new GridLayout(1, false));
		GridUtils.removeMargins(plot);
		
		if (closeable instanceof IEditorPart) {
			this.wrapper = ActionBarWrapper.createActionBars(plot, ((IEditorPart)closeable).getEditorSite().getActionBars());
		} else if (closeable instanceof Dialog) {
			this.wrapper = ActionBarWrapper.createActionBars(plot, null);			
		}
		this.plotComposite = new Composite(plot, SWT.BORDER); // Used to plot on to.
		plotComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		plotComposite.setLayout(new FillLayout());
	
		this.toolComposite = new Composite(contents, SWT.BORDER);
		toolComposite.setLayout(new StackLayout());
		
		// Show tools here, not on a page.
		((IToolPageSystem)system).setToolComposite(toolComposite);
		
		contents.setWeights(new int[]{100,0});
		
	}
	
    /**
     * Please called setPartName
     * @param bean
     */
	private void createPlot(UserPlotBean bean) {
		
		IWorkbenchPart part = closeable instanceof IWorkbenchPart ? (IWorkbenchPart)closeable : null;
		if (part==null)  part = new EmptyWorkbenchPart(system);
		
		system.createPlotPart(plotComposite, bean.getPartName(), wrapper, PlotType.XY, part);
		
		if (bean.getData()!=null) {
			// Plot whatever was in the bean, if one 2D dataset is encountered use that
			// since it is exclusive.
			final Map<String,Serializable> data = new HashMap<String, Serializable>(bean.getData());
			if (bean.getDataNames()!=null && !bean.getDataNames().isEmpty()) {
				data.keySet().retainAll(bean.getDataNames());
			}
			final AbstractDataset image = getFirst2DDataset(data);
			// TODO Other plotting modes
			// TODO Plot title?
			if (image!=null) { // We plot in 2D
				system.createPlot2D(image, getAxes(bean), null);
			} else { // We plot in 1D
				system.createPlot1D(getXAxis(bean), get1DDatasets(data), null);
			}
		}
		
	    // Regions
		if (bean.getRois()!=null) {
			for (String roiName : bean.getRois().keySet()) {
				final ROIBase roi = (ROIBase)bean.getRois().get(roiName);
				try {
					IRegion region = RegionUtils.createRegion(system, roi, roiName);
					if (region==null) {
						logger.error("Cannot create region '"+roiName+"' with ROI "+roi);
					} else {
						logger.trace("Ceated "+region.getRegionType()+" region '"+roiName+"' with ROI "+roi);
					}
				 
				} catch (Exception e) {
					logger.error("Cannot create region '"+roiName+"'", e);
				}
			}
		}
		
		// Description
		if (bean.getDescription()!=null) {
			customLabel.setText(bean.getDescription());
			GridUtils.setVisible(customLabel, true);
			main.layout();
		}
		
		// Tool
		final AbstractPlottingSystem asystem = (AbstractPlottingSystem)system;
		if (bean.getToolId()!=null && asystem.getToolPage(bean.getToolId())!=null) {
			
			final IToolPage    page = asystem.getToolPage(bean.getToolId());
			final ToolPageRole role = page.getToolPageRole();
			page.setToolData(bean);
			try {
				asystem.setToolVisible(bean.getToolId(), role, null);
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						contents.setWeights(new int[]{50,50});
						contents.layout();
					}
				});
				
			} catch (Exception e) {
				logger.error("Cannot select tool '"+bean.getToolId()+"'", e);
			}
		} else {
			asystem.addToolChangeListener(new IToolChangeListener() {	
				@Override
				public void toolChanged(ToolChangeEvent evt) {
					contents.setWeights(new int[]{50,50});
					contents.layout();
					asystem.removeToolChangeListener(this);
				}
			});
		}
		
		if (wrapper!=null) wrapper.update(true);
		plotComposite.layout(plotComposite.getChildren());
		main.layout(main.getChildren());
	}

	private List<AbstractDataset> getAxes(UserPlotBean bean) {
		final List<String>    axes  = bean.getAxesNames();
		if (axes==null) return null;
		if (!bean.getData().containsKey(axes.get(0))) return null;
		if (!bean.getData().containsKey(axes.get(1))) return null;
		return Arrays.asList((AbstractDataset)bean.getData().get(axes.get(0)), (AbstractDataset)bean.getData().get(axes.get(1)));
	}
	private AbstractDataset getXAxis(UserPlotBean bean) {
		final List<String>    axes  = bean.getAxesNames();
		if (axes==null) return null;
		if (!bean.getData().containsKey(axes.get(0))) return null;
		return (AbstractDataset)bean.getData().get(axes.get(0));
	}

	private AbstractDataset getFirst2DDataset(Map<String, Serializable> data) {
		for (String name : data.keySet()) {
			Serializable d = data.get(name);
			if (d instanceof AbstractDataset) {
				final int rank = ((AbstractDataset)d).getRank();
				if (rank==2) return (AbstractDataset)d;
			}
		}
		return null;
	}
	private List<AbstractDataset> get1DDatasets(Map<String, Serializable> data) {
		List<AbstractDataset> ret = new ArrayList<AbstractDataset>(7);
		for (String name : data.keySet()) {
			Serializable d = data.get(name);
			if (d instanceof AbstractDataset) {
				final int rank = ((AbstractDataset)d).getRank();
				if (rank==1) ret.add( (AbstractDataset)d );
			}
		}
		return ret;
	}

	/**
	 * Create the actions.
	 */
	public void initializeMenu(Object bars) {
		MenuManager man = new MenuManager();
		man.add(confirm);
		man.add(stop);
	}
	
	@Override
	public void setConfiguration(String configurationXML) throws Exception {
		throw new RuntimeException(getClass().getName()+" does not support configuration xml yet!");
	}


	public void dispose() {
			
		if (queue!=null) {
			queue.clear();
			if (queue!=null) {
				// Just in case something is waiting
				// An empty one cancels the message.
				if (queue.isEmpty()) queue.add(new UserPlotBean());
			}
		}
		this.queue          = null;
	}

	/**
	 * Queue must not be null and is cleared prior to using.
	 */
	public void setQueue(Queue<Object> queue) {
		this.queue = queue;
		queue.clear();
	}
	
	public void setValues(final Map<String, String> inputValues) {
		throw new RuntimeException("Set values as a Map of strings it not supported for Plotting!");
	}

	// Actions used by class
	protected final Action confirm = new Action("Confirm values, close view and continue workflow.", Activator.getImageDescriptor("icons/application_form_confirm.png")) {
		public void run() {
			doConfirm();
			closeable.close();
		}
	};
	
	// Actions used by class
	protected final Action stop = new Action("Stop workflow downstream of this node.", Activator.getImageDescriptor("icons/stop_workflow.gif")) {
		public void run() {
			doStop();
			closeable.close();
		}
	};
	

	public String getPartName() {
		return partName;
	}

	protected void doConfirm() {
		userPlotBean = createUserPlotBean();
		if (queue==null || userPlotBean==null) {
			MessageDialog.open(MessageDialog.INFORMATION, Display.getCurrent().getActiveShell(),
					           "Cannot confirm", "The workflow is not waiting for you to confirm these values.\n\nThere is currently nothing to confirm.", SWT.NONE);
			return;
		}
		if (queue.isEmpty()) queue.add(userPlotBean);
	}
	
	/**
	 * Reads plot data. Sends back everything it can.
	 * This may be slower but it is hard to predict which data the
	 * workflow needs. If speed becomes an issue we will change the 
	 * actor to specify which data should be returned.
	 * 
	 * @return
	 */
	private UserPlotBean createUserPlotBean() {
		

		final UserPlotBean ret = new UserPlotBean();
		
		// Send back any data not in the original message
		final Collection<ITrace> traces = system.getTraces();
		if (traces!=null) {
			for (ITrace iTrace : traces) {
				
				if (iTrace instanceof IImageTrace) {
					IImageTrace image = (IImageTrace)iTrace;
					if (image.getAxes()!=null) {
						ret.clearAxisNames();
					    final List<AbstractDataset> axes = image.getAxes();
					    for (AbstractDataset axis : axes) {
						    ret.addList(axis.getName(), axis);
						    ret.addAxisName(axis.getName());
					    }
					}
					AbstractDataset data =  iTrace.getData();
					data.setName(image.getName());
					data.setMetadata(null); // Gives some error with Serialization with Diffraction metadata.
					ret.addList(image.getName(), data);

				} else if (iTrace instanceof ILineTrace) { 
					ILineTrace line = (ILineTrace)iTrace;
					AbstractDataset data =  line.getYData();
 				    data.setName(line.getName());
					ret.addList(line.getName(), data);
				    
					// TODO Fit tool does not give datasets for the
					// peaks drawn to the same resolution as the original data.
					// Perhaps use some kind of downsampling to make the same?
					
				}
			}
		}
		
		final Collection<IRegion> regions = system.getRegions();
		if (regions!=null) {
			for (IRegion iRegion : regions) {
                ret.addRoi(iRegion.getName(), iRegion.getROI());
			}
		}

		IToolPageSystem tsystem = (IToolPageSystem)system;
		// Get the data from the tool required by the actor.
		if (originalUserPlotBean.getToolId()!=null) {
			IToolPage       tool    = tsystem.getToolPage(originalUserPlotBean.getToolId());
			if (tool!=null) ret.setToolData(tool.getToolData());
		} else if (tsystem.getActiveTool()!=null){ 
			// Set the data for any tool which was selected and also set the id for this tool.
			final IToolPage tool = tsystem.getActiveTool();
			if (tool!=null) {
				ret.setToolId(tool.getToolId());
				ret.setToolData(tool.getToolData());
			}
		}
		
		if (ret.getToolData()!=null && ret.getToolData() instanceof UserPlotBean) {
			UserPlotBean toolData = (UserPlotBean)ret.getToolData();
			ret.merge(toolData);
		}
		
		return ret;
	}

	protected void doStop() {
		if (queue.isEmpty()) queue.add(new UserPlotBean()); // null means stop for this part
	}

	public void setPartName(String partName) {
		this.partName = partName;
	}

	@Override
	public void setUserObject(Object userObject) {
		this.originalUserPlotBean = (UserPlotBean)userObject;
		createPlot(originalUserPlotBean);
	}


	@Override
	public void stop() {
		doStop();
	}


	@Override
	public void confirm() {
		doConfirm();
	}


	@Override
	public Object getViewer() {
		return system;
	}
	@Override
	public boolean isMessageOnly() {
		return false;
	}
	@Override
	public void setRemoteFocus() {
		try {
			((AbstractPlottingSystem)system).setFocus();
		} catch (Throwable ne) {
			logger.error("Cannot set focus!", ne);
		}
	}

}
