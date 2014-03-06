/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.actors.net;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.List;

import org.dawb.passerelle.actors.file.SubstituteTransformer;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.DataMessageException;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.IVariable.VARIABLE_TYPE;
import org.dawb.passerelle.common.message.SubstitutionParticipant;
import org.dawb.passerelle.common.message.Variable;
import org.dawb.passerelle.common.utils.SubstituteUtils;

import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.workbench.model.actor.IPartListenerActor;
import com.isencia.passerelle.workbench.model.actor.IResourceActor;

/**
 * A actor for replacing variables in a file and writing it to a socket.
 * 
 * Designed for running data collection commands on the GDA Server
 * 
 * @author gerring
 *
 */
public class TelnetTransformer extends SubstituteTransformer implements SubstitutionParticipant, IResourceActor, IPartListenerActor {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4358470952296340116L;
	
	protected StringParameter   uri;
	protected StringParameter   port;

	public TelnetTransformer(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		super(container, name);
		
		this.templateParam.setDisplayName("Script Location");
		templateParam.setExpression("/${project_name}/src/gda-script.py");
		
		this.outputParam.setDisplayName("Substitute Temporary Folder");
		outputParam.setExpression("/${project_name}/tmp/");
		
		this.uri = new StringParameter(this, "URI");
		uri.setExpression("localhost");
		registerConfigurableParameter(uri);
		
		this.port = new StringParameter(this, "Port");
		port.setExpression("9999");
		registerConfigurableParameter(port);

	}

	@Override
	protected DataMessageComponent getTransformedMessage(final List<DataMessageComponent> cache) throws ProcessingException {
		
		DataMessageComponent sub = super.getTransformedMessage(cache);
		
		InputStream  inStream  = null;
		OutputStream outStream = null;
		try {	
			SocketAddress sa = new InetSocketAddress(uri.getExpression(), Integer.parseInt(port.getExpression()));
			Socket sock = new Socket();
			sock.connect(sa,  10*1000);
			
			sub.putScalar("uri",  uri.getExpression());
			sub.putScalar("port", port.getExpression());
			
			inStream  = sock.getInputStream();
			
			final String       uniqueId = createUniqueId();
			final StreamMonitor gobbler = new StreamMonitor(inStream, "telnet thread", encoding.getExpression(), uniqueId);
			gobbler.setDaemon(true);
			gobbler.start(); // Keeps running. We stop when the unique id is seen in the output.
			
			outStream = sock.getOutputStream();
			
			final String        path = SubstituteUtils.substitute("${substitute_output}", sub.getScalar());
			BufferedWriter out = write(new FileInputStream(path), outStream);
		    
			out.write("print \""+uniqueId+"\"\n");
			out.flush();
			
			gobbler.block(); // Waits until the id appears in the output.
			
			if (gobbler.getScanLocations()!=null && !gobbler.getScanLocations().isEmpty()) {
				final List<String> locs = gobbler.getScanLocations();
				int scanNumber = 0;
				for (String loc : locs) {
					scanNumber++;
					sub.putScalar("scan_location"+scanNumber, loc);
				}
			}
			
			return sub;
			
		} catch (Throwable ne) {
			DataMessageException dme = super.createDataMessageException("Cannot telnet!", ne);
			dme.getDataMessageComponent().putScalar("uri",  uri.getExpression());
			dme.getDataMessageComponent().putScalar("port", port.getExpression());
			throw dme;
		} finally {
			close(inStream);
			close(outStream);
		}
 
	}
	
	private BufferedWriter write(FileInputStream fileInputStream, OutputStream outStream) throws Exception {
		
		final BufferedReader reader = new BufferedReader(new InputStreamReader(fileInputStream, encoding.getExpression()));
		final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outStream, encoding.getExpression()));
		
		try {
			String line = null;
			while((line=reader.readLine())!=null) {
				logger.debug("Sending > "+line);
				writer.write(line);
				writer.newLine();
			}
		} finally {
			reader.close();
			writer.flush();
		}
		
		return writer;
	}

	@Override
	public List<IVariable> getOutputVariables() {
		
		final List<IVariable> ret = super.getOutputVariables();
		ret.add(new Variable("uri",  VARIABLE_TYPE.SCALAR, uri.getExpression(),  String.class));
		ret.add(new Variable("port", VARIABLE_TYPE.SCALAR, port.getExpression(), Integer.class));
		ret.add(new Variable("scan_location1", VARIABLE_TYPE.SCALAR, "/home/fedid/gda_training/gda-training-config/users/data/2013/1.dat", String.class));
		
        return ret;
	}

	
	private static volatile int id = 0;

	private synchronized String createUniqueId() {
		id++;	
		return "Telnet Command Completed (key="+id+")";
	}

	private void close(Closeable strm) {
		if ( strm !=null ) {
			try {
				strm.close();
			} catch (IOException e) {
				logger.error("Cannot close stream!", e);
			}	
		}
	}
}
