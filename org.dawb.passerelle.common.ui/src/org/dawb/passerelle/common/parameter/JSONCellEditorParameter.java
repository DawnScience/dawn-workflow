package org.dawb.passerelle.common.parameter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isencia.passerelle.workbench.model.editor.ui.properties.CellEditorAttribute;

/**
 * To use this attribute the object you wish to serialize must do so with a standard ObjectMapper
 * from jackson.
 * 
 * @author fcp94556
 *
 */
public abstract class JSONCellEditorParameter extends StringParameter implements CellEditorAttribute {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3435802171552221838L;

	private static Logger logger = LoggerFactory.getLogger(JSONCellEditorParameter.class);

	private final ObjectMapper mapper;

	public JSONCellEditorParameter(NamedObj container, String name) throws IllegalActionException, NameDuplicationException {
		super(container, name);
		this.mapper = new ObjectMapper();
	}


	public final Object getBeanFromValue(final Class<? extends Object> clazz) {

		try {
			if (getExpression()==null || "".equals(getExpression())) return clazz.newInstance();

			try { // Unmarshall as JSON 
				return mapper.readValue(getExpression(), clazz);

			} catch (Exception ne) { // Old moml files have Base64 XML, we try that
				logger.error("Cannot read bean "+super.getExpression(), ne);
				return clazz.newInstance();
			}
		} catch (Exception ne) {
			logger.error("There is no null argument constructor in "+clazz.getName(), ne);
			return new Object();
		}
	}	

	/**
	 * returns the string which is to be saved in the parameter.
	 * @param bean
	 * @return
	 */
	public final String getValueFromBean(Object bean) {
		if (bean==null) return null;
		try {
			return mapper.writeValueAsString(bean);
		} catch (Exception e) {
			logger.error("Cannot write bean "+bean, e);
			return null;
		}
	}

}
