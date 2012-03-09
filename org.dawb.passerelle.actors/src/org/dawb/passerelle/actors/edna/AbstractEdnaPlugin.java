/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.actors.edna;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dawb.common.python.EDJob;
import org.dawb.common.python.PythonUtils;
import org.dawb.common.python.rpc.PythonService;
import org.dawb.common.util.SubstituteUtils;
import org.dawb.common.util.io.Grep;
import org.dawb.common.util.io.IFileUtils;
import org.dawb.common.util.io.PropUtils;
import org.dawb.common.util.xml.XMLUtils;
import org.dawb.passerelle.common.actors.AbstractDataMessageTransformer;
import org.dawb.passerelle.common.actors.AbstractPassModeTransformer;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.DataMessageComponent.VALUE_TYPE;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.IVariableProvider;
import org.dawb.passerelle.common.message.MessageUtils;
import org.dawb.passerelle.common.message.XPathVariable;
import org.dawb.passerelle.common.parameter.ParameterUtils;
import org.dawb.passerelle.editors.EdnaActorMultiPageEditor;
import org.dawb.passerelle.editors.SubstitutionParticipant;
import org.dawb.passerelle.editors.XPathParticipant;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.PlatformUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.actor.Actor;
import ptolemy.actor.ExecutionListener;
import ptolemy.actor.Manager;
import ptolemy.actor.util.Time;
import ptolemy.data.BooleanToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Settable;

import com.isencia.passerelle.actor.InitializationException;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.util.ptolemy.ResourceParameter;
import com.isencia.passerelle.workbench.model.actor.IResourceActor;
import com.isencia.passerelle.workbench.model.actor.ResourceObject;
import com.isencia.passerelle.workbench.model.utils.ModelUtils;

public abstract class AbstractEdnaPlugin extends AbstractDataMessageTransformer implements Actor, IResourceActor, IVariableProvider, SubstitutionParticipant, XPathParticipant {
	
	private static String[] PYLINK_CHOICES      = new String[]{"RPC",    "Jep"};
	private static String[] PYDEBUG_CHOICES     = new String[]{"Start new python rcp server",    "Python rpc server already running"};

	private static Logger logger = LoggerFactory.getLogger(AbstractEdnaPlugin.class);
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1551436102354087550L;
	
	
	protected StringParameter pluginParam;
	protected String          plugin;
	
	protected ResourceParameter inputParam;
	protected String            inputString;

	protected ResourceParameter outputParam;
	protected String            output;
	
	private final Parameter             passInputsParameter;
	private final Parameter             debugModeParameter;
	private final Parameter             enableChangeWorkingDirParameter;
	private boolean                     isPassInputs = true;
	private boolean                     isDebugMode = false;
	private boolean                     isEnabledChangeWorkingDir = false;
	
	private StringParameter             pythonCommand;
	private StringParameter             pythonLink;
	private StringParameter             pythonDebug;
	

	public AbstractEdnaPlugin(final String          pluginName,
			                  final CompositeEntity container, 
			                  final String          name) throws NameDuplicationException, IllegalActionException {
		super(container, name);
		
		pluginParam = new StringParameter(this, "Plugin Name");
		registerExpertParameter(pluginParam);
		pluginParam.setExpression(pluginName);
		this.plugin = pluginName;
		
		inputParam = new ResourceParameter(this, "Input Template", "XML File", "*.xml");
		registerConfigurableParameter(inputParam);
		
		outputParam = new ResourceParameter(this, "Output Template", "XML File", "*.xml");
		registerConfigurableParameter(outputParam);

		passModeParameter.setExpression(EXPRESSION_MODE.get(1));
		memoryManagementParam.setVisibility(Settable.NONE);
		dataSetNaming.setVisibility(Settable.NONE);
		
		passInputsParameter = new Parameter(this,"Pass Inputs On",new BooleanToken(true));
		registerConfigurableParameter(passInputsParameter);

		debugModeParameter = new Parameter(this,"Debug mode",new BooleanToken(false));
		registerConfigurableParameter(debugModeParameter);

		enableChangeWorkingDirParameter = new Parameter(this,"Enable change to edna_working_dir",new BooleanToken(true));
		registerConfigurableParameter(enableChangeWorkingDirParameter);
		
		pythonCommand = new StringParameter(this, "Python Interpreter Command");
		registerConfigurableParameter(pythonCommand);
		pythonCommand.setExpression(PythonUtils.getPythonInterpreterCommand());

		// Expert param for changing between Jep and RPC
		pythonLink = new StringParameter(this,"Python Link") {

			public String[] getChoices() {
				return PYLINK_CHOICES;
			}
		};
		registerExpertParameter(pythonLink);
		pythonLink.setExpression(PYLINK_CHOICES[0]);
		
		// Expert param for turning on debug python server
		pythonDebug = new StringParameter(this,"Python Debug") {

			public String[] getChoices() {
				return PYDEBUG_CHOICES;
			}
		};
		registerExpertParameter(pythonDebug);
		pythonDebug.setExpression(PYDEBUG_CHOICES[0]);
	}
	
