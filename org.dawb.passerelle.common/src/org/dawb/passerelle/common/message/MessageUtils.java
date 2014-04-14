/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.common.message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dawb.common.util.ExpressionUtils;
import org.dawb.passerelle.common.DatasetConstants;
import org.eclipse.core.resources.IProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.kernel.util.NamedObj;
import uk.ac.diamond.scisoft.analysis.dataset.DoubleDataset;
import uk.ac.diamond.scisoft.analysis.dataset.IDataset;
import uk.ac.diamond.scisoft.analysis.fitting.functions.AFunction;
import uk.ac.diamond.scisoft.analysis.io.IMetaData;
import uk.ac.diamond.scisoft.analysis.io.MetaDataAdapter;
import uk.ac.diamond.scisoft.analysis.message.DataMessageComponent;
import uk.ac.diamond.scisoft.analysis.message.DataMessageComponent.VALUE_TYPE;
import uk.ac.diamond.scisoft.analysis.roi.IROI;

import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.MessageException;
import com.isencia.passerelle.message.MessageFactory;
import com.isencia.passerelle.message.internal.ErrorMessageContainer;
import com.isencia.passerelle.resources.util.ResourceUtils;
import com.isencia.util.ArrayUtil;
/**
 * Class to encapsulate messages and data methods sent around the network.
 * 
 * @author gerring
 *
 */
public class MessageUtils {

	private static Logger logger = LoggerFactory.getLogger(MessageUtils.class);
	
	/**
	 * Extracts or creates a DataMessageComponent from a message
	 * @param message
	 * @return
	 * @throws Exception
	 */
	public static DataMessageComponent coerceMessage(final ManagedMessage message) throws Exception {
		
		if (message instanceof ErrorMessageContainer) {
			final ErrorMessageContainer cont = (ErrorMessageContainer)message;
			final Object ob = cont.getBodyContent();
			if (ob instanceof DataMessageException) {
				return ((DataMessageException)ob).getDataMessageComponent();
			}
		}
		
		final Object data = message.getBodyContent();
		
		// TODO Add more data types allowed to process this body
		if (data instanceof DataMessageComponent) {
			return (DataMessageComponent)data;
			
		} else if (message.getBodyContent() instanceof String) {
			final DataMessageComponent comp = new DataMessageComponent();
			comp.putScalar("message_text", (String)data);
            return comp;
		
		} else {
			final Map<String, Serializable>  hash = MessageUtils.coerceData(message);
			final IMetaData            meta = MessageUtils.coerceMeta(message);
			final DataMessageComponent comp = new DataMessageComponent();
			comp.setList(hash);
			comp.setMeta(meta);
			return comp;
		}
	}

	public static boolean isErrorMessage(ManagedMessage message) {
		return message instanceof ErrorMessageContainer;
	}

	public static boolean isErrorMessage(List<DataMessageComponent> data) {
		if (data==null || data.isEmpty()) return false;
		for (DataMessageComponent dataMessageComponent : data) {
			if (dataMessageComponent.isError()) return true;
		}
		return false;
	}

	/**
	 * Attempts to get the IMetaData from the message if it exists.
	 * 
	 * Otherwise will return an IMetaData based on the message headers.
	 * 
	 * @param message
	 * @return
	 */
	public static IMetaData coerceMeta(final ManagedMessage message) throws Exception {
		
		final Object data = message.getBodyContent();
		
		if (data instanceof DataMessageComponent) {
			return ((DataMessageComponent)data).getMeta();
		} else {
			return new MetaDataAdapter() {
				private static final long serialVersionUID = MetaDataAdapter.serialVersionUID;

				@SuppressWarnings("unchecked")
				@Override
				public Collection<String> getDataNames() {
					try {
						return Collections.unmodifiableCollection((Collection<String>) message.getAllBodyHeaders());
					} catch (MessageException e) {
						logger.error("Cannot get data names from ManagedMessage",e);
						return null;
					}
				}
				
				@Override
				public String getMetaValue(String key) {
					try {
					    return message.getBodyHeader(key)[0];	
					} catch (MessageException e) {
						logger.error("Cannot get data names from ManagedMessage",e);
						return null;
					}
				}
			};
		}
	}
	
