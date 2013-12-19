/* Copyright 2011 - iSencia Belgium NV

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.dawb.passerelle.common.actors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ptolemy.data.Token;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

import com.isencia.passerelle.actor.InitializationException;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.core.ErrorCode;
import com.isencia.passerelle.core.PasserelleException;
import com.isencia.passerelle.core.Port;
import com.isencia.passerelle.core.PortFactory;
import com.isencia.passerelle.core.PortHandler;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.MessageHelper;

/**
  * The AbstractTriggeredSource class is a copy of the Passerelle class com.isencia.passerelle.actor.AbstractTriggeredSource
 *  and modified to inherit from AbstractSource. Olof Svensson, 2013-12-18
 * 
 * @author dirk
 */
public abstract class AbstractTriggeredSource extends AbstractSource {
  private static final long serialVersionUID = 1L;
  private static Logger LOGGER = LoggerFactory.getLogger(AbstractTriggeredSource.class);

  public final static String TRIGGER_PORT = "trigger";

  public Port trigger = null;
  private boolean triggerConnected = false;
  private PortHandler triggerHandler = null;

  /**
   * Constructor for Source.
   * 
   * @param container
   * @param name
   * @throws NameDuplicationException
   * @throws IllegalActionException
   */
  public AbstractTriggeredSource(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
    super(container, name);
    trigger = PortFactory.getInstance().createInputPort(this, TRIGGER_PORT, null);
  }

  @Override
  protected Logger getLogger() {
    return LOGGER;
  }
  
  protected void doInitialize() throws InitializationException {
    super.doInitialize();
    triggerConnected = trigger.getWidth() > 0;
    if (triggerConnected) {
      triggerHandler = createPortHandler(trigger);
      triggerHandler.start();
    }
  }

  protected boolean doPreFire() throws ProcessingException {
    boolean res = true;
    if (isTriggerConnected() && mustWaitForTrigger()) {
      ManagedMessage triggerMsg = waitForTrigger();
      if (isFinishRequested() || (triggerMsg == null))
        res = false;
    }
    return res && super.doPreFire();
  }

  protected boolean doPostFire() throws ProcessingException {
    boolean res = !hasNoMoreMessages() || isTriggerConnected();
    if (!res) {
      // just to make sure base class knows that we're finished
      requestFinish();
    } else {
      res = super.doPostFire();
    }
    return res;
  }

  /**
   * This method blocks until a trigger message has been received on the trigger port.
   * 
   * @throws ProcessingException
   */
  public final ManagedMessage waitForTrigger() throws ProcessingException {
    if (triggerConnected) {
      getLogger().debug("{} - Waiting for trigger", getFullName());
      Token token = triggerHandler.getToken();
      if (token == null) {
        // no more triggers will arrive so let's call it quits
        requestFinish();
        return null;
      } else if (!token.isNil()) {
        try {
          ManagedMessage message = MessageHelper.getMessageFromToken(token);
          acceptTriggerMessage(message);
          getLogger().debug("{} - Trigger received",getFullName());
          return message;
        } catch (PasserelleException e) {
          throw new ProcessingException(ErrorCode.FLOW_EXECUTION_ERROR, "Error handling trigger", this, e);
        }
      } else {
        return null;
      }
    }
    return null;
  }

  /**
   * Returns the triggerConnected.
   * 
   * @return boolean
   */
  public boolean isTriggerConnected() {
    return triggerConnected;
  }

  /**
   * "callback"-method that can be overridden by TriggeredSource implementations, if they want to act e.g. on the contents of a received trigger message.
   * 
   * @param triggerMsg
   */
  protected void acceptTriggerMessage(ManagedMessage triggerMsg) {

  }

  protected abstract boolean mustWaitForTrigger();
}
