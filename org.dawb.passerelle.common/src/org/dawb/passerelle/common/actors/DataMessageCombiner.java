package org.dawb.passerelle.common.actors;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.DataMessageException;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.workbench.jmx.UserDebugBean;
import org.eclipse.core.resources.IProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;

import com.isencia.passerelle.actor.InitializationException;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.actor.TerminationException;
import com.isencia.passerelle.actor.Transformer;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.workbench.model.utils.ModelUtils;

/**
 * A Synchronizer designed to replace the Expression Mode parameter
 * 
 * Fires once after each input wire has fired once.
 * 
 * @author fcp94556
 *
 */
public class DataMessageCombiner extends Transformer implements IProjectNamedObject {
	
	private static final Logger logger = LoggerFactory.getLogger(DataMessageCombiner.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = 4497761553258309813L;
	  
	protected final List<DataMessageComponent> cache;

	public DataMessageCombiner(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		super(container, name);
		cache = new ArrayList<DataMessageComponent>(7);
		ActorUtils.createDebugAttribute(this);
	}

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
			
	        sendOutputMsg(output, MessageUtils.getDataMessage(despatch));
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
			return ModelUtils.getProject(this);
		} catch (Exception e) {
			logger.error("Cannot get the project for actor "+getName(), e);
			return null;
		}
	}
}
