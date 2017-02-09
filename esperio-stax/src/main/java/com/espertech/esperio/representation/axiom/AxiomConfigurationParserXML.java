/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esperio.representation.axiom;

import com.espertech.esper.client.ConfigurationException;
import com.espertech.esper.client.EPException;
import com.espertech.esper.util.DOMElementIterator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;

/**
 * DOM-based parser for configuration XML for the Axiom event type representation.
 */
public class AxiomConfigurationParserXML {
    /**
     * Parses the configuration XML.
     *
     * @param theString xml to parse
     * @return parsed configuration
     * @throws EPException if the parse operation failed
     */
    protected static ConfigurationEventTypeAxiom parse(String theString) throws EPException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;

        Document document;

        try {
            builder = factory.newDocumentBuilder();
            document = builder.parse(new InputSource(new StringReader(theString)));
        } catch (ParserConfigurationException ex) {
            throw new EPException("Could not get a DOM parser configuration", ex);
        } catch (SAXException ex) {
            throw new EPException("Could not parse configuration", ex);
        } catch (IOException ex) {
            throw new EPException("Could not read configuration", ex);
        }

        return parse(document);
    }

    private static ConfigurationEventTypeAxiom parse(Document doc) throws EPException {
        Element root = doc.getDocumentElement();
        String nodeName = root.getNodeName();
        if (!nodeName.equals("xml-axiom")) {
            throw new ConfigurationException("Expected root element 'xml-axiom' not found in document, found '" + nodeName + "' instead");
        }
        return handleAxiom(root);
    }

    private static ConfigurationEventTypeAxiom handleAxiom(Element xmldomElement) {
        String rootElementName = xmldomElement.getAttributes().getNamedItem("root-element-name").getTextContent();
        String rootElementNamespace = getOptionalAttribute(xmldomElement, "root-element-namespace");
        String defaultNamespace = getOptionalAttribute(xmldomElement, "default-namespace");
        String resolvePropertiesAbsoluteStr = getOptionalAttribute(xmldomElement, "resolve-properties-absolute");

        ConfigurationEventTypeAxiom xmlAxiomConfig = new ConfigurationEventTypeAxiom();
        xmlAxiomConfig.setRootElementName(rootElementName);
        xmlAxiomConfig.setRootElementNamespace(rootElementNamespace);
        xmlAxiomConfig.setDefaultNamespace(defaultNamespace);
        if (resolvePropertiesAbsoluteStr != null) {
            xmlAxiomConfig.setResolvePropertiesAbsolute(Boolean.parseBoolean(resolvePropertiesAbsoluteStr));
        }

        DOMElementIterator propertyNodeIterator = new DOMElementIterator(xmldomElement.getChildNodes());
        while (propertyNodeIterator.hasNext()) {
            Element propertyElement = propertyNodeIterator.next();
            if (propertyElement.getNodeName().equals("namespace-prefix")) {
                String prefix = propertyElement.getAttributes().getNamedItem("prefix").getTextContent();
                String namespace = propertyElement.getAttributes().getNamedItem("namespace").getTextContent();
                xmlAxiomConfig.addNamespacePrefix(prefix, namespace);
            }
            if (propertyElement.getNodeName().equals("xpath-property")) {
                String propertyName = propertyElement.getAttributes().getNamedItem("property-name").getTextContent();
                String xPath = propertyElement.getAttributes().getNamedItem("xpath").getTextContent();

                String propertyType = propertyElement.getAttributes().getNamedItem("type").getTextContent();
                QName xpathConstantType;
                if (propertyType.toUpperCase(Locale.ENGLISH).equals("NUMBER")) {
                    xpathConstantType = XPathConstants.NUMBER;
                } else if (propertyType.toUpperCase(Locale.ENGLISH).equals("STRING")) {
                    xpathConstantType = XPathConstants.STRING;
                } else if (propertyType.toUpperCase(Locale.ENGLISH).equals("BOOLEAN")) {
                    xpathConstantType = XPathConstants.BOOLEAN;
                } else {
                    throw new IllegalArgumentException("Invalid xpath property type for property '" +
                            propertyName + "' and type '" + propertyType + '\'');
                }

                String castToClass = null;
                if (propertyElement.getAttributes().getNamedItem("cast") != null) {
                    castToClass = propertyElement.getAttributes().getNamedItem("cast").getTextContent();
                }

                xmlAxiomConfig.addXPathProperty(propertyName, xPath, xpathConstantType, castToClass);
            }
        }

        return xmlAxiomConfig;
    }

    private static String getOptionalAttribute(Node node, String key) {
        Node valueNode = node.getAttributes().getNamedItem(key);
        if (valueNode != null) {
            return valueNode.getTextContent();
        }
        return null;
    }
}



