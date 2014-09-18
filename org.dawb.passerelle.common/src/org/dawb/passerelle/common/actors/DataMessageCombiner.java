package org.dawb.passerelle.common.actors;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.dawb.passerelle.common.message.DataMessageException;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.IVariable.VARIABLE_TYPE;
import org.dawb.passerelle.common.message.IVariableProvider;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.passerelle.common.message.Variable;
import org.dawb.workbench.jmx.UserDebugBean;
import org.eclipse.core.resources.IProject;
import org.eclipse.dawnsci.analysis.api.message.DataMessageComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.actor.IOPort;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.ChangeListener;
import ptolemy.kernel.util.ChangeRequest;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;

import com.isencia.passerelle.actor.InitializationException;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.actor.TerminationException;
import com.isencia.passerelle.actor.Transformer;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.resources.util.ResourceUtils;

/**
 * TODO check about deleting this actor completely, it breaks normal message flow handling
 * A Synchronizer designed to replace the Expression Mode parameter
 * 
 * Fires once after each input wire has fired once.
 * 
 * @author fcp94556
 *
 */
public class DataMessageCombiner extends Transformer implements IProjectNamedObject, IVariableProvider{
	
	private static final Logger logger = LoggerFactory.getLogger(DataMessageCombiner.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = 4497761553258309813L;
	  
	protected final List<DataMessageComponent> cache;
	/**
	 * Cached variables
	 */
	private ArrayList<IVariable> cachedUpstreamVariables;
 
	public DataMessageCombiner(final CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		super(container, name);
		cache = new ArrayList<DataMessageComponent>(7);
		
		this.cachedUpstreamVariables = new ArrayList<IVariable>(7);

		// Any change upsteam means they are invalid.
		container.addChangeListener(new ChangeListener() {		
			@Override
			public void changeFailed(ChangeRequest change, Exception exception) { }
			@Override
			public void changeExecuted(ChangeRequest change) {
				cachedUpstreamVariables.clear();
				if (!container.deepContains(DataMessageCombiner.this)) {
					container.removeChangeListener(this);
				}
			}
		});
		
		
		ActorUtils.createDebugAttribute(this);	}

	public void doPreInitialize() throws InitializationException{
		cache.clear();
	}

	@Override
	protected void doFire(ManagedMessage message) throws ProcessingException {
		
		try {
			if (message!=null) {
				DataMessageComponent msg = MessageUtils.coerceMessage(message);
				cache.add(msg);
			}
		} catch (ProcessingException pe) {
			throw pe;
		
		} catch (Exception ne) {
			throw new DataMessageException("Cannot add data from '"+message+"'", this, cache, ne);
		}

	}
	
	protected void doWrapUp() throws TerminationException {
		try {
			final DataMessageComponent despatch = getDespatch();
			if (despatch==null) return;
			
	        sendOutputMsg(output, MessageUtils.getDataMessage(despatch, null));
			cache.clear();
		} catch (Exception ne) {
			throw new TerminationException("Cannot despatch file message!", this, ne);
		}
	}

	private DataMessageComponent getDespatch() throws ProcessingException {
		
		try {
			ActorUtils.setActorExecuting(this, true);
			
			final DataMessageComponent despatch = MessageUtils.mergeAll(cache);
			if (despatch==null) return null;
			try {
				UserDebugBean bean = ActorUtils.create(this, MessageUtils.mergeAll(cache), despatch);
				if (bean!=null) bean.setPortName(input.getDisplayName());
				ActorUtils.debug(this, bean);
			} catch (Exception e) {
				logger.trace("Unable to debug!", e);
			}
			
			despatch.putScalar("operation.time."+getName(), DateFormat.getDateTimeInstance().format(new Date()));
			despatch.putScalar("operation.type."+getName(), "Combine");
			
			try {
			    despatch.putScalar("project_name", getProject().getName());
			} catch (Exception ignored) { // we can still combine.
				// do nothing
			}
			
			return despatch;
			
		} finally {
			ActorUtils.setActorExecuting(this, false);
		}
	}

	@Override
	protected String getExtendedInfo() {
		return "Combiner";
	}

	@Override
	public NamedObj getObject() {
		return this;
	}

	public IProject getProject() {
		try {
			return ResourceUtils.getProject(this);
		} catch (Exception e) {
			logger.error("Cannot get the project for actor "+getName(), e);
			return null;
		}
	}

	
    /**
     * Adds the scalar and the 
     */
	@Override
	public List<IVariable> getOutputVariables() {
		
        final List<IVariable> ret = getScalarOutputVariables();
     
        return ret;
	}
	
    /**
     * By default just passes the upstream string and path variables as 
     * these are normally preserved
     */
	protected List<IVariable> getScalarOutputVariables() {

		final List<IVariable> ret = new ArrayList<IVariable>(7);
		final List<IVariable> all = getInputVariables();
		for (IVariable iVariable : all) {
			if (iVariable.getVariableType()==VARIABLE_TYPE.PATH || 
				iVariable.getVariableType()==VARIABLE_TYPE.SCALAR|| 
				iVariable.getVariableType()==VARIABLE_TYPE.XML) {
				ret.add(iVariable);
			}
		}
		ret.add(new Variable("project_name", VARIABLE_TYPE.SCALAR, getProject().getName()));

		return ret;
	}

    /**
     * These are cached and cleared when the model is.
     * @return
     */
	public List<IVariable> getInputVariables() {

		synchronized (cachedUpstreamVariables) {
			
			if (cachedUpstreamVariables.isEmpty()) {
				@SuppressWarnings("rawtypes")
				final List connections = input.connectedPortList();
				for (Object object : connections) {
					final IOPort  port      = (IOPort)object;
					final Object connection =  port.getContainer();
					if (connection instanceof IVariableProvider) {
						final List<IVariable> vars = ((IVariableProvider)connection).getOutputVariables();
						if (vars!=null) cachedUpstreamVariables.addAll(vars);
					}
				}
			}
		}
		return cachedUpstreamVariables;
	}
	
	public boolean isUpstreamVariable(final String name) {
		final List<IVariable> up = getInputVariables();
		for (IVariable iVariable : up) {
			if (iVariable.getVariableName().equals(name)) return true;
		}
		return false;
	}	
}
