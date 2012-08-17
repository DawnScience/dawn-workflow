package org.dawb.passerelle.common.parameter.roi;

import org.dawb.common.ui.plot.roi.ROIEditTable;
import org.dawb.common.ui.plot.roi.ROIType;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.kernel.util.NamedObj;
import uk.ac.diamond.scisoft.analysis.roi.ROIBase;
import uk.ac.gda.common.rcp.util.DialogUtils;

/**
 * A dialog for editing a ROI. Uses ROIViewer table.
 * @author fcp94556
 *
 */
public class ROIDialog extends Dialog {

	private static final Logger logger = LoggerFactory.getLogger(ROIDialog.class);
	
	private ROIEditTable roiEditor;
	private CCombo roiType;
	
	ROIDialog(Shell parentShell, NamedObj container) {	
		super(parentShell);
	}
	
	protected boolean isResizable() {
		return true;
	}
	
	public Control createDialogArea(Composite parent) {
		
		final Composite main = (Composite)super.createDialogArea(parent);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		final Composite top= new Composite(main, SWT.NONE);
		top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		top.setLayout(new GridLayout(2, false));
		
		final Label label = new Label(top, SWT.NONE);
		label.setText("Region type    ");
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));	
		
		roiType = new CCombo(top, SWT.READ_ONLY|SWT.BORDER);
		roiType.setItems(ROIType.getTypes());
		roiType.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		roiType.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try {
					roiEditor.setRegion(ROIType.createNew(roiType.getSelectionIndex()), null);
				} catch (Exception e1) {
					logger.error("Cannot create roi "+ROIType.getType(roiType.getSelectionIndex()).getName(), e1);
				}
			}
		});

		
		this.roiEditor = new ROIEditTable();
		roiEditor.createPartControl(main);
		
		return main;
	}
	
	void setROI(ROIBase roi) {
		final int index = ROIType.getIndex(roi.getClass());
		roiType.select(index);
		roiEditor.setRegion(roi, null);
	}
	
	ROIBase getROI() {
		return roiEditor.getRoi();
	}
}