	public void attributeChanged(Attribute attribute) throws IllegalActionException {
		
		if (attribute == pluginParam) {
			plugin = pluginParam.getExpression();
		} else if (attribute == inputParam) {
			inputString = inputParam.getExpression();
		} else if (attribute == passInputsParameter) {
			final BooleanToken b = (BooleanToken) passInputsParameter.getToken();
			isPassInputs = b.booleanValue();
		} else if (attribute == debugModeParameter) {
			final BooleanToken b = (BooleanToken) debugModeParameter.getToken();
			isDebugMode = b.booleanValue();
		} else if (attribute == enableChangeWorkingDirParameter) {
			final BooleanToken b = (BooleanToken) enableChangeWorkingDirParameter.getToken();
			isEnabledChangeWorkingDir = b.booleanValue();
		} else if (attribute == outputParam) {
			output = outputParam.getExpression();
		}
		
	    super.attributeChanged(attribute);
		
	}
	
	protected static PythonService sharedService;
	protected static String        managerName;
	
	public boolean doPreFire() throws ProcessingException {
		
        final boolean ret = super.doPreFire();
		
        if (PYLINK_CHOICES[0].equals(pythonLink.getExpression())) {
        	
        	// Create a new python interpreter if the name of the Manager has changed,
        	// The name of the Manager must always be unique for this to work
			final String manName = getManager().getName();
			if (sharedService==null || !manName.equals(managerName)) {
				
				final boolean isRpcDebug = PYDEBUG_CHOICES[1].equals(pythonDebug.getExpression());
				
				try {
	                if (isRpcDebug) { // The python server could be running in a pydev session!
		                              // Very handy for debugging the python
	                	logger.debug(getName() + ": setting shared rpc service");
		            	sharedService = PythonService.openClient(PythonService.getDebugPort());
		            } else {
		            	// Normally start an interpreter and connect to it on a free port.
	                	logger.debug(getName() + ": setting normal rpc service");
		            	sharedService = PythonService.openConnection(pythonCommand.getExpression());
		            } 
	                
	                getManager().addExecutionListener(new ExecutionListener() {

						@Override
						public void executionFinished(Manager manager) {
							sharedService.stop();
						}

						@Override
						public void managerStateChanged(Manager manager) { }
	                	
						@Override
						public void executionError(Manager manager, Throwable throwable) {}

	                });
	                
				} catch (Exception ne) {
					throw new ProcessingException("Cannot start python connection!", this, ne);
				}
	
	            managerName = manName;
			}
        }
		
		return ret;
	}

	/**
	 * Should return the file path to the schema file for this plugin.
	 * This is a relative path to the workspace folder.
	 * 
	 * For instance "edna/execPlugins/plugins/EDPluginExecThumbnail-v1.0/datamodel/XSDataExecThumbnail.xsd"
	 * 
	 * Use / and not \
	 * 
	 * @return
	 */
	protected abstract String getSchemaLocation();
	
	/**
	 * Should return the file path to the schema file for this plugin.
	 * This is a relative path to the workspace folder.
	 * 
	 * For instance "edna/execPlugins/plugins/EDPluginExecThumbnail-v1.0/datamodel/XSDataExecThumbnail.xsd"
	 * 
	 * Use / and not \
	 * 
	 * @return
	 */
	protected abstract String getPythonLocation();
	
	
	/**
	 * The original idea for this method was to use the schema to generate
	 * some default input xml. Just as the action Generate ->Xml doe in eclipse
	 * on a xsd file.
	 * 
	 * However the xml was not really good enough for the default input. It turns
	 * out better for each actor to provide the default XML.
	 */
	public abstract String getDefaultInputXML();
	
	/**
	 * Please provide an implementation to return an example output xml in the
	 * edna folder for using as a practice value to run xpaths on.
	 */
	protected abstract String getExampleOutputLocation();
	
