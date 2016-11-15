package org.dawb.passerelle.actors.data.config;

import java.util.HashMap;
import java.util.Map;

import org.dawb.passerelle.common.parameter.JSONCellEditorParameter;
import org.dawb.passerelle.common.parameter.Marshaller;
import org.dawnsci.processing.ui.ServiceHolder;
import org.dawnsci.processing.ui.api.IOperationModelWizard;
import org.dawnsci.processing.ui.api.IOperationSetupWizardPage;
import org.dawnsci.processing.ui.model.OperationModelWizard;
import org.dawnsci.processing.ui.model.OperationModelWizardDialog;
import org.eclipse.dawnsci.analysis.api.processing.IOperationInputData;
import org.eclipse.dawnsci.analysis.api.processing.model.IOperationModel;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;

/**
 * 
 * Parameter to allow one to edit operation models.
 * 
 * @author Matthew Gerring
 *
 */
public class OperationModelParameter extends JSONCellEditorParameter<IOperationModel> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7973600673600983836L;

	/**
	 * 
	 * @param container
	 * @param name
	 * @throws IllegalActionException
	 * @throws NameDuplicationException
	 */
	public OperationModelParameter(NamedObj container, String name) throws IllegalActionException, NameDuplicationException {
		super(container, name);
	}
	
	/**
	 * Marshaling models.
	 */
	protected Marshaller<IOperationModel> createMarshaller() {
        return new OperationModelMarshaller();
	}

	/**
	 * Model viewer
	 */
	@Override
	public CellEditor createCellEditor(Control control) {
		
		final DialogCellEditor editor = new DialogCellEditor((Composite)control) {
			@Override
			protected Object openDialogBox(Control cellEditorWindow) {
				MessageDialog.openInformation(cellEditorWindow.getShell(), "Dialog Information", "The OperationWizard Dialog has not been implemented yet.");
				return null;
				//TODO
//				IOperationModelInstanceProvider prov = (IOperationModelInstanceProvider)getContainer();
//				IOperationModel modelEditor = OperationModelParameter.this.getValue(prov.getModelClass());
//				
//				IOperationSetupWizardPage wizardPage = ServiceHolder.getOperationUIService().getWizardPage(null);
//
//				OperationModelWizardDialog dialog = new OperationModelWizardDialog(cellEditorWindow.getShell(), wizardPage);
//				dialog.create();
//				if (dialog.open() == Dialog.OK) {
//					EventAdmin eventAdmin = ServiceHolder.getEventAdmin();
//					Map<String,IOperationInputData> props = new HashMap<>();
//					eventAdmin.postEvent(new Event("org/dawnsci/events/processing/PROCESSUPDATE", props));
//		            return getValueFromBean(modelEditor);
//				}
//				
//		        return null;

			}
		};
		return editor;
	}
	

	/**
	 * Text representation of model. Model.toString()?
	 */
	@Override
	public String getRendererText() {
		try {
			IOperationModelInstanceProvider prov = (IOperationModelInstanceProvider)getContainer();
			IOperationModel model = OperationModelParameter.this.getValue(prov.getModelClass());
			String json = mapper.marshal(model);
			String[] sa = json.split("_____");
			return sa[1];
		} catch (Exception ne) {
		    return getExpression();
		}
	}


}
