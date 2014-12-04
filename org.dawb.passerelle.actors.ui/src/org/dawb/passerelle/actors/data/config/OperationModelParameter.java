package org.dawb.passerelle.actors.data.config;

import org.dawb.passerelle.common.parameter.JSONCellEditorParameter;
import org.dawb.passerelle.common.parameter.Marshaller;
import org.eclipse.dawnsci.analysis.api.processing.model.IOperationModel;
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
				
				//final OperationModelDialog dialog = new OperationModelDialog(cellEditorWindow.getShell());
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
		return getExpression();
	}


}
