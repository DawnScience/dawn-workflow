package org.dawnsci.workflow.ui.updater;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	private String modelFilePath;

	public WorkflowUpdaterImpl(String modelFilePath) {
		this.modelFilePath = modelFilePath;
	}

	public void createFile(String filename, String path) throws IOException {
		File f;
		f = new File(path+filename);
		if (!f.exists()) {
			f.createNewFile();
			logger.debug("New file "+filename+" has been created to the following directory:"+path);
		}
	}

	@Override
	public void updateInputActor(String actorName, String attributeName, Map<String, String> attributeValues){
		File modelFile = new File(modelFilePath);

		try {

			Flow flow = FlowManager.readMoml(modelFile.toURI().toURL());
			ComponentEntity dataEntity = flow.getEntity(actorName);
			// for dataEntity
			if (dataEntity != null) {
				// data path parameter
				// "User Fields"
				Attribute pathDataAttr = dataEntity.getAttribute(attributeName);
				if (pathDataAttr instanceof FieldParameter){
					FieldContainer fields = (FieldContainer) ((FieldParameter) pathDataAttr).getBeanFromValue(FieldContainer.class);
					List<FieldBean> fieldList = fields.getFields();
					for (FieldBean fieldBean : fieldList) {
						String name = fieldBean.getVariableName();
						if(attributeValues.containsKey(name)){
							fieldBean.setDefaultValue(attributeValues.get(name));
							FieldParameter fp = ((FieldParameter)pathDataAttr);
							fp.setExpression(fp.getValueFromBean(fields));
						}
					}
				}
				// "Path"
				if (pathDataAttr instanceof ResourceParameter){
					Set<String> names = attributeValues.keySet();
					for (String name : names) {
						((ResourceParameter) pathDataAttr).setExpression(attributeValues.get(name));
					}
				}
				
				pathDataAttr.setContainer(dataEntity);
				dataEntity.setContainer(flow);
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
