package org.dawb.passerelle.common.parameter.roi;

import org.dawb.common.ui.plot.roi.ROIEditTable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import ptolemy.kernel.util.NamedObj;
import uk.ac.diamond.scisoft.analysis.roi.ROIBase;

/**
 * A dialog for editing a ROI. Uses ROIViewer table.
 * @author fcp94556
 *
 */
public class ROIDialog extends Dialog {

	private ROIEditTable roiEditor;
	
	ROIDialog(Shell parentShell, NamedObj container) {	
		super(parentShell);
	}
	
	public Control createDialogArea(Composite parent) {
		
		final Composite main = (Composite)super.createDialogArea(parent);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		this.roiEditor = new ROIEditTable();
		roiEditor.createPartControl(main);
		
		return main;
	}
	
	void setROI(ROIBase roi) {
		roiEditor.setRegion(roi, null);
	}
	
	ROIBase getROI() {
		return roiEditor.getRoi();
	}

}
