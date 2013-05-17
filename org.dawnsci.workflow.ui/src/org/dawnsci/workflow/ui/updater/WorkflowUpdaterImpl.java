package org.dawnsci.workflow.ui.updater;

import java.io.File;
import java.io.IOException;

import org.dawb.passerelle.actors.roi.ROISource;
import org.dawb.passerelle.actors.ui.config.FieldBean;
import org.dawb.passerelle.actors.ui.config.FieldContainer;
import org.dawb.passerelle.actors.ui.config.FieldParameter;
import org.dawb.passerelle.common.parameter.function.FunctionParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.ComponentEntity;
import ptolemy.kernel.util.Attribute;
import uk.ac.diamond.scisoft.analysis.fitting.functions.AFunction;
import uk.ac.diamond.scisoft.analysis.roi.IROI;

import com.isencia.passerelle.model.Flow;
import com.isencia.passerelle.model.FlowManager;
import com.isencia.passerelle.util.ptolemy.ResourceParameter;

/**
 * Class to interact(update, read) with a model file
 * TODO: make this class more generic
 */
public class WorkflowUpdaterImpl implements IWorkflowUpdater{

	private static Logger logger = LoggerFactory.getLogger(WorkflowUpdaterImpl.class);
	private String dataFilePath;
	private String modelFilePath;
	private String goldFilePath = "";

	public WorkflowUpdaterImpl(String dataFilePath, String modelFilePath) {
		this.dataFilePath = dataFilePath;
		this.modelFilePath = modelFilePath;
	}

	public void setGoldFilePath(String goldFilePath) {
		this.goldFilePath = goldFilePath;
	}
	
	public void createFile(String filename, String path) throws IOException {
		File f;
		f = new File(path+filename);
		if (!f.exists()) {
			f.createNewFile();
			logger.debug("New file "+filename+" has been created to the following directory:"+path);
		}
	}

