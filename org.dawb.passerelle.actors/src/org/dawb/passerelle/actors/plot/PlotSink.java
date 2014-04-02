/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.actors.plot;

import java.util.List;

import org.dawb.passerelle.common.actors.AbstractDataMessageSink;
import org.dawb.passerelle.common.message.MessageUtils;

import ptolemy.data.BooleanToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.IDataset;
import uk.ac.diamond.scisoft.analysis.message.DataMessageComponent;

import com.isencia.passerelle.actor.ProcessingException;

/**
 * Attempts to plot data in eclipse or writes a csv file with the data if 
 * that is not possible.
 * 
 * @author gerring
 *
 */
public class PlotSink extends AbstractDataMessageSink {
	
	private boolean   update;
	private String    plotPartName,title;
	private Parameter updateParam,plotName,plotTitle;

	public PlotSink(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		super(container, name);
		
		updateParam = new Parameter(this, "Update Graph", new BooleanToken(false));
		updateParam.setTypeEquals(BaseType.BOOLEAN);
		registerConfigurableParameter(updateParam);
		
		plotName = new StringParameter(this, "Plot Name");
		plotName.setExpression("Plot 1");
		registerConfigurableParameter(plotName);
		plotPartName = plotName.getExpression();
		
		plotTitle = new StringParameter(this, "Plot Title");
		registerConfigurableParameter(plotTitle);
	}
	
	public void attributeChanged(Attribute attribute) throws IllegalActionException {

		if (attribute == updateParam) {
			setUpdate(((BooleanToken) updateParam.getToken()).booleanValue());
		} else if (attribute == plotName) {
			setPlotPartName(plotName.getExpression());
		} else if (attribute == plotTitle) {
			setTitle(plotTitle.getExpression());
		} else
			super.attributeChanged(attribute);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -8241926002035209866L;

	@Override
	protected void sendCachedData(final List<DataMessageComponent> data) throws ProcessingException {
		
		try {			
			IDataset   xAxis = getXAxis(data);
			IDataset[] yAxes = getYAxes(data);
			
			if (yAxes!=null || xAxis.getShape().length==1) {
				if (yAxes==null) {
					yAxes = new IDataset[]{xAxis};
					xAxis = AbstractDataset.arange(xAxis.getSize(), AbstractDataset.INT32);
				}
				if (update) {
					//SDAPlotter.updatePlot(plotPartName, xAxis, yAxes);
				} else {
				    //SDAPlotter.plot(plotPartName, title, xAxis, yAxes);
				}				
			} else {
				//SDAPlotter.imagePlot(plotPartName, xAxis);
			}
			
		} catch (Exception e) {
			if (data!=null) {
				// TODO CSV
				//CSVUtils.createCSV(dataFile, data, conjunctive)
				throw createDataMessageException("Cannot process plot, writing as a CSV instead", e);
			} else {
				throw createDataMessageException("Cannot process plot!", e);

			}
		}
		
	}

	private IDataset[] getYAxes(final List<DataMessageComponent> data) {
		
		final List<IDataset> sets = MessageUtils.getDatasets(data);
		if (sets.size()<2) return null;
		
		sets.remove(0);
		return sets.toArray(new IDataset[sets.size()]);
	}
	
	private IDataset getXAxis(final List<DataMessageComponent> data) {
		final List<IDataset> sets = MessageUtils.getDatasets(data);
		return sets.get(0);
	}
	
	public boolean isUpdate() {
		return update;
	}
	public void setUpdate(boolean update) {
		this.update = update;
	}

	public String getPlotPartName() {
		return plotPartName;
	}

	public void setPlotPartName(String plotPartName) {
		this.plotPartName = plotPartName;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

}
