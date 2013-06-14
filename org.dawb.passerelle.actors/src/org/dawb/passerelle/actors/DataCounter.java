/* Copyright 2010 - iSencia Belgium NV

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
package org.dawb.passerelle.actors;




import java.util.ArrayList;
import java.util.List;

import org.dawb.passerelle.common.actors.AbstractDataMessageTransformer;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.MessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Settable;

import com.isencia.passerelle.actor.ProcessingException;

/**
 * Produce a counter output.
 * 
 * @version 1.0
 * @author edeley
 */

public class DataCounter extends AbstractDataMessageTransformer {

	private static final Logger logger = LoggerFactory.getLogger(DataCounter.class);

	
	private int storedValue;
	private boolean readValue = false;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4663716377018297122L;
	private StringParameter nameParam;

	public DataCounter(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		super(container, name);
		
	    nameParam = new StringParameter(this, "Name");
		nameParam.setExpression("x");
		nameParam.setDisplayName("Scalar Name");
		registerConfigurableParameter(nameParam);
		
		memoryManagementParam.setVisibility(Settable.NONE);
		dataSetNaming.setVisibility(Settable.NONE);

	}
	
	
	@Override
	protected DataMessageComponent getTransformedMessage(List<DataMessageComponent> cache) throws ProcessingException {

        final String scalarName = nameParam.getExpression();
		try {
	        final DataMessageComponent comp  = MessageUtils.mergeAll(cache);
	        int value;
	        try {
		        value  = Integer.parseInt(comp.getScalar(scalarName));	        	
	        } catch (Exception e) { 
		        value  = (int)Double.parseDouble(comp.getScalar(scalarName));
	        }
	        readValue = true;
//	        if (MODES.get(2).equals(passModeParameter.getExpression()) && readValue) {
//	        	value = storedValue;	
//	        }
	        value = value + 1;
	        storedValue = value;
	        
	        logInfo("Counter '" + scalarName + "' increased, new value = " + storedValue);
	        
	        comp.putScalar(scalarName, String.valueOf(value));
			return comp;
			
		} catch (Throwable ne) {
			throw createDataMessageException("Cannot increment '"+scalarName+"'", ne);
		}
	}

	@Override
	protected String getOperationName() {
		return "Counter";
	}

}