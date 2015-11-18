/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 

package org.dawb.passerelle.actors.dawn;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.dawb.passerelle.common.actors.AbstractDataMessageTransformer;
import org.dawb.passerelle.common.message.MessageUtils;
import org.eclipse.dawnsci.analysis.api.dataset.IDataset;
import org.eclipse.dawnsci.analysis.api.message.DataMessageComponent;
import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.analysis.dataset.impl.Dataset;
import org.eclipse.dawnsci.analysis.dataset.roi.EllipticalFitROI;
import org.eclipse.dawnsci.analysis.dataset.roi.EllipticalROI;
import org.eclipse.dawnsci.analysis.dataset.roi.LinearROI;
import org.eclipse.dawnsci.analysis.dataset.roi.PointROI;
import org.eclipse.dawnsci.analysis.dataset.roi.RectangularROI;
import org.eclipse.dawnsci.analysis.dataset.roi.SectorROI;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.PlottingFactory;
import org.eclipse.dawnsci.plotting.api.region.IRegion;
import org.eclipse.dawnsci.plotting.api.region.IRegion.RegionType;
import org.eclipse.dawnsci.plotting.api.tool.IToolPage.ToolPageRole;
import org.eclipse.dawnsci.plotting.api.tool.IToolPageSystem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import uk.ac.diamond.scisoft.analysis.SDAPlotter;
import uk.ac.diamond.scisoft.analysis.rcp.plotting.PlottingMode;

import com.isencia.passerelle.actor.ProcessingException;

public class PlotImageActor	extends AbstractDataMessageTransformer{

	private static final long serialVersionUID = 4457133165062873343L;
	
	private IPlottingSystem<Composite> plottingSystem;

	protected static final List<String> HAS_ROI;
	static {
		HAS_ROI = new ArrayList<String>(2);
		HAS_ROI.add("Plot the image with ROI(s)");
		HAS_ROI.add("Plot the image without ROI(s)");
	}
	protected static final List<String> BOX_ROI_TYPE;
	static {
		BOX_ROI_TYPE = new ArrayList<String>(3);
		BOX_ROI_TYPE.add("Box Region Of Interest");
		BOX_ROI_TYPE.add("X-Axis Selection Region Of Interest");
		BOX_ROI_TYPE.add("Y-Axis Selection Region Of Interest");
	}

	protected static final List<PlottingMode> PLOT_MODE = Arrays.asList(PlottingMode.values());
	
	private StringParameter plotViewName;
	private Parameter boxROITypeParam;
	private Parameter hasROIParam;	
	private Parameter dataNameParam;
	private Parameter xaxisNameParam;
	private Parameter yaxisNameParam;
	private Parameter plotModeParam;

	Logger logger = LoggerFactory.getLogger(PlotImageActor.class);


	public PlotImageActor(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);
		setDescription("This actor won't work unless it is used in the same runtime environment as DAWN. " +
				"To do so an Algorithm Process Page must be implemented to run the workflow." +
				"See the ExampleRunPage class for an example of how to implement it in DAWN.");
		// Plot View name parameter
		plotViewName = new StringParameter(this, "Name");
		plotViewName.setExpression("Plot 1");
		plotViewName.setDisplayName("Plot View Name");
		registerConfigurableParameter(plotViewName);

		// data name parameter
		dataNameParam = new StringParameter(this, "Data Name");
		dataNameParam.setDisplayName("Data Name");
		registerConfigurableParameter(plotViewName);

		plotModeParam = new StringParameter(this, "Plot Mode"){
			private static final long serialVersionUID = 2815254879307619914L;
			public String[] getChoices() {
				String[] modes = new String[PLOT_MODE.size()];
				for (int i = 0; i < modes.length; i++) {
					modes[i] = PLOT_MODE.get(i).toString();
				}
				return modes;
			}
		};
		plotModeParam.setDisplayName("Plot Mode");
		registerConfigurableParameter(plotModeParam);

