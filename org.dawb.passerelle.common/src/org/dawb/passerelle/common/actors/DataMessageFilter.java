package org.dawb.passerelle.common.actors;

import java.util.List;
import java.util.Map;

import org.dawb.common.util.list.ListUtils;
import org.dawb.passerelle.common.message.MessageUtils;
import org.eclipse.dawnsci.analysis.api.message.DataMessageComponent;

import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Settable;

import com.isencia.passerelle.actor.ProcessingException;

/**
 * Actor to filter data message variables. For instance after a 
 * peak fit, only the peaks may be required.
 * @author Matthew Gerring
 *
 */
public class DataMessageFilter extends AbstractDataMessageTransformer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 74221356367747024L;
	
	private StringParameter dataFilter;
	private StringParameter roiFilter;
	private StringParameter functionFilter;
	
	public DataMessageFilter(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		super(container, name);
		
		dataFilter = new StringParameter(this, "Data Filter");
		registerConfigurableParameter(dataFilter);

		roiFilter = new StringParameter(this, "Region Filter");
		registerConfigurableParameter(roiFilter);

		functionFilter = new StringParameter(this, "Function Filter");
		registerConfigurableParameter(functionFilter);
		
	    memoryManagementParam.setVisibility(Settable.NONE);;
		dataSetNaming.setVisibility(Settable.NONE);;
	}

	@Override
	protected DataMessageComponent getTransformedMessage(List<DataMessageComponent> cache) throws ProcessingException {
		
		DataMessageComponent ret = MessageUtils.mergeAll(cache);
		retainAll(dataFilter.getExpression(),      ret.getList());
		retainAll(roiFilter.getExpression(),       ret.getROIs());
		retainAll(functionFilter.getExpression(),  ret.getFunctions());
		return ret;
	}

	private void retainAll(String expression, Map<String,?> map) {
		
		if (map==null || map.isEmpty()) return;
		if (expression==null || "".equals(expression)) return;
		map.keySet().retainAll(ListUtils.getList(expression));
	}

	@Override
	protected String getOperationName() {
		return "Data Filter";
	}

}
