/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.passerelle.common.utils;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.digester.substitution.MultiVariableExpander;
import org.apache.commons.digester.substitution.VariableSubstitutor;
import org.dawb.common.util.io.Grep;
import org.eclipse.core.resources.IFile;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class XMLUtils {

	private static final String VARIABLE_EXPRESSION = "\\$\\{([a-zA-Z0-9_ ]+)\\}";

	/**
	 * Returns a list of strings surrounded by ${ } in the file.
	 * @param file
	 * @return
	 * @throws Exception 
	 */
	public static List<String> getVariables(String data) throws Exception {
		return Grep.group(data, VARIABLE_EXPRESSION, 1);
	}
	/**
	 * Returns a list of strings surrounded by ${ } in the file.
	 * @param file
	 * @return
	 * @throws Exception 
	 */
	public static List<CharSequence> getVariables(IFile file) throws Exception {
		return Grep.group(file, VARIABLE_EXPRESSION, 1);
	}

	private static final Pattern HEADER_PATTERN = Pattern.compile("^\\<\\?.+\\?\\>(.*)", Pattern.DOTALL);
	
	public static Map<String, String> getVariables(final Map<?, ?>           variables,
			                                       final String              xmlSource, 
			                                       final Map<String, String> scalarSource) throws Exception {
		
		
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		docFactory.setNamespaceAware(false); // never forget this!
		docFactory.setValidating(false);
		DocumentBuilder builder = docFactory.newDocumentBuilder();
		Document doc = null;
		
		XPathFactory factory = XPathFactory.newInstance();
		XPath        xpath   = factory.newXPath();
		
		final Map<String,String> values = new HashMap<String, String>(variables.size());
		for (Object varName : variables.keySet()) {
			final String varValue = (String)variables.get(varName.toString());
			if (varValue==null||"".equals(varValue)) {
				values.put(varName.toString(), scalarSource.get(varName));
				continue;
			}
			
			if ("/".equals(varValue)) {
				values.put(varName.toString(), xmlSource);
				continue;
			}
			
			final XPathExpression exp = xpath.compile(varValue);
			if (doc==null) doc = builder.parse(new InputSource(new StringReader(xmlSource)));
			
			final NodeList nodeList = (NodeList)exp.evaluate(doc, XPathConstants.NODESET);		
			values.put(varName.toString(), getNodeValue(nodeList));
		}
		
		// We allow names of variables to expand values of other variables.
		final Map<String,String> all = new HashMap<String,String>(values.size());
		all.putAll(scalarSource);
		all.putAll(values);
		
		final Map<String,String> ret = new HashMap<String, String>(variables.size());
		final MultiVariableExpander expander = new MultiVariableExpander( );
		expander.addSource("$", all);
		
		// Create a substitutor with the expander
		final VariableSubstitutor substitutor = new VariableSubstitutor(expander);
		
		for (final String varName : values.keySet()) {
			
			if (!varName.contains("$")) {
				ret.put(varName, values.get(varName));
			} else {
				ret.put(substitutor.substitute(varName), values.get(varName));
			}
		}
		
		return ret;
	}

	private static String getNodeValue(NodeList nodeList) throws TransformerFactoryConfigurationError, TransformerException {
		
		final Transformer serializer = TransformerFactory.newInstance().newTransformer();
		serializer.setURIResolver(null);
		
		final StringBuilder buf = new StringBuilder();
		for (int i = 0; i < nodeList.getLength(); i++) {
			final StringWriter sw = new StringWriter();
			serializer.transform(new DOMSource(nodeList.item(i)), new StreamResult(sw));
			String xml = sw.toString();
			final Matcher matcher = HEADER_PATTERN.matcher(xml);
			if (matcher.matches()) {
				xml = matcher.group(1);
			}
			buf.append(xml);
			buf.append("\n");
		}
		
		return buf.toString();
	}

	public static String getXPathValue(IFile file, String xPath) throws Exception {
		
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		docFactory.setNamespaceAware(false); // never forget this!
		docFactory.setValidating(false);
		DocumentBuilder builder = docFactory.newDocumentBuilder();
		
		XPathFactory factory = XPathFactory.newInstance();
		XPath        xpath   = factory.newXPath();
		final XPathExpression exp = xpath.compile(xPath);
		
		Document doc = builder.parse(new InputSource(file.getContents()));

		final NodeList nodeList = (NodeList)exp.evaluate(doc, XPathConstants.NODESET);
		return XMLUtils.getNodeValue(nodeList);
	}

	// Same method as above but with File instead of IFile as input type
	// TODO: Remove code duplication
	public static String getXPathValue(File file, String xPath) throws Exception {
		
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		docFactory.setNamespaceAware(false); // never forget this!
		docFactory.setValidating(false);
		DocumentBuilder builder = docFactory.newDocumentBuilder();
		
		XPathFactory factory = XPathFactory.newInstance();
		XPath        xpath   = factory.newXPath();
		final XPathExpression exp = xpath.compile(xPath);
		
		Document doc = builder.parse(new InputSource(file.getAbsolutePath()));

		final NodeList nodeList = (NodeList)exp.evaluate(doc, XPathConstants.NODESET);
		return XMLUtils.getNodeValue(nodeList);
	}
	
	/**
	 * 
	 * @param xPath
	 * @throws Exception - if path invalid.
	 */
	public static void isLegalXPath(final String xPath) throws Exception {
		XPathFactory factory = XPathFactory.newInstance();
		XPath        xpath   = factory.newXPath();
		xpath.compile(xPath);
	}

}
