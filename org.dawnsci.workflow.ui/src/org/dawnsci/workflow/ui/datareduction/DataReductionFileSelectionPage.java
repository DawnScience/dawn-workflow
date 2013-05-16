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

import java.util.ArrayList;
import java.util.List;

import org.dawb.common.ui.plot.AbstractPlottingSystem;
import org.dawb.common.ui.plot.PlottingFactory;
import org.dawb.common.ui.util.GridUtils;
import org.dawnsci.plotting.api.PlotType;
import org.dawnsci.workflow.ui.Activator;
import org.dawnsci.workflow.ui.views.runner.AbstractWorkflowRunPage;
import org.dawnsci.workflow.ui.views.runner.IWorkflowContext;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
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

import uk.ac.diamond.scisoft.analysis.dataset.IDataset;
import uk.ac.diamond.scisoft.analysis.rcp.plotting.datareduction.DataReductionPlotter;

public class DataReductionFileSelectionPage extends AbstractWorkflowRunPage {

	private static final Logger logger = LoggerFactory.getLogger(DataReductionFileSelectionPage.class);

	private static final String DATA_TYPE = "Data";
	private static final String CALIB_TYPE = "Calibration";
	private static final String DETECT_TYPE = "Detector";
	private static final String BACKGD_TYPE = "Background";
	private static final String MASK_TYPE = "Mask";

	private AbstractPlottingSystem dataPlot;
	private AbstractPlottingSystem calibrationPlot;
	private AbstractPlottingSystem detectorPlot;
	private AbstractPlottingSystem maskPlot;
	private AbstractPlottingSystem backgroundPlot;
	private Composite mainComposite;
	private Composite recapComp;

	private Button runWorkflowButton;