		// xaxis/yaxis data name parameter
		xaxisNameParam = new StringParameter(this, "X-Axis Data Name");
		xaxisNameParam.setDisplayName("X-Axis Data Name");
		registerConfigurableParameter(xaxisNameParam);
		yaxisNameParam = new StringParameter(this, "Y-Axis Data Name");
		yaxisNameParam.setDisplayName("Y-Axis Data Name");
		registerConfigurableParameter(yaxisNameParam);

		// ROI option parameter
		hasROIParam = new StringParameter(this,"Region Of Interest") {
			private static final long serialVersionUID = 2815254879307619914L;
			public String[] getChoices() {
				return HAS_ROI.toArray(new String[HAS_ROI.size()]);
			}
		};
		registerConfigurableParameter(hasROIParam);
		hasROIParam.setExpression(HAS_ROI.get(0));

		boxROITypeParam = new StringParameter(this,"Type of Rectangular ROI") {
			private static final long serialVersionUID = -380964888849739100L;
			public String[] getChoices() {
				return BOX_ROI_TYPE.toArray(new String[BOX_ROI_TYPE.size()]);
			}
		};
		registerConfigurableParameter(boxROITypeParam);
		boxROITypeParam.setExpression(BOX_ROI_TYPE.get(0));
		
	}

	private IROI myROI;

	@Override
	protected DataMessageComponent getTransformedMessage(final 
			List<DataMessageComponent> cache) throws ProcessingException {
		
		final String plotName = plotViewName.getExpression();
		final String dataName = dataNameParam.getExpression();
		final String plotMode = plotModeParam.getExpression();
		final String xaxisName = xaxisNameParam.getExpression();
		final String yaxisName = yaxisNameParam.getExpression();
		final String hasROISelection = hasROIParam.getExpression();
		final String boxTypeROI = boxROITypeParam.getExpression();
//		final List<IDataset>  data = MessageUtils.getDatasets(cache);
		final Map<String, Serializable>  data = MessageUtils.getList(cache);
		
		final DataMessageComponent mc = MessageUtils.copy(cache);

		try {
			Dataset aData = ((Dataset)data.get(dataName));
			//aData.setName(dataName);
			if(aData != null){
				if (plotMode.equals(PlottingMode.ONED.toString())) {
					if(xaxisName.equals("")) {
						SDAPlotter.plot(plotName, aData);
					} else {						
						Dataset xAxis = (Dataset)data.get(xaxisName);
						SDAPlotter.plot(plotName, xAxis, new IDataset[] {aData}, xAxis.getName(), aData.getName() );
					}
					
				} else if (plotMode.equals(PlottingMode.TWOD.toString())) {
					// if shape is like [1, 1000, 1000] replace by [1000, 1000]
					int[] shapes = aData.getShape();
					if (shapes.length > 2) {
						int[] newShapes = new int[2];
						int j = 0;
						for (int i = 0; i < shapes.length; i++) {
							if (shapes[i] != 1 && j < 2) {
								newShapes[j] = shapes[i];
								j++;
							}
						}
						if(newShapes.length == 2)
							aData.setShape(newShapes);
					}

					if(xaxisName.equals("")||(yaxisName.equals("")))
						SDAPlotter.imagePlot(plotName, aData);
					else{
						SDAPlotter.imagePlot(plotName, (Dataset)data.get(xaxisName), ((Dataset)data.get(yaxisName)), aData);
					}
				} else if (plotMode.equals(PlottingMode.SCATTER2D.toString())) {
//					if(xaxisName.equals("")||(yaxisName.equals("")))
//						SDAPlotter.(plotName, (Dataset)data.get(dataName));
//					else
//						SDAPlotter.imagePlot(plotName, ((Dataset)data.get(xaxisName)), ((Dataset)data.get(yaxisName)), ((Dataset)data.get(dataName)));

				} else if (plotMode.equals(PlottingMode.SCATTER3D.toString())) {
//					if(xaxisName.equals("")||(yaxisName.equals("")))
//						SDAPlotter.imagePlot(plotName, (Dataset)data.get(dataName));
//					else
//						SDAPlotter.imagePlot(plotName, ((Dataset)data.get(xaxisName)), ((Dataset)data.get(yaxisName)), ((Dataset)data.get(dataName)));

				} else if (plotMode.equals(PlottingMode.ONED_THREED.toString())) {
//					if(xaxisName.equals("")||(yaxisName.equals("")))
//						SDAPlotter.imagePlot(plotName, (Dataset)data.get(dataName));
//					else
//						SDAPlotter.imagePlot(plotName, ((Dataset)data.get(xaxisName)), ((Dataset)data.get(yaxisName)), ((Dataset)data.get(dataName)));

				} else if (plotMode.equals(PlottingMode.SURF2D.toString())) {
					if(xaxisName.equals("")||(yaxisName.equals("")))
						SDAPlotter.surfacePlot(plotName, aData);
					else
						SDAPlotter.surfacePlot(plotName, (Dataset)data.get(xaxisName), ((Dataset)data.get(yaxisName)), aData);

				} else if (plotMode.equals(PlottingMode.MULTI2D.toString())) {
//					if(xaxisName.equals("")||(yaxisName.equals("")))
//						SDAPlotter.imagePlot(plotName, (Dataset)data.get(dataName));
//					else
//						SDAPlotter.imagePlot(plotName, ((Dataset)data.get(xaxisName)), ((Dataset)data.get(yaxisName)), ((Dataset)data.get(dataName)));

				}
			}
			
		} catch (Exception e) {
			throw createDataMessageException("Displaying data sets", e);
		}

		if(hasROISelection.equals(HAS_ROI.get(0))){
			//create region
			Display.getDefault().asyncExec(new Runnable() {
				
				@Override
				public void run() {
					int[] maxPos = ((Dataset)data.get(dataName)).maxPos();
					double width = maxPos[0];
					double height = maxPos[1];
					myROI = new RectangularROI(width, height/2, 0);

					plottingSystem = PlottingFactory.getPlottingSystem(plotName);

					//Create Region(s)
					Map<String, IROI> rois = null;
					try {
						rois = MessageUtils.getROIs(cache);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					Set<Map.Entry<String, IROI>> roisSet = rois.entrySet();
					if(!roisSet.isEmpty()){
						Iterator<Entry<String, IROI>> it = roisSet.iterator();
						while(it.hasNext()){
							Entry<String,IROI> entry = it.next();
							String roiname = entry.getKey();
							myROI = entry.getValue();
							createRegion(plottingSystem, myROI, roiname, boxTypeROI);
							mc.addROI(roiname, myROI);
						}
					} else {
						createRegion(plottingSystem, myROI, "Default ROI", boxTypeROI);
						mc.addROI( "Default ROI", myROI);
					}
				}
			});
		}
		return mc;
	}

	private void createRegion(final IPlottingSystem<Composite> plottingSystem, final IROI roi, final String roiName, final String boxType){
		try {
			final IToolPageSystem system = (IToolPageSystem)plottingSystem.getAdapter(IToolPageSystem.class);
			if(roi instanceof LinearROI){
				LinearROI lroi = (LinearROI)roi;
				IRegion region = plottingSystem.getRegion(roiName);
				if(region!=null&&region.isVisible()){
					region.setROI(lroi);
				}else {
					IRegion newRegion = plottingSystem.createRegion(roiName, RegionType.LINE);
					newRegion.setROI(lroi);
					plottingSystem.addRegion(newRegion);
					system.setToolVisible("org.dawb.workbench.plotting.tools.boxProfileTool",
							ToolPageRole.ROLE_2D, "org.dawb.workbench.plotting.views.toolPageView.2D");
				}
			} else if(roi instanceof RectangularROI){
				RectangularROI rroi = (RectangularROI)roi;
				IRegion region = plottingSystem.getRegion(roiName);
				
				//Test if the region is already there and update the currentRegion
				if(region!=null&&region.isVisible()){
					region.setROI(rroi);
				}else {
					if(boxType.equals(BOX_ROI_TYPE.get(0))){
						IRegion newRegion = plottingSystem.createRegion(roiName, RegionType.BOX);
						newRegion.setROI(rroi);
						plottingSystem.addRegion(newRegion);
					}
					if(boxType.equals(BOX_ROI_TYPE.get(1))){
						IRegion newRegion = plottingSystem.createRegion(roiName, RegionType.XAXIS);
						newRegion.setROI(rroi);
						plottingSystem.addRegion(newRegion);
					}
					if(boxType.equals(BOX_ROI_TYPE.get(2))){
						IRegion newRegion = plottingSystem.createRegion(roiName, RegionType.YAXIS);
						newRegion.setROI(rroi);
						plottingSystem.addRegion(newRegion);
					}
					system.setToolVisible("org.dawb.workbench.plotting.tools.boxProfileTool",
							ToolPageRole.ROLE_2D, "org.dawb.workbench.plotting.views.toolPageView.2D");
				}
			} else if(roi instanceof SectorROI){
				SectorROI sroi = (SectorROI)roi;
				IRegion region = plottingSystem.getRegion(roiName);
				if(region!=null&&region.isVisible()){
					region.setROI(sroi);
				}else {
					IRegion newRegion = plottingSystem.createRegion(roiName, RegionType.SECTOR);
					newRegion.setROI(sroi);
					plottingSystem.addRegion(newRegion);
					system.setToolVisible("org.dawb.workbench.plotting.tools.boxProfileTool",
							ToolPageRole.ROLE_2D, "org.dawb.workbench.plotting.views.toolPageView.2D");
				}
			} else if(roi instanceof EllipticalROI){
				EllipticalROI eroi = (EllipticalROI)roi;
				IRegion region = plottingSystem.getRegion(roiName);
				if(region!=null&&region.isVisible()){
					region.setROI(eroi);
				}else {
					IRegion newRegion = plottingSystem.createRegion(roiName, RegionType.ELLIPSE);
					newRegion.setROI(eroi);
					plottingSystem.addRegion(newRegion);
					system.setToolVisible("org.dawb.workbench.plotting.tools.boxProfileTool",
							ToolPageRole.ROLE_2D, "org.dawb.workbench.plotting.views.toolPageView.2D");
				}
			} else if(roi instanceof EllipticalFitROI){
				EllipticalFitROI efroi = (EllipticalFitROI)roi;
				IRegion region = plottingSystem.getRegion(roiName);
				if(region!=null&&region.isVisible()){
					region.setROI(efroi);
				}else {
					IRegion newRegion = plottingSystem.createRegion(roiName, RegionType.ELLIPSEFIT);
					newRegion.setROI(efroi);
					plottingSystem.addRegion(newRegion);
					system.setToolVisible("org.dawb.workbench.plotting.tools.boxProfileTool",
							ToolPageRole.ROLE_2D, "org.dawb.workbench.plotting.views.toolPageView.2D");
				}
			} else if(roi instanceof PointROI){
				PointROI proi = (PointROI)roi;
				IRegion region = plottingSystem.getRegion(roiName);
				if(region!=null&&region.isVisible()){
					region.setROI(proi);
				}else {
					IRegion newRegion = plottingSystem.createRegion(roiName, RegionType.POINT);
					newRegion.setROI(proi);
					plottingSystem.addRegion(newRegion);
					system.setToolVisible("org.dawb.workbench.plotting.tools.boxProfileTool",
							ToolPageRole.ROLE_2D, "org.dawb.workbench.plotting.views.toolPageView.2D");
				}
			}
			
		} catch (Exception e) {
			logger.error("Couldn't open histogram view and create ROI", e);
		}
	}

	@Override
	protected String getOperationName() {
		return "Image Plot Actor";
	}

}
