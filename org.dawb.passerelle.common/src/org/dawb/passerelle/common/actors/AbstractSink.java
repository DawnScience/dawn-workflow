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
import ptolemy.actor.gui.style.CheckBoxStyle;
import ptolemy.data.BooleanToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

import com.isencia.passerelle.actor.InitializationException;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.actor.v5.Actor;
import com.isencia.passerelle.actor.v5.ActorContext;
import com.isencia.passerelle.actor.v5.ProcessRequest;
import com.isencia.passerelle.actor.v5.ProcessResponse;
import com.isencia.passerelle.core.ErrorCode;
import com.isencia.passerelle.core.PasserelleException;
import com.isencia.passerelle.core.Port;
import com.isencia.passerelle.core.PortFactory;
import com.isencia.passerelle.core.PortHandler;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.MessageHelper;

/**
 * Base class for all Passerelle sinks:
 * 
 * @author erwin
 */
public abstract class AbstractSink extends Actor {
  private static final long serialVersionUID = 1L;

  private static Logger LOGGER = LoggerFactory.getLogger(AbstractSink.class);

  /**
   * Holds the last received message
   */
  protected ManagedMessage message = null;

  public Port input = null;
  private PortHandler inputHandler = null;

  private boolean passThrough = false;
  public Parameter passThroughParam = null;

  public final static String PASSTHROUGH_PARAM = "PassThrough";

  /**
   * Constructor for Sink.
   * 
   * @param container
   * @param name
   * @throws NameDuplicationException
   * @throws IllegalActionException
   */
  public AbstractSink(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
    super(container, name);

    input = PortFactory.getInstance().createInputPort(this, null);

    passThroughParam = new Parameter(this, "PassThrough", new BooleanToken(false));
    passThroughParam.setTypeEquals(BaseType.BOOLEAN);
    registerExpertParameter(passThroughParam);

    new CheckBoxStyle(passThroughParam, "style");

    _attachText("_iconDescription", "<svg>\n" + "<rect x=\"-20\" y=\"-20\" width=\"40\" " + "height=\"40\" style=\"fill:green;stroke:green\"/>\n"
        + "<line x1=\"-19\" y1=\"-19\" x2=\"19\" y2=\"-19\" " + "style=\"stroke-width:1.0;stroke:white\"/>\n"
        + "<line x1=\"-19\" y1=\"-19\" x2=\"-19\" y2=\"19\" " + "style=\"stroke-width:1.0;stroke:white\"/>\n"
        + "<line x1=\"20\" y1=\"-19\" x2=\"20\" y2=\"20\" " + "style=\"stroke-width:1.0;stroke:black\"/>\n" + "<line x1=\"-19\" y1=\"20\" x2=\"20\" y2=\"20\" "
        + "style=\"stroke-width:1.0;stroke:black\"/>\n" + "<line x1=\"19\" y1=\"-18\" x2=\"19\" y2=\"19\" " + "style=\"stroke-width:1.0;stroke:grey\"/>\n"
        + "<line x1=\"-18\" y1=\"19\" x2=\"19\" y2=\"19\" " + "style=\"stroke-width:1.0;stroke:grey\"/>\n" +

        "<circle cx=\"0\" cy=\"0\" r=\"10\"" + "style=\"fill:white;stroke-width:2.0\"/>\n" + "<line x1=\"0\" y1=\"0\" x2=\"15\" y2=\"0\" "
        + "style=\"stroke-width:2.0\"/>\n" + "<line x1=\"12\" y1=\"-3\" x2=\"15\" y2=\"0\" " + "style=\"stroke-width:2.0\"/>\n"
        + "<line x1=\"12\" y1=\"3\" x2=\"15\" y2=\"0\" " + "style=\"stroke-width:2.0\"/>\n" + "</svg>\n");
  }
  
  @Override
  protected Logger getLogger() {
    return LOGGER;
  }

  /**
   * Check whether the changed attribute corresponds to the "PassThrough" parameter and if so, adjust the param's value.
   * 
   * @param The
   *          changed attribute
   */
  public void attributeChanged(Attribute attribute) throws IllegalActionException {
    LOGGER.trace("{} attributeChanged() - entry : {}", getFullName(), attribute);

    if (attribute == passThroughParam) {
      setPassThrough(((BooleanToken) passThroughParam.getToken()).booleanValue());
    } else
      super.attributeChanged(attribute);

    LOGGER.trace("{} attributeChanged() - exit", getFullName());
  }

  protected void doInitialize() throws InitializationException {
    if (input.getWidth() > 0) {
      inputHandler = createPortHandler(input);
      inputHandler.start();
    } else {
      requestFinish();
    }
  }

  /**
   * Returns the passThrough.
   * 
   * @return boolean
   */
  public boolean isPassThrough() {
    return passThrough;
  }

  /**
   * Sets the passThrough.
   * 
   * @param passThrough
   *          The passThrough to set
   */
  public void setPassThrough(boolean passThrough) {
    this.passThrough = passThrough;
  }

  protected boolean doPreFire() throws ProcessingException {
    boolean result = true;
    Token token = inputHandler.getToken();
    if (token == null) {
      message = null;
    } else if (!token.isNil()) {
      try {
        message = MessageHelper.getMessageFromToken(token);
      } catch (PasserelleException e) {
        throw new ProcessingException(ErrorCode.FLOW_EXECUTION_ERROR, "Error getting message from input " + token, this, e);
      }
    } else {
      result = false;
    }
    return result && super.doPreFire();
  }

  /**
   * Default and fully-functional implementation, relying on the ISenderChannel instance to send out all messages
   * received on the actor's input port.
   * 
   * @throws ProcessingException
   */
  final protected void doFire() throws ProcessingException {
    if (message != null) {
      notifyStartingFireProcessing();
      try {
        getLogger().debug("{} - Sink generated message : {}", getFullName(), getAuditTrailMessage(message, null));
        sendMessage(message);
      } finally {
        notifyFinishedFireProcessing();
      }
    } else {
      requestFinish();
    }
  }

  @Override
	protected void process(ActorContext ctxt, ProcessRequest request, ProcessResponse response) throws ProcessingException {
		// TODO Auto-generated method stub
		
	}
  /**
   * @param outgoingMessage
   * @throws ProcessingException
   */
  protected abstract void sendMessage(ManagedMessage outgoingMessage) throws ProcessingException;
}
