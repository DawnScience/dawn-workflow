package org.dawb.passerelle.actors.data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.dawb.passerelle.common.actors.AbstractDataMessageTransformer;
import org.dawb.passerelle.common.message.MessageUtils;
import org.eclipse.dawnsci.analysis.api.message.DataMessageComponent;
import org.eclipse.dawnsci.analysis.dataset.impl.Dataset;
import org.eclipse.dawnsci.analysis.dataset.impl.DatasetFactory;

import com.isencia.passerelle.actor.ProcessingException;

import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

public class DatasetMean extends AbstractDataMessageTransformer {

	private static final long serialVersionUID = 1L;
	private StringParameter meanDirection;
	private StringParameter dataName;

	public DatasetMean(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);

		meanDirection = new StringParameter(this, "Mean Direction");
		meanDirection.setExpression("0");
		registerConfigurableParameter(meanDirection);

		dataName = new StringParameter(this, "Data to Mean");
		registerConfigurableParameter(dataName);

	}

	@Override
	protected DataMessageComponent getTransformedMessage(
			List<DataMessageComponent> cache) throws ProcessingException {

		// get the data out of the message, name of the item should be specified
		final Map<String, Serializable> data = MessageUtils.getList(cache);

		// prepare the output message
		DataMessageComponent result = new DataMessageComponent();

		// put all the datasets in for reprocessing
		for (String key : data.keySet()) {
			result.addList(key, DatasetFactory.createFromObject(data.get(key)));
		}

		// get the required datasets
		String dataset = dataName.getExpression();
		int axis = Integer.parseInt(meanDirection.getExpression());

		Dataset dataDS = DatasetFactory.createFromObject(data.get(dataset)).clone();

		Dataset mean = dataDS.mean(axis);

		mean.setName(dataDS.getName()+"_mean");
		
		result.addList(dataDS.getName()+"_mean", mean);

		return result;
	}

	@Override
	protected String getExtendedInfo() {
		return "Get the mean in a direction of a dataset";
	}
	
	@Override
	protected String getOperationName() {
		return "mean";
	}

}
