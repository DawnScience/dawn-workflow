package org.dawb.passerelle.actors.flow;

import java.io.File;
import java.util.Calendar;
import java.util.List;

import org.dawb.passerelle.common.actors.AbstractDataMessageSource;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.passerelle.common.message.Variable;
import org.dawb.passerelle.common.message.IVariable.VARIABLE_TYPE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.data.IntToken;
import ptolemy.data.expr.Parameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import uk.ac.diamond.scisoft.analysis.message.DataMessageComponent;

import com.isencia.passerelle.actor.InitializationException;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.message.ManagedMessage;

/**
 * A simple source which fires every time interval unless the workflow is stopped.
 * 
 * The 'current_time' in milliseconds and the 'total_time' we have monitored
 * are passed down the workflow.
 * 
 * @author fcp94556
 *
 */
public class TimerSource extends AbstractDataMessageSource {

	private static final Logger logger = LoggerFactory.getLogger(TimerSource.class);
	
	/**
     * Trigger message
     */
	private ManagedMessage triggerMsg;

	/**
	 * The data will not be fired into the pipeline greater than this rate.
	 */
	private Parameter sourceFreq;

	/**
	 * Time waited so far
	 */
	private int totalTime;

	
	public TimerSource(CompositeEntity container, String name)
			throws NameDuplicationException, IllegalActionException {
		super(container, name);
		
		this.sourceFreq = new Parameter(this, "Timing Frequency", new IntToken(100));
		registerConfigurableParameter(sourceFreq);
	}

	public void doPreInitialize() throws InitializationException {
		super.doPreInitialize();
		totalTime = 0;
	}
	/**
	 * 
	 */
	private static final long serialVersionUID = 1786893904448598313L;

	@Override
	protected ManagedMessage getDataMessage() throws ProcessingException {
		
		try {
			final DataMessageComponent  ret = triggerMsg !=null
					                        ? MessageUtils.coerceMessage(triggerMsg)
					                        : new DataMessageComponent();
					                        
			ret.putScalar("current_time", String.valueOf(System.currentTimeMillis()));
			ret.putScalar("total_time",   String.valueOf(totalTime));
			return MessageUtils.getDataMessage(ret, null);
		} catch (Exception ne) {
			throw createDataMessageException("Cannot send data from "+getClass().getSimpleName(), ne);
		
		}

	}

	@Override
	protected boolean mustWaitForTrigger() {
		if (triggerMsg!=null) return false;
		return trigger.getWidth()>0;
	}

	@Override
	protected String getExtendedInfo() {
		return "Monitors fridge temperature evey 100ms";
	}
	
	public boolean hasNoMoreMessages() {
		if (isFinishRequested()) return true;
        return false; // run while hardware is on.
	}
	
	protected boolean doPostFire() throws ProcessingException {
		try {
			final int freq = ((IntToken)sourceFreq.getToken()).intValue();

			if (isFinishRequested()) return super.doPostFire();
			
			Thread.sleep(freq);
			
			totalTime+=freq;
			
		} catch (Exception e) {
			throw new ProcessingException("Cannot wait for new temperature data read!", this, e);
		}
	    return super.doPostFire();
	}
	
	@Override
	public List<IVariable> getOutputVariables() {
		
		try {
		    final List<IVariable>    ret  = super.getOutputVariables();
		    
			ret.add(new Variable("current_time", VARIABLE_TYPE.SCALAR, System.currentTimeMillis(), Long.class));
			ret.add(new Variable("time_time",    VARIABLE_TYPE.SCALAR, 0, Long.class));
		
			return ret;
			
		} catch (Exception e) {
			logger.error("Cannot read variables", e);
			return null;
		}

	}
	protected void acceptTriggerMessage(ManagedMessage triggerMsg) {
		this.triggerMsg = triggerMsg;
	}

}