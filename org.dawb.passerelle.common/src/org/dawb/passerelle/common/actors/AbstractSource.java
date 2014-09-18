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

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

import com.isencia.passerelle.actor.InitializationException;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.actor.v5.Actor;
import com.isencia.passerelle.actor.v5.ActorContext;
import com.isencia.passerelle.actor.v5.ProcessRequest;
import com.isencia.passerelle.actor.v5.ProcessResponse;
import com.isencia.passerelle.core.ErrorCode;
import com.isencia.passerelle.core.Port;
import com.isencia.passerelle.core.PortFactory;
import com.isencia.passerelle.message.ManagedMessage;

/**
 * The AbstractSource class is a copy of the Passerelle class com.isencia.passerelle.actor.Source
 * and modified to inherit from actorv5. Olof Svensson, 2013-12-18
 * 
 * @author erwin
 */
public abstract class AbstractSource extends Actor {


  private static final long serialVersionUID = 7015362310836683912L;

  private static Logger LOGGER = LoggerFactory.getLogger(AbstractSource.class);

  /**
   * Holds the last received message
   */
  private ManagedMessage message = null;

  /**
   * The output port used by a source to send messages with the data received from external data feeds.
   */
  public Port output = null;
  /**
   * A boolean flag indicating whether the external data feed for this actor has been exhausted or not.
   */
  private boolean noMoreMessages = false;

  /**
   * Constructor for Source.
   * 
   * @param container
   * @param name
   * @throws NameDuplicationException
   * @throws IllegalActionException
   */
  public AbstractSource(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
    super(container, name);

    output = PortFactory.getInstance().createOutputPort(this);

    _attachText("_iconDescription", "<svg>\n" + "<rect x=\"-20\" y=\"-20\" width=\"40\" " + "height=\"40\" style=\"fill:orange;stroke:orange\"/>\n"
        + "<line x1=\"-19\" y1=\"-19\" x2=\"19\" y2=\"-19\" " + "style=\"stroke-width:1.0;stroke:white\"/>\n"
        + "<line x1=\"-19\" y1=\"-19\" x2=\"-19\" y2=\"19\" " + "style=\"stroke-width:1.0;stroke:white\"/>\n"
        + "<line x1=\"20\" y1=\"-19\" x2=\"20\" y2=\"20\" " + "style=\"stroke-width:1.0;stroke:black\"/>\n" + "<line x1=\"-19\" y1=\"20\" x2=\"20\" y2=\"20\" "
        + "style=\"stroke-width:1.0;stroke:black\"/>\n" + "<line x1=\"19\" y1=\"-18\" x2=\"19\" y2=\"19\" " + "style=\"stroke-width:1.0;stroke:grey\"/>\n"
        + "<line x1=\"-18\" y1=\"19\" x2=\"19\" y2=\"19\" " + "style=\"stroke-width:1.0;stroke:grey\"/>\n" + "<circle cx=\"0\" cy=\"0\" r=\"10\""
        + "style=\"fill:white;stroke-width:2.0\"/>\n" + "<line x1=\"-15\" y1=\"0\" x2=\"0\" y2=\"0\" " + "style=\"stroke-width:2.0\"/>\n"
        + "<line x1=\"-3\" y1=\"-3\" x2=\"0\" y2=\"0\" " + "style=\"stroke-width:2.0\"/>\n" + "<line x1=\"-3\" y1=\"3\" x2=\"0\" y2=\"0\" "
        + "style=\"stroke-width:2.0\"/>\n" + "</svg>\n");
  }
  
  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  protected boolean doPreFire() throws ProcessingException {
    message = getMessage();
    if(message!=null) {
      getLogger().trace("{} - received msg : ", getFullName(), getAuditTrailMessage(message, null));
    }
    return super.doPreFire();
  }

  protected void doFire() throws ProcessingException {
    if (message != null) {
      notifyStartingFireProcessing();
      noMoreMessages = false;
      try {
        sendOutputMsg(output, message);
      } catch (IllegalArgumentException e) {
        throw new ProcessingException(ErrorCode.MSG_DELIVERY_FAILURE, "Error sending msg on output", this, message, e);
      } finally {
        notifyFinishedFireProcessing();
      }
    } else {
      noMoreMessages = true;
    }
  }

  @Override
	protected void process(ActorContext ctxt, ProcessRequest request, ProcessResponse response) throws ProcessingException {
		// TODO Auto-generated method stub
		
	}
  
  protected boolean doPostFire() throws ProcessingException {
    boolean res = !hasNoMoreMessages();
    if (!res) {
      // just to make sure base class knows that we're finished
      requestFinish();
    } else {
      res = super.doPostFire();
      if (res) {
        try {
          getDirector().fireAtCurrentTime(this);
        } catch (IllegalActionException e) {
          throw new ProcessingException(ErrorCode.FLOW_EXECUTION_ERROR, "Error triggering a fire iteration for source actor " + getFullName(), this, e);
        }
      }
    }
    return res;
  }

  /**
   * @return
   * @throws ProcessingException
   */
  protected abstract ManagedMessage getMessage() throws ProcessingException;

  /**
   * @return a flag indicating whether this source has exhausted its datafeed.
   */
  public boolean hasNoMoreMessages() {
    return noMoreMessages;
  }

  protected void doInitialize() throws InitializationException {
    noMoreMessages = false;
    try {
      getDirector().fireAtCurrentTime(this);
    } catch (IllegalActionException e) {
      throw new InitializationException(ErrorCode.FLOW_EXECUTION_FATAL, "Error triggering a fire iteration for source actor " + getFullName(), this, e);
    }
  }
}