	/**
	 * Method used to get data into map of data sets.
	 * 
	 * Can check ManagedMessage for various content and return it has maps of Datasets or primitive arrays
	 * 
	 * @param message
	 * @return
	 * @throws Exception
	 */
	public static Map<String, Serializable> coerceData(final ManagedMessage message) throws Exception {
		
		final Object data = message.getBodyContent();
		
		// TODO Add more data types allowed to process this body
		if (data instanceof DataMessageComponent) {
			return ((DataMessageComponent)data).getList();
			
		} else if (data instanceof Map) {
			return (Map<String,Serializable>)data;

		} else if (data instanceof IDataset) {
			final Map<String, Serializable> d = new HashMap<String,Serializable>(1);
			final IDataset set = (IDataset)data;
			d.put(set.getName()!=null?set.getName():"sum", set);
			return d;
		
		} else if (data.getClass().isArray()) {
			
			String[] strDims = message.getBodyHeader("shape");
			final int[]    dims;
			if (strDims == null) {
			    dims    = new int[]{1};
			} else {
			    dims    = new int[strDims.length];
				for (int i = 0; i < strDims.length; i++) dims[i] = Integer.parseInt(strDims[i]);
			}
			final IDataset set;
			// We can deal with doubles or Numbers
			if (data instanceof double[]) {
				set = new  DoubleDataset((double[])data, dims);
			} else if (data instanceof Double[]) {
				final Double[] dD = (Double[])data;
				final double[] dd = new double[dD.length];
				for (int i = 0; i < dd.length; i++) dd[i] = dD[i].doubleValue();
				set = new  DoubleDataset(dd, dims);
			} else if (data instanceof Number[]) {
				final Number[] nD = (Number[])data;
				final double[] dd = new double[nD.length];
				for (int i = 0; i < dd.length; i++) dd[i] = nD[i].doubleValue();
				set = new  DoubleDataset(dd, dims);
			} else {
				throw new Exception("Cannot process data of type "+data.getClass());
			}
			
			String name = message.getBodyHeader("name")[0];
			if (name==null) name = "data";
			
			final Map<String,Serializable> ret = new HashMap<String,Serializable>(1);
			ret.put(name, set);
			return ret;
		}
		
		throw new Exception("Cannot process data of type "+data.getClass());
	}
	
	
	/**
	 * List of all the data sets contained in the DataMessageComponent(s)
	 * 
	 * Does not try and make data sets out of primitive arrays
	 * 
	 * @param data
	 * @return
	 */
	public static List<IDataset> getDatasets(List<DataMessageComponent> data) {
		List<IDataset> ret = null;
		for (DataMessageComponent comp : data) {
			if (comp==null||comp.getList()==null) continue;
			if (ret==null) ret = new ArrayList<IDataset>(7);
			final List<IDataset> sets = getDatasets(comp);
			if (sets==null) continue;
			ret.addAll(sets);
		}
		return ret;
	}
	
	/**
	 * List of all the data sets contained in the DataMessageComponent(s)
	 * 
	 * Does not try and make data sets out of primitive arrays
	 * 
	 * @param data
	 * @return
	 */
	public static Map<String, Serializable> getRois(List<DataMessageComponent> data) {
		Map<String, Serializable> ret = null;
		for (DataMessageComponent comp : data) {
			if (comp==null||comp.getRois()==null) continue;
			if (ret==null) ret = new HashMap<String, Serializable>(7);
			final  Map<String, Serializable> sets = getRois(comp);
			if (sets==null) continue;
			ret.putAll(sets);
		}
		return ret;
	}
	
