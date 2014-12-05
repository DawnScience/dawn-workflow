package org.dawb.passerelle.common.parameter;

import java.io.IOException;

import org.castor.util.Base64Decoder;
import org.castor.util.Base64Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.isencia.passerelle.workbench.model.editor.ui.properties.CellEditorAttribute;

/**
 * To use this attribute the object you wish to serialize must do so with a standard ObjectMapper
 * from jackson.
 * 
 * @author fcp94556
 *
 */
public abstract class JSONCellEditorParameter<T> extends StringParameter implements CellEditorAttribute {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3435802171552221838L;

	private static Logger logger = LoggerFactory.getLogger(JSONCellEditorParameter.class);

	protected final Marshaller<T> mapper;

	public JSONCellEditorParameter(NamedObj container, String name) throws IllegalActionException, NameDuplicationException {
		super(container, name);
		this.mapper = createMarshaller();
	}

	/**
	 * Override to provide custom marshaling
	 * @return
	 */
	protected Marshaller<T> createMarshaller() {
		final ObjectMapper def = new ObjectMapper();
		def.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
		return new Marshaller<T>() {
			@Override
			public String marshal(T value) throws JsonProcessingException {
				return def.writeValueAsString(value);
			}
			@Override
			public Object unmarshal(String str, Class<? extends T> clazz) throws JsonParseException, JsonMappingException, IOException {
				return def.readValue(str, clazz);
			}
		};
	}

	public final T getBeanFromValue(final Class<? extends T> clazz) {

		try {
			String encoded = getExpression();
			if (encoded==null || "".equals(encoded)) return clazz.newInstance();

			try { // Unmarshall as JSON 
				final byte[] bytes = Base64Decoder.decode(encoded);
				String bean = new String(bytes, "UTF-8");
				return (T)mapper.unmarshal(bean, clazz);

			} catch (Exception ne) { // Old moml files have Base64 XML, we try that
				logger.error("Cannot read bean ", ne);
				return clazz.newInstance();
			}
		} catch (Exception ne) {
			logger.error("There is no null argument constructor in "+clazz.getName(), ne);
			try {
				return clazz.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				return null;
			}
		}
	}	

	/**
	 * returns the string which is to be saved in the parameter.
	 * @param bean
	 * @return
	 */
	public final String getValueFromBean(T bean) {
		if (bean==null) return null;
		try {
			String json = mapper.marshal(bean);
			final String save = new String(Base64Encoder.encode(json.getBytes("UTF-8")));
			return save;
		} catch (Exception e) {
			logger.error("Cannot write bean "+bean, e);
			return null;
		}
	}
	
	
	public void setValue(T value) {
		String enc = getValueFromBean(value);
		setExpression(enc);
	}
	
	public T getValue(Class<? extends T> clazz) {
		return getBeanFromValue(clazz);
	}

}
