package org.dawb.passerelle.actors.data.config;

import org.dawb.passerelle.common.parameter.JSONCellEditorParameter;
import org.dawb.passerelle.common.parameter.Marshaller;
import org.dawnsci.processing.ui.model.OperationModelDialog;
import org.eclipse.dawnsci.analysis.api.processing.model.IOperationModel;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;

/**
 * 
 * Parameter to allow one to edit operation models.
 * 
 * @author fcp94556
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
				
				final OperationModelDialog dialog = new OperationModelDialog(cellEditorWindow.getShell());
				dialog.create();
				dialog.getShell().setSize(600,450); // As needed
				dialog.getShell().setText("Edit Model");
				
				IOperationModelInstanceProvider prov = (IOperationModelInstanceProvider)getContainer();
				try {
					dialog.setModel(OperationModelParameter.this.getValue(prov.getModelClass()));
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
				
		        final int ok = dialog.open();
		        if (ok == Dialog.OK) {
		            Object model = getValueFromBean(dialog.getModel());
		            return model;
		        }
		        
		        return null;

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