	/**
	 * 
	 * @param comp
	 * @return
	 */
	public static Map<String, Serializable> getRois(DataMessageComponent comp) {
		Map<String, Serializable> ret = null;
		if (comp==null||comp.getRois()==null) return null;
		if (ret==null) ret = new HashMap<String, Serializable>(7);
		final Collection<String> keys = comp.getRois().keySet();
		for (String key : keys) {
			Object object = comp.getRois().get(key);
			if (object instanceof IROI) ret.put(key, (Serializable)object);
		}
		return ret;
	}
	
	/**
	 * List of all the data sets contained in the DataMessageComponent(s)
	 * 
	 * Does not try and make data sets out of primitive arrays
	 * 
	 * @param data
	 * @return
	 */
	public static List<IDataset> getDatasets(DataMessageComponent comp) {
		List<IDataset> ret = null;
		if (comp==null||comp.getList()==null) return null;
		if (ret==null) ret = new ArrayList<IDataset>(7);
		final Collection<Serializable> values = comp.getList().values();
		for (Object object : values) {
			if (object instanceof IDataset) ret.add((IDataset)object);
		}
		return ret;
	}


    /**
     * Attempts to determine meta data to be passed on
     * from range of inputs. If there is only one meta 
     * in the cache then that is returned otherwise it
     * will return null.
     * 
     * @param cache
     * @return
     */
	public static IMetaData getMeta(List<DataMessageComponent> cache) {
		List<IMetaData> ret = null;
		for (DataMessageComponent comp : cache) {
			if (comp==null||comp.getMeta()==null) continue;
			if (ret==null) ret = new ArrayList<IMetaData>(3);
			ret.add(comp.getMeta());
		}
		if (ret==null) return null;
		if (ret.size()==1) return ret.get(0);
		return null;
	}


	/**
	 * 
	 * @param cache
	 * @return
	 */
	public static Map<String, String> getScalar(List<DataMessageComponent> cache) {
		Map<String, String> ret = null;
		for (DataMessageComponent comp : cache) {
			if (comp==null||comp.getScalar()==null) continue;
			if (ret==null) ret = new HashMap<String,String>(7);
			final Map<String,String> scalar = comp.getScalar();
			if (scalar!=null) ret.putAll(scalar);
		}
		return ret;
	}
	

	/**
	 * 
	 * @param cache
	 * @return
	 */
	public static Map<String, Serializable> getList(List<DataMessageComponent> cache) {
		Map<String, Serializable> ret = null;
		for (DataMessageComponent comp : cache) {
			if (comp==null||comp.getList()==null) continue;
			if (ret==null) ret = new HashMap<String,Serializable>(7);
			final Map<String,Serializable> list = comp.getList();
			if (list!=null) ret.putAll(list);
		}
		return ret;
	}


	/**
	 * 
	 * @param despatch
	 * @return
	 * @throws Exception
	 */
	public static ManagedMessage getDataMessage(final DataMessageComponent despatch, final ManagedMessage originalMessage) throws Exception {
        if (despatch==null) 
        	throw new NullPointerException("Result DataMessageComponent must not be null");
        
        if(originalMessage!=null) {
			final ManagedMessage outputMsg = MessageFactory.getInstance().createCausedCopyMessage(originalMessage);
			outputMsg.setBodyContent(despatch, DatasetConstants.CONTENT_TYPE_DATA);
			return outputMsg;
        } else {
        	// TODO for source actors itś better to use the Actor.createMessage methods 
        	// as they also add actor-specific system headers in the message.
			final ManagedMessage message = MessageFactory.getInstance().createMessage();
	        message.setBodyContent(despatch, DatasetConstants.CONTENT_TYPE_DATA);
	        return message;
        }
	}

	public static String getNames(Collection<? extends Serializable> sets) {
		if (sets==null||sets.isEmpty()) return null;
	    return getNames(sets.toArray(new IDataset[sets.size()]));
	}
    /**
     * 
     * @param sets
     * @return
     */
	public static String getNames(IDataset... sets) {
		
		if (sets==null||sets.length<1) return null;
		final StringBuilder buf = new StringBuilder();
		for (int i = 0; i < sets.length; i++) {
		
			IDataset iDataset = sets[i];
			buf.append(iDataset.getName());
			if (i < sets.length-1) buf.append(", ");
		}
		return buf.toString();
	}
	