	/**
	 * Initialize a model file
	 */
	@Override
	public void initialize(){
		File modelFile = new File(modelFilePath);

		try {

			Flow flow = FlowManager.readMoml(modelFile.toURI().toURL());
			ComponentEntity dataEntity = flow.getEntity("Data");
			ComponentEntity axesEntity = flow.getEntity("Axes");
			ComponentEntity inputEntity = flow.getEntity("Input");
			ComponentEntity goldEntity = flow.getEntity("Gold");
			
			// for dataEntity
			if (dataEntity != null) {
				// data path parameter
				Attribute pathDataAttr = dataEntity.getAttribute("Path");
				if (pathDataAttr instanceof ResourceParameter)
					((ResourceParameter) pathDataAttr).setExpression(dataFilePath);
				pathDataAttr.setContainer(dataEntity);

				// data set parameter
				// TODO not sure if this is required here. (MB)
				//Attribute dataAttr = dataEntity.getAttribute("Data Sets");
				//if (dataAttr instanceof StringChoiceParameter)
				//	((StringChoiceParameter) dataAttr).setExpression("/entry1/analyser/data");
				//dataAttr.setContainer(dataEntity);
				
				dataEntity.setContainer(flow);
			};

			if (goldEntity != null) {
				// data path parameter
				Attribute pathDataAttr = goldEntity.getAttribute("Path");
				if (pathDataAttr instanceof ResourceParameter)
					((ResourceParameter) pathDataAttr).setExpression(goldFilePath);
				pathDataAttr.setContainer(goldEntity);

				// data set parameter
				// TODO not sure if this is required here. (MB)
				//Attribute dataAttr = dataEntity.getAttribute("Data Sets");
				//if (dataAttr instanceof StringChoiceParameter)
				//	((StringChoiceParameter) dataAttr).setExpression("/entry1/analyser/data");
				//dataAttr.setContainer(dataEntity);
				
				goldEntity.setContainer(flow);
			};
			
			// for AxisEntity
			if (axesEntity != null) {
				// data path parameter
				Attribute pathAxesAttr = axesEntity.getAttribute("Path");
				if (pathAxesAttr instanceof ResourceParameter)
					((ResourceParameter) pathAxesAttr).setExpression(dataFilePath);
				pathAxesAttr.setContainer(axesEntity);
	
				// xaxis/yaxis data set parameter
				//TODO again not sure this should be here (MB)
				//Attribute xyaxisAttr = axesEntity.getAttribute("Data Sets");
				//if (xyaxisAttr instanceof StringChoiceParameter)
				//	((StringChoiceParameter) xyaxisAttr).setExpression("/entry1/analyser/energies, /entry1/analyser/angles");
				//xyaxisAttr.setContainer(axesEntity);
				
				axesEntity.setContainer(flow);
			};

			// for InputEntity
			if (inputEntity != null) {
				// input filename field parameter(for saving) 
				Attribute fileNameAttr = inputEntity.getAttribute("User Fields");
				FieldBean filenameBean = null;
				if (fileNameAttr instanceof FieldParameter){
					FieldContainer fields = (FieldContainer) ((FieldParameter) fileNameAttr).getBeanFromValue(FieldContainer.class);
					if (fields.containsBean("fileName")) {
						filenameBean = fields.getBean("fileName");
						filenameBean.setDefaultValue((String)dataFilePath);
						FieldParameter fp = ((FieldParameter)fileNameAttr);
						fp.setExpression(fp.getValueFromBean(fields));
					}
				}
				fileNameAttr.setContainer(inputEntity);
				
				inputEntity.setContainer(flow);
			};

			FlowManager.save(flow, modelFile.toURI().toURL());

			logger.debug("Model file updated");

		} catch (Exception e) {
			logger.error("Error copying MOML file:"+e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Method that updates a region actor
	 * @param actorName the unique actor's name
	 * @param roi the region of interest
	 */
	@Override
	public void updateRegionActor(String actorName, IROI roi){
		File modelFile = new File(modelFilePath);

		try {
			Flow flow = FlowManager.readMoml(modelFile.toURI().toURL());
			ComponentEntity regionEntity = flow.getEntity(actorName);
			// region
			if (regionEntity instanceof ROISource){
				ROISource roiSrc = (ROISource)regionEntity;
				roiSrc.roiParam.setRoi(roi);
				regionEntity = roiSrc;
			}
				
			regionEntity.setContainer(flow);
			
			FlowManager.save(flow, modelFile.toURI().toURL());
			logger.debug("Model file updated");
		} catch (Exception e) {
			logger.error("Region update error:"+e);
		}
	}

	/**
	 * Method that updates a scalar actor
	 * @param actorName the unique actor's name
	 * @param scalar value parameter
	 */
	@Override
	public void updateScalarActor(String actorName, double scalarValue){
		File modelFile = new File(modelFilePath);

		try {
			Flow flow = FlowManager.readMoml(modelFile.toURI().toURL());
			ComponentEntity scalarEntity = flow.getEntity(actorName);
			Attribute scalarValueAttr = scalarEntity.getAttribute("Value");

			//scalar value
			if(scalarValueAttr instanceof Parameter){
				((Parameter) scalarValueAttr).setExpression(String.valueOf(scalarValue));
				scalarValueAttr.setContainer(scalarEntity);
			}
			scalarEntity.setContainer(flow);

			FlowManager.save(flow, modelFile.toURI().toURL());
			logger.debug("Model file updated");
		} catch (Exception e) {
			logger.error("Scalar update error:"+e);
		}
	}

	/**
	 * Method that updates a function actor
	 * @param actorName the unique actor's name
	 * @param function parameter
	 */
	@Override
	public void updateFunctionActor(String actorName, AFunction function){
		File modelFile = new File(modelFilePath);

		try {
			Flow flow = FlowManager.readMoml(modelFile.toURI().toURL());
			ComponentEntity functionEntity = flow.getEntity(actorName);
			Attribute functionValueAttr = functionEntity.getAttribute("Function");

			//function value
			if(functionValueAttr instanceof FunctionParameter){
				((FunctionParameter) functionValueAttr).setFunction(function);
				functionValueAttr.setContainer(functionEntity);
			}
			functionEntity.setContainer(flow);

			FlowManager.save(flow, modelFile.toURI().toURL());
			logger.debug("Model file updated");
		} catch (Exception e) {
			logger.error("Function update error:"+e);
		}
	}

	/**
	 * Method that reads a scalar actor's parameter
	 * @param actorName the unique actor's name
	 * @param scalar name parameter
	 * @return the parameter value
	 */
	@Override
	public String getActorParam(String actorName, String paramName){
		String paramValue = null;
		File modelFile = new File(modelFilePath);

		try {
			Flow flow = FlowManager.readMoml(modelFile.toURI().toURL());
			ComponentEntity scalarEntity = flow.getEntity(actorName);
			Attribute scalarAttr = scalarEntity.getAttribute(paramName);

			if (scalarAttr instanceof Parameter){
				Parameter name = (Parameter)scalarAttr;
				paramValue = name.getExpression();
			}

			logger.debug("Model file read");
		} catch (Exception e) {
			logger.error("Model file reading error:"+e);
			return null;
		}
		return paramValue;
	}

	/**
	 * Method that reads a function actor
	 * @param actorName the unique actor's name
	 * @return function
	 */
	@Override
	public AFunction getFunctionFromActor(String actorName){
		AFunction function = null;
		File modelFile = new File(modelFilePath);

		try {
			Flow flow = FlowManager.readMoml(modelFile.toURI().toURL());
			ComponentEntity functionEntity = flow.getEntity(actorName);
			Attribute functionValueAttr = functionEntity.getAttribute("Function");

			//function value
			if(functionValueAttr instanceof FunctionParameter){
				FunctionParameter param = (FunctionParameter)functionValueAttr;
				function = param.getFunction();
			}
			logger.debug("Model file read");
		} catch (Exception e) {
			logger.error("Function reader error:"+e);
			return null;
		}
		return function;
	}

	/**
	 * Method that updates a parameter with a value
	 * @param actorName the unique actor's name
	 * @param paramName the parameter name
	 * @param paramValue the parameter value
	 */
	@Override
	public void updateActorParam(String actorName, String paramName, String paramValue) {
		File modelFile = new File(modelFilePath);

		try {
			Flow flow = FlowManager.readMoml(modelFile.toURI().toURL());
			ComponentEntity functionEntity = flow.getEntity(actorName);
			Attribute valueAttr = functionEntity.getAttribute(paramName);

			//a string value value
			if(valueAttr instanceof StringParameter){
				((StringParameter) valueAttr).setExpression(paramValue);
				valueAttr.setContainer(functionEntity);
			}
			functionEntity.setContainer(flow);

			FlowManager.save(flow, modelFile.toURI().toURL());
			logger.debug("Model file updated");
		} catch (Exception e) {
			logger.error("Function update error:"+e);
		}
	}
}
