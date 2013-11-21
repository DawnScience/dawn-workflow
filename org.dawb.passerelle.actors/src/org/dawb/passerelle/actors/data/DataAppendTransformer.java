package org.dawb.passerelle.actors.data;

import java.io.File;
import java.io.Serializable;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.dawb.common.util.io.FileUtils;
import org.dawb.passerelle.common.actors.AbstractDataMessageTransformer;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.IVariable;
import org.dawb.passerelle.common.message.MessageUtils;
import org.eclipse.swt.SWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.data.BooleanToken;
import ptolemy.data.expr.Parameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import uk.ac.diamond.scisoft.analysis.io.LoaderFactory;

import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.util.EnvironmentUtils;
import com.isencia.passerelle.util.ptolemy.IAvailableChoices;
import com.isencia.passerelle.util.ptolemy.IAvailableMap;
import com.isencia.passerelle.util.ptolemy.ResourceParameter;
import com.isencia.passerelle.util.ptolemy.StringChoiceParameter;
import com.isencia.passerelle.util.ptolemy.StringMapParameter;
import com.isencia.util.StringConvertor;

/**
 * Similar to a Data Import Source this adds data to the message without
 * generating new messages.
 * 
 * @author fcp94556
 *
 */
public class DataAppendTransformer extends AbstractDataMessageTransformer {
	
	private static final Logger logger = LoggerFactory.getLogger(DataAppendTransformer.class);

	private final ResourceParameter       path;
	private final StringChoiceParameter   names;
	private final StringMapParameter      rename;
	private final Parameter               relativePathParam;
	private final DataImportDelegate      delegate;
	/**
	 * 
	 */
	private static final long serialVersionUID = 5293756233333119335L;

	public DataAppendTransformer(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		
		super(container, name);
		
		relativePathParam = new Parameter(this, "Relative Path", new BooleanToken(true));
		registerConfigurableParameter(relativePathParam);

		path = new ResourceParameter(this, "Path", "Data files", LoaderFactory.getSupportedExtensions().toArray(new String[0]));
		try {
			URI baseURI = new File(StringConvertor.convertPathDelimiters(EnvironmentUtils.getApplicationRootFolder())).toURI();
			path.setBaseDirectory(baseURI);
		} catch (Exception e) {
			logger.error("Cannot set base directory for "+getClass().getName(), e);
		}
		registerConfigurableParameter(path);

		names = new StringChoiceParameter(this, "Data Sets", new IAvailableChoices() {		
			@Override
			public String[] getChoices() {
                Collection<String> names = delegate.getAllDatasetsInFile();
                if (names==null || names.isEmpty()) return null;
                return names.toArray(new String[names.size()]);
			}
			@Override
			public Map<String,String> getVisibleChoices() {
			    return delegate.getChoppedNames();
			}
		}, SWT.MULTI);
		
		registerConfigurableParameter(names);

		rename = new StringMapParameter(this, "Rename Data Sets", new IAvailableMap() {		
			@Override
			public Map<String,String> getMap() {
				return delegate.getDataSetsRenameName();
			}
			@Override
			public Map<String,String> getVisibleKeyChoices() {
			    return delegate.getChoppedNames();
			}
			@Override
			public Collection<String> getSelectedChoices() {		
                final String[] ds     = names.getValue();
				return ds!=null ? Arrays.asList(ds) : null;
			}
		});
		
		registerConfigurableParameter(rename);
		
		delegate = new DataImportDelegate(path, names, relativePathParam, rename);
	}

	@Override
	protected DataMessageComponent getTransformedMessage(List<DataMessageComponent> cache) throws ProcessingException {
		
		try {
			final String                     filePath = delegate.getSourcePath(cache);
		    final Map<String,Serializable>   datasets = delegate.getDatasets(filePath, null);
		    final DataMessageComponent       despatch = MessageUtils.mergeAll(cache);
		    
		    if (datasets!=null) despatch.addList(datasets);
		    
			despatch.putScalar("file_path", filePath);
			despatch.putScalar("file_name", new File(filePath).getName());
			despatch.putScalar("file_dir",  FileUtils.getDirectory(filePath));

		    return despatch;
		    
		} catch (Exception ne) {
			throw createDataMessageException("Cannot read data sets from "+delegate.getSourcePath(cache), ne);
		}
	}

	@Override
	protected String getOperationName() {
		return "Data append transformer";
	}

	@Override
	public List<IVariable> getOutputVariables() {
		return delegate.getOutputVariables(true);
	}


}
