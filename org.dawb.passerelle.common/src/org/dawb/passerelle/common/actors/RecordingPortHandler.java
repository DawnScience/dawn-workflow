/* Copyright 2010 - European Synchrotron Radiation Facility

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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.actor.IOPort;
import ptolemy.actor.NoTokenException;
import ptolemy.actor.process.TerminateProcessException;
import ptolemy.data.Token;
import ptolemy.kernel.util.IllegalActionException;

import com.isencia.passerelle.core.PortHandler;
import com.isencia.passerelle.core.PortListener;
import com.isencia.passerelle.domain.ProcessThread;
import com.isencia.passerelle.util.LoggerManager;


public class RecordingPortHandler extends PortHandler {

	private static final Logger logger = LoggerFactory.getLogger(RecordingPortHandler.class);

	private boolean recordPorts = false;
	private Map<Integer,Object> portRecord;
	
    /** Creates a new instance of PortHandler */
    public RecordingPortHandler(IOPort ioPort) {
        this(ioPort, null);
    }
    
    /** Creates a new instance of PortHandler */
    public RecordingPortHandler(IOPort ioPort, boolean isRecordPorts) {
        this(ioPort, isRecordPorts, null);
    }

    /**
     * Creates a new PortHandler object.
     * 
     * @param ioPort
     * @param listener an object interested in receiving messages from the handler
     * in push mode
     */
    public RecordingPortHandler(IOPort ioPort, PortListener listener) {
        this(ioPort, false, listener);
    }
    
    private RecordingPortHandler(IOPort ioPort, boolean recordPorts, PortListener listener) {
    	
    	super(ioPort, listener);
        setRecordPorts(recordPorts);
    }

	/**
	 * Override to provide alternative implementation.
	 * @param index
	 * @return
	 */
    @Override
	protected Thread createChannelHandler(final int index) {
		return new RecordingHandler(index);
	}

	/**
     * Returns a message token received by this handler.
     * This method blocks until either:
     * <ul>
     * <li> a message has been received
     * <li> the message channels are all exhausted. In this case a null token is returned.
     * <ul>
     *
     * @return a message token received by the handler
     */
    public Token getToken() {
    	if(logger.isTraceEnabled()) {
    		logger.trace(getName()+" getToken() - entry");
    	}
        Token token = null;
        synchronized (channelLock) {
            if ((channelCount == 0) && hasNoMoreTokens()) {
                return null;
            }
        }

		if(mustUseHandlers()) {
			// messages will be in the queue
            try {
				token = (Token) queue.take();
				if(Token.NIL.equals(token)) {
					// indicates a terminating system
					queue.offer(token);
					token=null;
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			// just read the port directly
			try {
				if (ioPort.hasToken(0)) {
					token = ioPort.get(0);
                    if (recordPorts) {
                    	portRecord.put(0, token!=null?token:new Object());
                    }

				}
			} catch (NoTokenException e) {
				// do nothing, just return null
			} catch (IllegalActionException e) {
				// do nothing, just return null
			}
		}
    	if(logger.isTraceEnabled()) {
    		logger.trace(getName()+" getToken() - exit - token : "+token);
    	}

        return token;
    }
    

    //~ Classes ������������������������������������������������������������������������������������������������������������������������������������������������

    private class RecordingHandler extends Thread {
        private Token token = null;
        private boolean terminated = false;
        private int channelIndex = 0;

        public RecordingHandler(int channelIndex) {
            this.channelIndex = channelIndex;
        }

        public void run() {
        	try {
				LoggerManager.pushMDC(ProcessThread.ACTOR_MDC_NAME,actorInfo);
	
				if(logger.isTraceEnabled()) {
	        		logger.trace(RecordingPortHandler.this.ioPort.getFullName()+" ChannelHandler."+channelIndex+" run() - entry");
	        	}
	
	            while (!terminated) {
	                fetch();
	            }
	            synchronized (channelLock) {
	                channelCount--;
	            }
	
	            // No more channels active
	            // Force queue to return
	            if (channelCount == 0) {
	                queue.offer(Token.NIL);
	                if (listener != null) {
	                    listener.noMoreTokens();
	                }
	            }
				
	        	if(logger.isTraceEnabled()) {
	        		logger.trace(RecordingPortHandler.this.ioPort.getFullName()+" ChannelHandler."+channelIndex+" run() - exit");
	        	}
        	} catch (Throwable t) {
        		logger.error(RecordingPortHandler.this.ioPort.getFullName()+" - Error in ChannelHandler",t);
        		t.printStackTrace();
        		throw new RuntimeException(t);
        	} finally {
        		LoggerManager.popMDC(ProcessThread.ACTOR_MDC_NAME);
        	}
        }

        private void fetch() {
        	if(logger.isTraceEnabled()) {
        		logger.trace(RecordingPortHandler.this.ioPort.getFullName()+" ChannelHandler."+channelIndex+" fetch() - entry");
        	}

            try {
                if (ioPort.hasToken(channelIndex)) {
                    token = ioPort.get(channelIndex);
                    if (recordPorts) {
                    	portRecord.put(channelIndex, token!=null?token:new Object());
                    }
                    
                    if(logger.isDebugEnabled()) {
                    	logger.debug(RecordingPortHandler.this.ioPort.getFullName()+" ChannelHandler."+channelIndex+" fetch() - got token : "+token);
                    }

                    if (token != null) {
                        queue.offer(token);
                    } else {
                        terminated = true;
                    }
                }

                if (listener != null) {
                    listener.tokenReceived();
                }
            } catch (TerminateProcessException e) {
            	terminated = true; 
        	} catch (IllegalActionException e) {
                terminated = true;
            } catch (NoTokenException e) {
                terminated = true;
            }
            
        	if(logger.isTraceEnabled()) {
        		logger.trace(RecordingPortHandler.this.ioPort.getFullName()+" ChannelHandler."+channelIndex+" fetch() - exit");
        	}
        }
    }

    /**
     * Returns true when each input has fired received one message and 
     * the queue has dealt with it.
     * 
     * If true is returned then the count of which port has been received and
     * 
     * 
     * @return
     */
    public boolean isInputComplete() {
    	
    	if (queue==null || portRecord==null) return false;
    	if (!queue.isEmpty())                return false;
    	
    	final int size  = portRecord.size();
    	final int width = this.ioPort.getWidth();
    	final boolean complete = size>=width;
    	if (complete) {
    		portRecord.clear();
    	}
    	return complete;
    }
    
	public boolean isRecordPorts() {
		return recordPorts;
	}

	public void setRecordPorts(boolean recordPorts) {
		this.recordPorts = recordPorts;
		if (recordPorts) {
			portRecord = new ConcurrentHashMap<Integer, Object>(7);
		} else {
			portRecord = null;
		}
	}
}