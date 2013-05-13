/*-
 * Copyright 2013 Diamond Light Source Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dawnsci.workflow.ui.datareduction;

import org.dawb.common.ui.plot.AbstractPlottingSystem;
import org.dawb.common.ui.plot.PlottingFactory;
import org.dawb.common.ui.util.GridUtils;
import org.dawnsci.plotting.api.PlotType;
import org.dawnsci.workflow.ui.views.runner.AbstractWorkflowRunPage;
import org.dawnsci.workflow.ui.views.runner.IWorkflowContext;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISourceProvider;
import org.eclipse.ui.IWorkbenchPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.rcp.plotting.datareduction.DataReductionFilePlotter;
import uk.ac.diamond.scisoft.analysis.rcp.plotting.datareduction.DataReductionSelectionType;
import uk.ac.diamond.scisoft.analysis.rcp.plotting.datareduction.DataReductionSelectionType.DATA_TYPE;

public class DataReductionFileSelectionPage extends AbstractWorkflowRunPage {
	
	private static final Logger logger = LoggerFactory.getLogger(DataReductionFileSelectionPage.class);
	private AbstractPlottingSystem dataPlot;
	private AbstractPlottingSystem calibrationPlot;
	private AbstractPlottingSystem detectorPlot;
	private AbstractPlottingSystem maskPlot;
	private AbstractPlottingSystem backgroundPlot;
	private Button dataButton;
	private Button calibrationButton;
	private Button detectorButton;
	private Button backgroundButton;
	private Button maskButton;
	private Composite mainComposite;

	public DataReductionFileSelectionPage() {
		try {
			dataPlot = PlottingFactory.createPlottingSystem();
			calibrationPlot = PlottingFactory.createPlottingSystem();
			detectorPlot = PlottingFactory.createPlottingSystem();
			backgroundPlot = PlottingFactory.createPlottingSystem();
			maskPlot = PlottingFactory.createPlottingSystem();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error("TODO put description of error here", e);
		}
		
	}

	@Override
	public Composite createPartControl(Composite parent) {
		mainComposite = new Composite(parent, SWT.NONE);
		mainComposite.setLayout(new GridLayout(1, true));
		GridUtils.removeMargins(mainComposite);

		SashForm mainSash = new SashForm(mainComposite, SWT.HORIZONTAL);
		mainSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		SashForm leftSash = new SashForm(mainSash, SWT.VERTICAL);
		leftSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//		leftSash.setBackground(new Color(parent.getDisplay(), 192, 192, 192));
		SashForm middleSash = new SashForm(mainSash, SWT.VERTICAL);
		middleSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//		middleSash.setBackground(new Color(parent.getDisplay(), 192, 192, 192));
		SashForm rightSash = new SashForm(mainSash, SWT.VERTICAL);
		rightSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//		rightSash.setBackground(new Color(parent.getDisplay(), 192, 192, 192));
		//mainSash.setWeights(new int[] {200, 183, 195});

		// Composite to put the main controls in
		Composite selectionComp = new Composite(leftSash, SWT.BORDER);
		selectionComp.setLayout(new GridLayout(1, false));
		selectionComp.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));
		
		Label helpLabel = new Label(selectionComp, SWT.WRAP);
		helpLabel.setText("Select a file and tick the corresponding checkbox. Once all your data is chosen, press the Run button.");
		helpLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, true));

		dataButton = new Button(selectionComp, SWT.CHECK);
		dataButton.setText("Data");
		calibrationButton = new Button(selectionComp, SWT.CHECK);
		calibrationButton.setText("Calibration");
		detectorButton = new Button(selectionComp, SWT.CHECK);
		detectorButton.setText("Detector");
		backgroundButton = new Button(selectionComp, SWT.CHECK);
		backgroundButton.setText("Background");
		maskButton = new Button(selectionComp, SWT.CHECK);
		maskButton.setText("Mask");

		Button runWorkflowButton = new Button(selectionComp, SWT.PUSH);
		runWorkflowButton.setText("Run Workflow");
		runWorkflowButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		runWorkflowButton.addSelectionListener(runWorkflowListener);
		
		detectorPlot.createPlotPart(leftSash, "Detector", null, PlotType.IMAGE, workflowRunView.getSite().getPart());
		detectorPlot.setTitle("Detector");

		dataPlot.createPlotPart(middleSash, "Data", null, PlotType.IMAGE, workflowRunView.getSite().getPart());
		dataPlot.setTitle("Data");

		backgroundPlot.createPlotPart(middleSash, "Background", null, PlotType.IMAGE, workflowRunView.getSite().getPart());
		backgroundPlot.setTitle("Background");

		calibrationPlot.createPlotPart(rightSash, "Calibration", null, PlotType.IMAGE, workflowRunView.getSite().getPart());
		calibrationPlot.setTitle("Calibration");

		maskPlot.createPlotPart(rightSash, "Mask", null, PlotType.IMAGE, workflowRunView.getSite().getPart());
		maskPlot.setTitle("Mask");


		workflowRunView.getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(fileSelectionListener);

		return mainComposite;
	}

	private ISelectionListener fileSelectionListener = new ISelectionListener() {
		@Override
		public void selectionChanged(IWorkbenchPart part, ISelection selection) {
			if (selection instanceof IStructuredSelection) {
				Object file = ((IStructuredSelection) selection).getFirstElement();
				if (file instanceof IFile) {
					String fileExtension = ((IFile) file).getFileExtension();
					String filename = ((IFile) file).getRawLocation().toOSString();
					DataReductionFilePlotter.plotData(dataPlot, filename, fileExtension, 
							"/entry1/instrument/analyser/data", 
							new String[] {
								"/entry1/instrument/analyser/energies",
								"/entry1/instrument/analyser/angles"
							}, 
							false, false, DATA_TYPE.DATA);
				}
			}
		}
	};

	private SelectionListener runWorkflowListener = new SelectionListener() {
		
		@Override
		public void widgetSelected(SelectionEvent e) {
			widgetDefaultSelected(e);
		}
		
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
			// TODO Auto-generated method stub
			
		}
	};

	@Override
	public String getTitle() {
		return "Data Reduction";
	}

	@Override
	public void run(IWorkflowContext context) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ISourceProvider[] getSourceProviders() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	
}
