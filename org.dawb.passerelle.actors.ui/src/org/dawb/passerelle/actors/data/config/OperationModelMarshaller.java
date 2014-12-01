package org.dawb.passerelle.actors.data.config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dawb.passerelle.common.parameter.Marshaller;
import org.dawnsci.persistence.json.IJSonMarshaller;
import org.dawnsci.persistence.json.JacksonMarshaller;
import org.eclipse.dawnsci.analysis.api.fitting.functions.IFunction;
import org.eclipse.dawnsci.analysis.api.processing.model.IOperationModel;
import org.eclipse.dawnsci.analysis.api.roi.IROI;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Class used to get around fact that ROIs and Functions
 * have the @JsonIgnore annotation.
 * 
 * We use Baha's 
 * 
 * 
 * @author fcp94556
 *
 */
public class OperationModelMarshaller implements Marshaller<IOperationModel> {

	private ObjectMapper    mapper;
	private IJSonMarshaller specials;
	
	OperationModelMarshaller() {
		mapper = new ObjectMapper();
		mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
		
		specials = new JacksonMarshaller();
	}
	
	@Override
	public String marshal(IOperationModel model) throws Exception {
		// Some objects are told not to Json in the model
		final OperationModelBean bean = new OperationModelBean(getSpecialObjects(model), model);
		return mapper.writeValueAsString(bean);
	}
	
	@Override
	public IOperationModel unmarshal(String str, Class<? extends IOperationModel> clazz) throws Exception {		
		OperationModelBean bean = mapper.readValue(str, OperationModelBean.class);
		IOperationModel   model = bean.getModel();
		setSpecialObjects(model, bean.getSpecials());
		return model;
	}

	
	private void setSpecialObjects(IOperationModel model, Map<String, String> specialsObs) throws Exception {
		for (String fieldName : specialsObs.keySet()) {
			Object roiOrFunc = specials.unmarshal(specialsObs.get(fieldName));
			model.set(fieldName, roiOrFunc);
		}
	}

	private Map<String, String> getSpecialObjects(IOperationModel model) throws Exception {
		
		final List<Field> allFields = new ArrayList<Field>(31);
		allFields.addAll(Arrays.asList(model.getClass().getDeclaredFields()));
		allFields.addAll(Arrays.asList(model.getClass().getSuperclass().getDeclaredFields()));

		Map<String, String> ret = new HashMap<String, String>(7);
		for (Field field : allFields) {
			Class<?> class1 = field.getType();
			if (IROI.class.isAssignableFrom(class1) || IFunction.class.isAssignableFrom(class1)) {
				ret.put(field.getName(), specials.marshal(model.get(field.getName())));
			}
		}
		return ret;
	}


}
