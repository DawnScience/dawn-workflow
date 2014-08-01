package org.dawb.passerelle.common.parameter.roi;

import java.io.IOException;

import org.dawb.common.services.IPersistenceService;
import org.dawb.common.services.ServiceManager;
import org.dawnsci.plotting.roi.IRegionTransformer;
import org.dawnsci.plotting.roi.RegionCellEditor;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import uk.ac.diamond.scisoft.analysis.roi.IROI;
import uk.ac.diamond.scisoft.analysis.roi.RectangularROI;

import com.isencia.passerelle.workbench.model.editor.ui.properties.CellEditorAttribute;

/**
 * This parameter can be used to edit a ROI in the workflow.
 * 
 * It pops up a dialog for the ROI values.
 * 
 * @author fcp94556
 *
 */
public class ROIParameter extends StringParameter  implements CellEditorAttribute, IRegionTransformer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5160321810304396960L;
	
	private static final Logger logger = LoggerFactory.getLogger(ROIParameter.class);

	public ROIParameter(NamedObj container, String name) throws IllegalActionException, NameDuplicationException {
		super(container, name);
	}


	@Override
	public CellEditor createCellEditor(Control control) {
		
		return new RegionCellEditor((Composite)control, this);
	}

	@Override
	public String getRendererText() {
		if (getExpression()==null || "".equals(getExpression())) return "Click to define roi...";
		try {
			final IROI roi = getROIFromValue();
			return roi.getClass().getSimpleName()+"  "+roi.toString();
		} catch (Throwable e) {
			return "Click to define roi...";
		} 
	}

	/**
	 * Decode the roi from a string.
	 * @return
	 */
	private IROI getROIFromValue() throws Exception {
		
		if (getExpression()==null || "".equals(getExpression())) return new RectangularROI();

		return getROIFromValue(getExpression());
	}

	private IROI getROIFromValue(String expression) throws Exception {
		IPersistenceService service = (IPersistenceService)ServiceManager.getService(IPersistenceService.class);
		try {
			return (IROI)service.unmarshal(expression);
		} catch (Exception e) {
			// if not a JSon value try to retrieve the value from a base64 encoded value
			String exception = e.getClass().toString();
			if (exception.endsWith("JsonParseException")) {
				logger.error("Error reading value from ROI, replace the Region actor by a new one: "+ e);
				return null;
			} else {
				logger.error("Error reading value from ROI: "+ e);
				return null;
			}
		}
	}

	public IROI getRoi() {
		if (getExpression()==null || "".equals(getExpression())) return null;
		try {
			return getROIFromValue(getExpression());
		} catch (Exception ne) {
			return null;
		}
	}

	/**
	 * Encode the roi as a string, base64 encode probably
	 * @param roi
	 * @return
	 */
	private String getValueFromROI(final IROI roi) throws IOException {
		if (roi==null) return "";
		try {
			IPersistenceService service = (IPersistenceService)ServiceManager.getService(IPersistenceService.class);
			return service.marshal(roi);
		} catch (Exception e) {
			logger.error("Error marshalling IROI to JSON:"+ e);
			return "";
		}
	}

	public void setRoi(IROI roi) {
		try {
			setExpression(getValueFromROI(roi));
		} catch (IOException e) {
			setExpression("");
		}
	}


	@Override
	public IROI getROI() throws Exception {
		return getROIFromValue();
	}


	@Override
	public Object getValue(IROI value) throws IOException {
		return getValueFromROI(value);
	}

}