	/**
	 * Returns key value pairs for the variables and their corresponding
	 * xpath values or a string describing the parameter.
	 * 
	 * @param message
	 * @throws ProcessingException
	 */
	public abstract Properties getDefaultOutputProperties();

	@SuppressWarnings("unused")
	@Override
	protected DataMessageComponent getTransformedMessage(List<DataMessageComponent> cache) throws ProcessingException {
		
	
		String  outputXML = null;
		IFolder ednaWkDir = null;
    	logger.debug(getName() + " - in AbstractEdnaPlugin.getTransformedMessage");
		try {
			ednaWkDir = getEdnaWorkingDirFolder();
			String lastDir = ednaWkDir.getLocation().lastSegment();
			String absPathToWorkingDir = ednaWkDir.getLocation().toOSString();
			if (isEnabledChangeWorkingDir) {
				for (DataMessageComponent dataMessageComponent : cache) {
					if (dataMessageComponent.getScalar()!=null) {
						for (String name : dataMessageComponent.getScalar().keySet()) {						
							if (name.equals("edna_working_dir")) {
								absPathToWorkingDir = dataMessageComponent.getScalar().get(name)+"/"+lastDir;
								boolean success = (new File(absPathToWorkingDir)).mkdirs();
								if (!success) {
									logger.warn("Cannot create EDNA working directory: "+absPathToWorkingDir);
									absPathToWorkingDir = ednaWkDir.getLocation().toOSString();
									logger.warn("Reverting back to workspace location: "+absPathToWorkingDir);
								}
							}
						}
					}
				}
			}
			
            final EDJob  job  = new EDJob(sharedService, plugin, absPathToWorkingDir);
    		final String xml  = getDataInput(cache);
            job.setDataInput(xml);
            job.setDebugMode(isDebugMode);
            job.execute();
            
            outputXML = job.getDataOutput();
            
            // We get the variables to pass on
            final Map      stringProperties = PropUtils.loadProperties(getOutputFile().getContents());
		    final Map<String,String> xPaths = XPathVariable.getXPaths(stringProperties);
		    final Map<String,String> rename = XPathVariable.getRenames(stringProperties);
		    final Map<String,String>     in = MessageUtils.getValues(cache, xPaths.keySet(), this);
		    
		    
			if (xPaths==null) {
				throw createDataMessageException("Cannot run edna task "+getName()+", the xpaths could not be found!", null);
			}
			if (in==null) {
				throw createDataMessageException("Cannot run edna task "+getName()+", the xpath existing variables could not be extracted!", null);
			}
			if (outputXML==null || "".equals(outputXML)) {
				throw createDataMessageException("Cannot run edna task "+getName()+", the output XML is null!", null);
			}
			
			final DataMessageComponent despatch = new DataMessageComponent();
			final Map<String,String>   outputs  = XMLUtils.getVariables(xPaths, outputXML, in);				
			transformNames(outputs, rename);
			transformOutputValues(outputs);
			
			despatch.setMeta(MessageUtils.getMeta(cache));
			if (isPassInputs) {
				final Map<String,String> upStreamScalar = MessageUtils.getScalar(cache);
			    despatch.addScalar(upStreamScalar);
			}
			despatch.addScalar(outputs, true);
			assignAdditiveProperties(despatch, xPaths);

			return despatch;
            
		} catch (Throwable ne) {
			logger.error("Output XML from "+getName()+" may be invalid. It is:\n"+outputXML);
			throw createDataMessageException("Cannot run edna task "+plugin, ne);
			
		} finally {
			try {
	            if (ednaWkDir!=null) {
	            	ednaWkDir.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
	    			AbstractPassModeTransformer.refreshResource(ednaWkDir.getProject());
	            }
			} catch (Throwable ne) {
				logger.error("Cannot refresh folder "+ednaWkDir);
				throw createDataMessageException("Cannot run edna task "+plugin, ne);
			}
		}

	}

	/**
	 * Project used to get edna resources. 
	 * 
	 * You may set a system property org.dawb.edna.project.name to
	 * change the default project, or override this method.
	 * 
	 * @return
	 */
	
	public String getEdnaProject() {
		return System.getenv("EDNA_HOME");
	}

	
	private void assignAdditiveProperties(final DataMessageComponent despatch, final Map outputs) {
		
		for (Object key : outputs.keySet()) {
			if (outputs.get(key)==null || "".equals(outputs.get(key))) continue;
			despatch.setValueType(key.toString(), VALUE_TYPE.OVERWRITE_STRING);
		}
	}