	public static Map<String, String> getValues(final DataMessageComponent comp,
                                                final NamedObj             actor) throws Exception {
		
		return MessageUtils.getValues(comp, null, actor);
	}

	/**
	 * Returns a map of the values of vars contained in DataMessageComponent
	 * @param comp may be null
	 * @param vars may be null in which case all are sent.
	 * @return
	 * @throws Exception 
	 */
	public static Map<String, String> getValues(final DataMessageComponent comp,
			                                          Collection<String>   vars,
			                                    final NamedObj             actor) throws Exception {
		
		final Map<String,String> ret = new HashMap<String,String>(7);
		
		if (vars==null) {
			if (comp!=null) {
				vars = new HashSet<String>(comp.getList().keySet());
				vars.addAll(comp.getScalar().keySet());
			} else {
				vars = Collections.emptySet();
			}
		}
		
		if (comp!=null) for (Object seq : vars) {
			final String name = seq.toString();
			
			String value = comp.getScalar(name);
			
			if (value==null) {
				final Object o = comp.getList(name);
				if (o instanceof IDataset) {
					IDataset iDataset = (IDataset) o;
					if (iDataset.getSize() == 1) {
						Double data = iDataset.getDouble(0);
						if (data != null) {
							value = data.toString();
						}
					}
				} else {
					if (o!=null && o.getClass().isArray()) value = ArrayUtil.toString(o);
				}
				if (value==null&&o!=null) value = o.toString();
			}

			if (value!=null) ret.put(name, value);
		}
		
		if (actor!=null) {
			IProject project = ResourceUtils.getProject(actor);
			if (project != null) {
				ret.put("project_name", project.getName());
			}
			ret.put("actor_name",   actor.getName());
		}
		
		return ret;
	}

	public static Map<String, String> getValues(final List<DataMessageComponent> cache,
			                                    final NamedObj                   actor) throws Exception {

		return MessageUtils.getValues(cache, null, actor);
	}

	/**
	 * 
	 * @param cache
	 * @param keySet
	 * @return
	 * @throws Exception 
	 */
	public static Map<String, String> getValues(final List<DataMessageComponent> cache, 
			                                    final Collection                 vars,
			                                    final NamedObj                   actor) throws Exception {
		
		if (cache==null) return null;
        final Map<String,String> ret = new HashMap<String,String>(cache.size()*7);
		for (DataMessageComponent comp : cache) {
			
			final Map<String, String> values = MessageUtils.getValues(comp, vars, null);
			for (String key : values.keySet()) {
				if (!ret.containsKey(key)) {
					ret.put(key, values.get(key));
				} else {
					final VALUE_TYPE type = comp.getValueType(key);
					if (type==null||type==VALUE_TYPE.OVERWRITE_STRING) {
						ret.put(key, values.get(key));
					} else if (type==VALUE_TYPE.ADDITIVE_STRING) {
						ret.put(key, ret.get(key)+"\n"+values.get(key));
					}
				}
			}
		}
		
		if (actor!=null) {		
			if (ResourceUtils.getProject(actor) != null) {
				ret.put("project_name", ResourceUtils.getProject(actor).getName());
			}
			ret.put("actor_name",   actor.getName());
		}
		
		return ret;
	}


	public static DataMessageComponent copy(List<DataMessageComponent> cache) {
		final DataMessageComponent ret = new DataMessageComponent();
		for (DataMessageComponent a : cache) {
			ret.add(a);
		}
		return ret;
	}

	public static boolean isScalarOnly(Collection<DataMessageComponent> cache) {
		for (DataMessageComponent a : cache) {
			if (a.isScalarOnly()) return true;
		}
		return false;
	}

