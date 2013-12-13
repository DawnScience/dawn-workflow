package org.dawb.passerelle.actors.data;

import java.util.ArrayList;
import java.util.List;

import org.dawb.common.util.list.ListUtils;
import org.dawb.passerelle.common.DatasetConstants;
import org.dawb.passerelle.common.actors.AbstractDataMessageSource;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.IVariable.VARIABLE_TYPE;
import org.dawb.passerelle.common.message.Variable;

import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.MessageFactory;

/**
 * A source that reads several comma separated properties and passes these into the pipeline.
 * @author fcp94556
 *
 */
public class SystemPropertySource extends AbstractDataMessageSource {

	private StringParameter propertyNames;
	private StringParameter renameNames;

	public SystemPropertySource(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		super(container, name);
		
		this.propertyNames = new StringParameter(this, "Property Names");
		registerConfigurableParameter(propertyNames);
		
		this.renameNames = new StringParameter(this, "Rename Names");
		registerConfigurableParameter(renameNames);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -882368562641398476L;

	protected boolean firedOnce = false;

	@Override
	protected ManagedMessage getDataMessage() throws ProcessingException {
		
		if (firedOnce) return null;
		try {
	     	ManagedMessage msg = MessageFactory.getInstance().createMessage();
			try {
				msg.setBodyContent(getSystemProperties(), DatasetConstants.CONTENT_TYPE_DATA);
				
		        return msg;
		        
			} catch (Exception e) {
	            throw createDataMessageException(e.getMessage(), e);
	            
			}
		} finally {
			firedOnce = true;
		}
	}
	
	private DataMessageComponent getSystemProperties() {
		
		DataMessageComponent ret = new DataMessageComponent();
		final List<String> names = ListUtils.getList(propertyNames.getExpression());
		
		List<String> renames = null;
		if (renameNames.getExpression()!=null && !"".equals(renameNames.getExpression())) {
			renames =  ListUtils.getList(renameNames.getExpression());
		}
		for (int i = 0; i < names.size(); i++) {
	        final String key = names.get(i);
	        String name = key;
	        if (renames!=null) {
	        	try {
	        		name = renames.get(i);
	        	} catch (Throwable ignored) {
	        		name = key;
	        	}
	        }
			ret.putScalar(name, System.getProperty(key));
		}
		return ret;
	}

	@Override
	public List<IVariable> getOutputVariables() {
		
		final List<IVariable> ret = new ArrayList<IVariable>(7);
		final List<String> names  = ListUtils.getList(propertyNames.getExpression());
		for (String name : names) {
			ret.add(new Variable(name, VARIABLE_TYPE.SCALAR,  "<system property value of '"+name+"'>", String.class));
		}
        return ret;
	}

	@Override
	protected boolean mustWaitForTrigger() {
		return false;
	}

}