	protected String getDataInput(List<DataMessageComponent> cache) throws Exception {
		
		final IFile              file = getInputFile();
		final List<CharSequence> referencedVars = XMLUtils.getVariables(file);
		final Map<String,String> variables = MessageUtils.getValues(cache, referencedVars, this);
		return SubstituteUtils.substitute(file.getContents(), variables);
	}

	@Override
	public int getResourceCount() {
		return 2;
	}
	
	@Override
	public ResourceObject getResource(int num) {
		
		if (num==0) {
			final ResourceObject ret = new ResourceObject();
			ret.setResource(getLinkerFile());
			ret.setResourceTypeName("Linker");
			ret.setEditorId(EdnaActorMultiPageEditor.ID);
			return ret;
		} else {
			final ResourceObject ret = new ResourceObject();
			final File res = new File(getEdnaProject() + "/" + getPythonLocation());
			ret.setResource(res);
			ret.setResourceTypeName("'"+plugin+"'");
			return ret;
		}
	}
	
	protected String getOutputTagName() throws Exception {
		String tag = getInputTagName();
		tag = tag.replace("Input", "Output");
		tag = tag.replace("input", "output");
		return tag;
	}
	
	private String cachedTagName = null;
	/**
	 * Should return the name of the XSData input object
	 * 
	 * @return
	 * @throws Exception 
	 */
	abstract protected String getInputTagName() throws Exception;
	

	private IResource momlFile;
	
	@Override
	public void setMomlResource(IResource momlFile) {
		this.momlFile = momlFile;
	}

	/**
	 * Creates or reuses a properties file in the workspace
	 * which carries the properties for the three (or so)
	 * files needed to link an edna node.
	 * 
	 * Method can be called a lot, needs to be fast.
	 * 
	 * @return
	 */
	private IResource getLinkerFile() {
		
		if (!PlatformUI.isWorkbenchRunning() || momlFile==null) return null;
		
		IFolder edna = null;
		try {
			edna = getEdnaXmlFolder();
			
			final StringBuilder props = new StringBuilder();
			props.append("org.dawb.edna.name = ");
			props.append(getName());
			props.append("\n");

			props.append("org.dawb.edna.moml = ");
			props.append(momlFile.getFullPath().toString());
			props.append("\n");
			
			props.append("org.dawb.edna.input = ");
			props.append(getInputFile().getFullPath().toString());
			props.append("\n");

			props.append("org.dawb.edna.output = ");
			props.append(getOutputFile().getFullPath().toString());
			props.append("\n");
			
			props.append("org.dawb.edna.schema = ");
			props.append(getEdnaProject()+"/"+getSchemaLocation());
			props.append("\n");
		
			final IFile linker = edna.getFile(getName()+".properties");
			if (linker.exists()) linker.delete(true, new NullProgressMonitor());
			final InputStream is = new ByteArrayInputStream(props.toString().getBytes("UTF-8"));
			linker.create(is, true, new NullProgressMonitor());
			linker.setHidden(true);
			
			return linker;
			
		} catch (Exception ne) {
			logger.error("Cannot create input xml!", ne);
		} 
        return null;
	}

	/**
	 * Creates one if its not there.
	 * @return
	 */
	protected IFile getInputFile() {
		try {
		    return getFileForParameter(inputParam, getInputTagName(), "xml", getDefaultInputXML());
		} catch (Exception ne) {
			logger.error("Cannot create input xml!", ne);
			return null;
		}
	}
	
	/**
	 * Creates one if its not there.
	 * @return
	 */
	protected IFile getOutputFile() {
		try {
	        final String out = PropUtils.getPropertiesAsString(getDefaultOutputProperties());
		    return getFileForParameter(outputParam, getOutputTagName(), "properties", out);
		} catch (Exception ne) {
			logger.error("Cannot create input xml!", ne);
			return null;
		}
	}