	private IDataset image;
	private TableViewer viewer;
	private List<SelectedData> rows;

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
		SashForm middleSash = new SashForm(mainSash, SWT.VERTICAL);
		middleSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		SashForm rightSash = new SashForm(mainSash, SWT.VERTICAL);
		rightSash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// Composite to put the main controls in
		Composite mainRecapComp = new Composite(leftSash, SWT.BORDER);
		mainRecapComp.setLayout(new GridLayout(1, false));
		mainRecapComp.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, false));

		Label helpLabel = new Label(mainRecapComp, SWT.WRAP);
		helpLabel.setText("Select a file in the Project Explorer and lock the type. " +
				"Make sure that all your selected data has the same shape.");
		helpLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));

		recapComp = new Composite(mainRecapComp, SWT.NONE);
		recapComp.setLayout(new GridLayout(1, false));
		recapComp.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, true, true));

		viewer = new TableViewer(recapComp, SWT.FULL_SELECTION | SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		createColumns(viewer);
		viewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		viewer.getTable().setLinesVisible(true);
		viewer.getTable().setHeaderVisible(true);
		workflowRunView.getSite().setSelectionProvider(viewer);
		rows = createSelectedDataRows();
		viewer.setContentProvider(new IStructuredContentProvider() {
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				// TODO Auto-generated method stub
			}
			@Override
			public void dispose() {
				// TODO Auto-generated method stub
			}
			@Override
			public Object[] getElements(Object inputElement) {
				return rows.toArray(new SelectedData[rows.size()]);
			}
		});
		viewer.setInput(rows.get(0));

		runWorkflowButton = new Button(mainRecapComp, SWT.PUSH);
		runWorkflowButton.setText("Run Workflow");
		runWorkflowButton.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		runWorkflowButton.setEnabled(isRunWorkflowEnabled());
		runWorkflowButton.addSelectionListener(runWorkflowListener);

		detectorPlot.createPlotPart(leftSash, DETECT_TYPE, null, PlotType.IMAGE, workflowRunView.getSite().getPart());
		detectorPlot.setTitle(DETECT_TYPE);

		dataPlot.createPlotPart(middleSash, DATA_TYPE, null, PlotType.IMAGE, workflowRunView.getSite().getPart());
		dataPlot.setTitle(DATA_TYPE);

		backgroundPlot.createPlotPart(middleSash, BACKGD_TYPE, null, PlotType.IMAGE, workflowRunView.getSite().getPart());
		backgroundPlot.setTitle(BACKGD_TYPE);

		calibrationPlot.createPlotPart(rightSash, CALIB_TYPE, null, PlotType.IMAGE, workflowRunView.getSite().getPart());
		calibrationPlot.setTitle(CALIB_TYPE);

		maskPlot.createPlotPart(rightSash, MASK_TYPE, null, PlotType.IMAGE, workflowRunView.getSite().getPart());
		maskPlot.setTitle(MASK_TYPE);

		workflowRunView.getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(fileSelectionListener);

		return mainComposite;
	}

	protected void createColumns(final TableViewer viewer) {
		
		ColumnViewerToolTipSupport.enableFor(viewer,ToolTip.NO_RECREATE);

		TableViewerColumn var = new TableViewerColumn(viewer, SWT.LEFT, 0);
		var.getColumn().setText("Lock");
		var.getColumn().setWidth(50);
		var.setLabelProvider(new SelectedDataLabelProvider(0));
		SelectedDataEditingSupport regionEditor = new SelectedDataEditingSupport(viewer, 0);
		var.setEditingSupport(regionEditor);

		var = new TableViewerColumn(viewer, SWT.LEFT, 1);
		var.getColumn().setText("Type");
		var.getColumn().setWidth(100);
		var.setLabelProvider(new SelectedDataLabelProvider(1));
		regionEditor = new SelectedDataEditingSupport(viewer, 1);
		var.setEditingSupport(regionEditor);

		var = new TableViewerColumn(viewer, SWT.LEFT, 2);
		var.getColumn().setText("File name");
		var.getColumn().setWidth(200);
		var.setLabelProvider(new SelectedDataLabelProvider(2));
		regionEditor = new SelectedDataEditingSupport(viewer, 2);
		var.setEditingSupport(regionEditor);

		var = new TableViewerColumn(viewer, SWT.CENTER, 3);
		var.getColumn().setText("Shape");
		var.getColumn().setWidth(80);
		var.setLabelProvider(new SelectedDataLabelProvider(3));
		regionEditor = new SelectedDataEditingSupport(viewer, 3);
		var.setEditingSupport(regionEditor);

	}

	private ISelectionListener fileSelectionListener = new ISelectionListener() {

		@Override
		public void selectionChanged(IWorkbenchPart part, ISelection selection) {
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection structSelection = (IStructuredSelection)selection;
				image = DataReductionPlotter.loadData(structSelection);
				if(image == null) return;
				if (!((SelectedData)viewer.getElementAt(0)).isLocked()) {
					DataReductionPlotter.plotData(dataPlot, image);
					((SelectedData)viewer.getElementAt(0)).setShape(image.getShape());
					((SelectedData)viewer.getElementAt(0)).setFileName(DataReductionPlotter.getFileName(structSelection));
				}
				if (!((SelectedData)viewer.getElementAt(1)).isLocked()) {
					DataReductionPlotter.plotData(calibrationPlot, image);
					((SelectedData)viewer.getElementAt(1)).setShape(image.getShape());
					((SelectedData)viewer.getElementAt(1)).setFileName(DataReductionPlotter.getFileName(structSelection));
				}
				if (!((SelectedData)viewer.getElementAt(2)).isLocked()) {
					DataReductionPlotter.plotData(detectorPlot, image);
					((SelectedData)viewer.getElementAt(2)).setShape(image.getShape());
					((SelectedData)viewer.getElementAt(2)).setFileName(DataReductionPlotter.getFileName(structSelection));
				}
				if (!((SelectedData)viewer.getElementAt(3)).isLocked()) {
					DataReductionPlotter.plotData(backgroundPlot, image);
					((SelectedData)viewer.getElementAt(3)).setShape(image.getShape());
					((SelectedData)viewer.getElementAt(3)).setFileName(DataReductionPlotter.getFileName(structSelection));
				}
				if (!((SelectedData)viewer.getElementAt(4)).isLocked()) {
					DataReductionPlotter.plotData(maskPlot, image);
					((SelectedData)viewer.getElementAt(4)).setShape(image.getShape());
					((SelectedData)viewer.getElementAt(4)).setFileName(DataReductionPlotter.getFileName(structSelection));
				}
				viewer.refresh();
			}
		}
	};

	private List<SelectedData> createSelectedDataRows(){
		final List<SelectedData> selectedDataList = new ArrayList<SelectedData>(5);
		SelectedData dataSelectedData = new SelectedData(DATA_TYPE, new int[]{0, 0}, "-", false);
		SelectedData calibrationSelectedData = new SelectedData(CALIB_TYPE, new int[]{0, 0}, "-", false);
		SelectedData detectorSelectedData = new SelectedData(DETECT_TYPE, new int[]{0, 0}, "-", false);
		SelectedData backgroundSelectedData = new SelectedData(BACKGD_TYPE, new int[]{0, 0}, "-", false);
		SelectedData maskSelectedData = new SelectedData(MASK_TYPE, new int[]{0, 0}, "-", false);
		selectedDataList.add(dataSelectedData);
		selectedDataList.add(calibrationSelectedData);
		selectedDataList.add(detectorSelectedData);
		selectedDataList.add(backgroundSelectedData);
		selectedDataList.add(maskSelectedData);
		return selectedDataList;
	}

	private String formatIntArray(int[] array){
		String str = "";
		for (int i = 0; i < array.length; i++) {
			str += String.valueOf(array[i]);
			if(i != array.length-1)
				str += ", ";
		}
		return str;
	}

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
		workflowRunView.getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(fileSelectionListener);
	}

	private boolean isRunWorkflowEnabled(){
		return true;
	}

	/**
	 * Data bean to fill the Table viewer
	 *
	 */
	private class SelectedData {

		private int[] shape;
		private boolean isLocked;
		private String fileName;
		private String type;

		public SelectedData(String type, int[] shape, String fileName, boolean isLocked) {
			this.type = type;
			this.shape = shape;
			this.fileName = fileName;
			this.isLocked = isLocked;
		}

		public void setShape(int[] shape){
			this.shape = shape;
		}

		public int[] getShape(){
			return shape;
		}

		public boolean isLocked() {
			return isLocked;
		}

		public void setLocked(boolean isLocked) {
			this.isLocked = isLocked;
		}

		public String getFileName() {
			return fileName;
		}

		public void setFileName(String fileName) {
			this.fileName = fileName;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}
	}

	/**
	 * EditingSupport Class
	 *
	 */
	private class SelectedDataEditingSupport extends EditingSupport {

		private int column;

		public SelectedDataEditingSupport(ColumnViewer viewer, int col) {
			super(viewer);
			this.column = col;
		}
		@Override
		protected CellEditor getCellEditor(final Object element) {
			CellEditor ed = null;
			
			if(column == 0){
				ed = new CheckboxCellEditor(((TableViewer)getViewer()).getTable(), SWT.RIGHT);
				return ed;
			} else if(column > 0){
				ed = new TextCellEditor(((TableViewer)getViewer()).getTable(), SWT.RIGHT);
				return ed;
			} 
			return null;
			
		}

		@Override
		protected boolean canEdit(Object element) {
			if (column == 0) return true;
			else return false;
		}

		@Override
		protected Object getValue(Object element) {
			final SelectedData myImage = (SelectedData)element;
			switch (column){
			case 0:
				return myImage.isLocked();
			case 1:
				return myImage.getType();
			case 2:
				return myImage.getFileName();
			case 3:
				return formatIntArray(myImage.getShape());
			default:
				return null;
			}
			
		}

		@Override
		protected void setValue(Object element, Object value) {
			try {
				this.setValue(element, value, true);
			} catch (Exception e) {
				logger.debug("Error while setting table value");
				e.printStackTrace();
			}
		}
		
		private void setValue(Object element, Object value, boolean tableRefresh) throws Exception {

			final SelectedData myImage = (SelectedData) element;
			switch (column){
			case 0:
				myImage.setLocked((Boolean)value);
				break;
			case 1:
				myImage.setType((String)value);
				break;
			case 2:
				myImage.setFileName((String)value);
				break;
			case 3:
				myImage.setShape((int[])value);
				break;
			default:
				break;
			}

			if (tableRefresh) {
				getViewer().refresh();
			}
		}
	}

	/**
	 * Table viewer Label provider
	 *
	 */
	private class SelectedDataLabelProvider extends ColumnLabelProvider {
		
		private int column;
		private Image lockedIcon;
		private Image unlockedIcon;

		public SelectedDataLabelProvider(int column) {
			this.column = column;
			ImageDescriptor id = Activator.getImageDescriptor("icons/lock_small.png");
			lockedIcon   = id.createImage();
			id = Activator.getImageDescriptor("icons/lock_open_small.png");
			unlockedIcon =  id.createImage();
		}

		@Override
		public Image getImage(Object element){
			
			if (!(element instanceof SelectedData)) return null;
			if (column==0){
				final SelectedData selectedData = (SelectedData)element;
				return selectedData.isLocked() ? lockedIcon : unlockedIcon;
			}
			return null;
		}

		@Override
		public String getText(Object element) {
			
			if (!(element instanceof SelectedData)) return null;
			final SelectedData    selectedData = (SelectedData)element;

			switch (column) {
			case 1:
				return selectedData.getType();
			case 2:
				return selectedData.getFileName();
			case 3:
				return formatIntArray(selectedData.getShape());
			default:
				return "";
			}
		}

		public String getToolTipText(Object element) {
			return "Click on the lock icon to select the data you want the workflow to be processed with.";
		}

		@Override
		public void dispose(){
			super.dispose();
			lockedIcon.dispose();
			unlockedIcon.dispose();
		}
	}

}