	public static List<DataMessageComponent> mergeScalar(List<DataMessageComponent> cache) {
		
		final List<DataMessageComponent> lists   = new ArrayList<DataMessageComponent>(3);
		final List<DataMessageComponent> scalars = new ArrayList<DataMessageComponent>(3);
		for (DataMessageComponent a : cache) {
			if (a.isScalarOnly()) {
				scalars.add(a);
			} else {
				lists.add(a);
			}
		}
		
		if (!lists.isEmpty() && !scalars.isEmpty()) {
			final DataMessageComponent first = lists.get(0);
			for (DataMessageComponent s : scalars) {
				first.addScalar(s.getScalar());
			}
			return lists;
		}
		
		return cache;
	}

	/**
	 * Merge 1 or more list of DataMessageComponent's into 1 single DataMessageComponent
	 * @param cache
	 * @return
	 */
	public static DataMessageComponent mergeAll(Collection<DataMessageComponent>... cache) {
		final DataMessageComponent ret = new DataMessageComponent();
		for (Collection<DataMessageComponent> c : cache) {
			for (DataMessageComponent a : c) {
			    ret.add(a);
			}
		}
		return ret;
	}
	
	public static DataMessageComponent copy(DataMessageComponent dmc) {
		DataMessageComponent ret = new DataMessageComponent();
		ret.add(dmc);
		return ret;
	}

	/**
	 * Return true if content is DataMessageComponent
	 * @param message
	 * @return
	 * @throws MessageException 
	 */
	public static boolean isDataMessage(ManagedMessage message) {
		try {
			final String contentType = message.getBodyContentType();
		    if (DatasetConstants.CONTENT_TYPE_DATA.equals(contentType))   return true;
		    if (message.getBodyContent() instanceof DataMessageComponent) return true;
		    return false;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * JEP evaluation based on scalar values.
	 * @param expression
	 * @param message
	 * @return
	 * @throws MessageException
	 */
	public static boolean isExpressionTrue(String expression, ManagedMessage message) throws Exception {
		
		DataMessageComponent comp = (DataMessageComponent)message.getBodyContent();
		return isExpressionTrue(expression, comp);
	}

	/**
	 * Expression evaluation based on scalar values.
	 * @param expression
	 * @param comp
	 * @return
	 * @throws MessageException
	 */
	public static boolean isExpressionTrue(String expression, DataMessageComponent comp) throws Exception {
		
		final double val = evaluateExpression(expression, comp);
		boolean ret =  val!=0d && !Double.isNaN(val) && !Double.isInfinite(val);
		return ret;
	}
	
	/**
	 * Expression evaluation based on scalar values.
	 * @param expression
	 * @param comp
	 * @return double value of evaluation
	 * @throws MessageException
	 */
	public static double evaluateExpression(String expression, DataMessageComponent comp) throws Exception {
		
        return ExpressionUtils.evaluateExpression(expression, comp.getScalar());
	}

	public static Map<String, IROI> getROIs(final List<DataMessageComponent> cache) throws Exception {
		Map<String, IROI> rois = new LinkedHashMap<String,IROI>(1);
		for (DataMessageComponent message : cache) {
			Map<String, Serializable> map = message.getROIs();
			if(map!=null){
				for (String key : map.keySet()) {
					if (map.get(key) instanceof IROI) {
						rois.put(key, (IROI)map.get(key));
					}
				}
			}
		}
		return rois;
	}
	
	
	public static Map<String, AFunction> getFunctions(final List<DataMessageComponent> cache) throws Exception {
		Map<String, AFunction> functions = new LinkedHashMap<String,AFunction>(1);
		for (DataMessageComponent message : cache) {
			Map<String, Serializable> map = message.getFunctions();
			if(map!=null){
				for (String key : map.keySet()) {
					if (map.get(key) instanceof AFunction) {
						functions.put(key, (AFunction) map.get(key));
					}
				}
			}
		}
		return functions;
	}
	
}
	