	/**
	 * Must return a real existing file which is used to 
	 * create the input to the edna node.
	 * 
	 * @return
	 */
	private IFile getFileForParameter(final ResourceParameter param,
							          final String            tag,
							          final String            ext,
			                          final String            defaultXml) throws Exception {

 	    synchronized (param) {
			
 	    	String path = param.getExpression();
 	    	path = ParameterUtils.substitute(path, this);
 	    	IFile file = null;
			// We generate a template input file if one does not exist.
			// This is only applicable in the UI.
	        // In the workflow, this file must have been set.
 	    	if (!path.equals("")) {
 	    		file = (IFile) ResourcesPlugin.getWorkspace().getRoot().findMember(path);
 	    		logger.debug(getName() + " - Linker getFileForParameter path="+path);
 	    	}
			final IFolder edna = getEdnaXmlFolder();
	        if ( file == null) {
	        	file = IFileUtils.getUniqueIFile(edna, tag, ext);
	        	param.setExpression(file.getFullPath().toString());
	        	logger.debug(getName() + " - Linker new input file: "+file.getFullPath().toString());
	        } else {
	        	param.setExpression(file.getFullPath().toString());
	        	logger.debug(getName() + " - Linker existing input file: "+file.getFullPath().toString());
	        }
	        	
	        
	        if (!file.exists()) {
		        // Only create a new file if we are not running through jmx!
	        	if ( (System.getProperty("org.dawb.worbench.jmx.service") == null) || (!System.getProperty("org.dawb.worbench.jmx.service").equals("true")) ) {
	        		logger.debug(getName() + " - Linker input file doesn't exist, we create one");
	        		final InputStream is = new ByteArrayInputStream(defaultXml.getBytes("UTF-8"));
	        		file.create(is, true, new NullProgressMonitor());
	        		file.refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());
	        	} else {
	        		throw createDataMessageException("Cannot run edna task "+getName()+", the linker path(s) could not be found!", null);
	        	}
	        }
			// Happens when model run in model runner
			return file;
	    }
	}



	private IFolder getEdnaXmlFolder() throws Exception {
		
		final IContainer cont = getProject();

		// Sub directory for the input file.
		final IFolder edna = cont.getFolder(new Path("edna-xml"));
		if (!edna.exists()) {
			try {
				edna.create(true, true, new NullProgressMonitor());
			} catch (Exception ne) {
				logger.error("Did not create new folder "+edna.getName(), ne);
			}
		}
		
		return edna;
	}
	
	private static final DateFormat DATE_FOLDER = new SimpleDateFormat("yyyyMMdd_HHmmss");
	private static final Object     LOCK        = new Object();
	
	private IFolder getEdnaWorkingDirFolder() throws Exception {
		
		final IProject cont = getProject();
		logger.debug(getName() + " - Containing project is "+cont);

		// Sub directory for the input file.
		final IFolder edna = cont.getFolder("edna-working-dir");
		
		// Best to use single lock blocks or lock entire methods.
		synchronized (LOCK) {
			if (!edna.exists()) {
				edna.create(true, true, new NullProgressMonitor());
			}

			Time   time       = getDirector()!=null
			                  ? getDirector().getModelTime()
			                  : new Time(null, 0);
			if (time.getLongValue()<=0)  {
				time = new Time(getDirector(), System.currentTimeMillis());
				if (getDirector()!=null) getDirector().setModelTime(time);
			}
			final String timeFolder = DATE_FOLDER.format(new Date(time.getLongValue()));
			
			final IFolder dir = edna.getFolder(timeFolder);
			if (!dir.exists()) {
				dir.create(true, true, new NullProgressMonitor());
			}
			
			return dir;
		}
	}

	public List<IVariable> getInputVariables() {
		
		final List<IVariable> inputs = new ArrayList<IVariable>(7);
		final List<String>    names  = new ArrayList<String>(7);
		
		final List<IVariable> vars = super.getInputVariables();				
		for (IVariable input : vars) {
			if (!names.contains(input.getVariableName())) {
				inputs.add(input);
				names.add(input.getVariableName());
			}
		}

		return inputs;
	}

	/**
	 * returns only the outputs from this plugin, not upstream ones.
	 * @return
	 * @throws CoreException 
	 * @throws IOException 
	 */
	public boolean isDefinitelyLocalOutput(final String name) throws IOException, CoreException {
		final Properties outputs  = PropUtils.loadProperties(getOutputFile().getContents());
		return outputs.containsKey(name);
	}
	/**
	 * Can be used to manipulate the output variable values after they
	 * have been evaluated. Bu default does nothing.
	 * 
	 * @param outputs
	 */
	protected void transformOutputValues(Map<String, String> outputs) {
		// TODO Auto-generated method stub
		
	}
	
	protected void transformNames(Map<String, String> outputs,
			                      Map<String, String> renames) {
		
		if (renames==null||renames.isEmpty()) return;
		for (String name : renames.keySet()) {
			final String rename = renames.get(name);
			final String value  = transformName(outputs.get(name), rename);
	        outputs.put(name, value);
		}
	}
	
	private static final Pattern FIRST_TAG = Pattern.compile("\\<([a-zA-Z0-9_]+)( |\\>).*", Pattern.DOTALL);
	
	private String transformName(String value, final String rename) {
		
		if (rename==null||value==null) return value;
		if ("".equals(rename)||"".equals(value)) return value;

		final Matcher matcher = FIRST_TAG.matcher(value.trim());
		if (!matcher.matches()) return value;
        final String  tag     = matcher.group(1);
        
        value = value.replace("<"+tag+">",  "<"+rename+">");
        value = value.replace("<"+tag+"/>", "<"+rename+"/>");
        value = value.replace("<"+tag+" ",  "<"+rename+" ");
        value = value.replace("</"+tag+">", "</"+rename+">");
        
        return value;
	}

	/**
	 * Can be used to manipulate the output variable values after they
	 * have been evaluated. Bu default does nothing.
	 * 
	 * @param outputs
	 */
	protected void transformExampleValue(IVariable var) {
		
		if (var instanceof XPathVariable) {
			final XPathVariable xp = (XPathVariable)var;
			if (getExampleOutputLocation()!=null) {
				xp.setExampleValue(getExampleValueFromFile(getExampleOutputLocation(), xp.getxPath(), xp.getRename()));
			}
		}
 		
	}

	/**
	 * Uses the data to get a subWedge example
	 * @return
	 */
	protected String getExampleValueFromFile(final String filePath, final String xPath, final String rename) {

		//final IFile file = (IFile)getEdnaProject().findMember(filePath);
		final File file = new File(getEdnaProject() + "/" + filePath);
	    try {
			String value = XMLUtils.getXPathValue(file, xPath);
			value        = transformName(value, rename);
			return value;
	    } catch (Exception ne) {
	    	logger.error("Cannot parse "+file, ne);
	    	return null;
	    }
	}
	
	public String getExampleValue(final String xPath, final String rename) {
		return getExampleValueFromFile(getExampleOutputLocation(), xPath, rename); 
	}

	@Override
	protected String getOperationName() {
		return plugin;
	}

	/**
	 * Use to reset editor 
	 */
	public String getDefaultSubstitution() {
		return getDefaultInputXML();
	}
	
	public boolean isUpstreamVariable(final String name) {
		
		try {
			final Properties xpathsInFile =  PropUtils.loadProperties(getOutputFile().getContents());
			if (xpathsInFile.containsKey(name)) return false; // We overwrite it here.
		} catch (Exception e) {
			logger.error("Cannot read properties file!", e);
		}
		return super.isUpstreamVariable(name);
	}	
	
	
	/**
	 * This method reads the output file and returns the 
	 * variable names defined in the output file.
	 * 
	 * You can override this method to return the variables from
	 * both upstream nodes and xpath variables.
	 * 
	 * The file is a properties file.
	 */
	public List<IVariable> getOutputVariables() {

		final List<IVariable> xpaths = getXPathVariables();
		final List<IVariable> ret;
		if (isPassInputs) {
			ret =  getInputVariables();
		} else {
			ret = new ArrayList<IVariable>(xpaths.size());
		}
		ret.addAll(xpaths);

		return ret;
	}

	/**
	 * XPaths local to this object
	 */
	public List<IVariable> getXPathVariables() {
		
		try {
			final IFile      outputProps = getOutputFile();
			final Properties outputs     = PropUtils.loadProperties(outputProps.getContents());
			final List<IVariable> ret    = new ArrayList<IVariable>(outputs.size());;

			for (Object varName : outputs.keySet()) {
				final String variableName = (String)varName;
				final String saveString   = outputs.getProperty(variableName);
				if (saveString!=null&&!"".equals(saveString)) {

					final IVariable iVariable = new XPathVariable(variableName, saveString);
					transformExampleValue(iVariable);
					ret.add(iVariable);
				} 
			}
			return ret;
		} catch (Exception ne) {
			logger.error("Cannot read output properties "+getOutputFile(), ne);
		}
		return Collections.emptyList();
	}

	public List<IVariable> getUpstreamVariables() {
		if (isPassInputs) {
			return  getInputVariables();
		} else {
			return Collections.emptyList();
		}
	}
}
