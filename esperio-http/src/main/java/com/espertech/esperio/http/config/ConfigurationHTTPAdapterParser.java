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
package com.espertech.esperio.http.config;

import com.espertech.esper.client.ConfigurationException;
import com.espertech.esper.util.DOMElementIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

public class ConfigurationHTTPAdapterParser {
    /**
     * Use the configuration specified in the given input stream.
     *
     * @param configuration is the configuration object to populate
     * @param stream        Inputstream to be read from
     * @param resourceName  The name to use in warning/error messages
     * @throws RuntimeException is thrown when the configuration could not be parsed
     */
    protected static void doConfigure(ConfigurationHTTPAdapter configuration, InputStream stream, String resourceName) throws RuntimeException {
        Document document = getDocument(stream, resourceName);
        doConfigure(configuration, document);
    }

    /**
     * Returns the document.
     *
     * @param stream       to read
     * @param resourceName resource in stream
     * @return document
     * @throws RuntimeException if the document could not be loaded or parsed
     */
    protected static Document getDocument(InputStream stream, String resourceName) throws RuntimeException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;

        Document document = null;

        try {
            builder = factory.newDocumentBuilder();
            document = builder.parse(stream);
        } catch (ParserConfigurationException ex) {
            throw new RuntimeException("Could not get a DOM parser configuration: " + resourceName, ex);
        } catch (SAXException ex) {
            throw new RuntimeException("Could not parse configuration: " + resourceName, ex);
        } catch (IOException ex) {
            throw new RuntimeException("Could not read configuration: " + resourceName, ex);
        } finally {
            try {
                stream.close();
            } catch (IOException ioe) {
                log.warn("could not close input stream for: " + resourceName, ioe);
            }
        }

        return document;
    }

    /**
     * Parse the W3C DOM document.
     *
     * @param configuration is the configuration object to populate
     * @param doc           to parse
     * @throws RuntimeException to indicate parse errors
     */
    protected static void doConfigure(ConfigurationHTTPAdapter configuration, Document doc) throws RuntimeException {
        Element root = doc.getDocumentElement();

        DOMElementIterator eventTypeNodeIterator = new DOMElementIterator(root.getChildNodes());
        while (eventTypeNodeIterator.hasNext()) {
            Element element = eventTypeNodeIterator.next();
            String nodeName = element.getNodeName();
            if (nodeName.equals("service")) {
                handleService(configuration, element);
            } else if (nodeName.equals("service")) {
                handleGet(configuration, element);
            } else if (nodeName.equals("get")) {
                handleGet(configuration, element);
            } else if (nodeName.equals("request")) {
                handleRequest(configuration, element);
            }
        }
    }

    private static void handleService(ConfigurationHTTPAdapter configuration, Node node) {
        String name = getRequiredAttribute(node, "name");
        String port = getRequiredAttribute(node, "port");
        String nio = getRequiredAttribute(node, "nio");

        Service service = new Service();
        configuration.getServices().put(name, service);
        service.setPort(Integer.parseInt(port));
        service.setNio(Boolean.parseBoolean(nio));
    }

    private static void handleGet(ConfigurationHTTPAdapter configuration, Node node) {
        String service = getRequiredAttribute(node, "service");
        String pattern = getRequiredAttribute(node, "pattern");

        GetHandler get = new GetHandler();
        configuration.getGetHandlers().add(get);
        get.setService(service);
        get.setPattern(pattern);
    }

    private static void handleRequest(ConfigurationHTTPAdapter configuration, Node node) {
        String stream = getRequiredAttribute(node, "stream");
        String uri = getRequiredAttribute(node, "uri");

        Request request = new Request();
        request.setStream(stream);
        request.setUri(uri);
        configuration.getRequests().add(request);
    }

    /**
     * Returns an input stream from an application resource in the classpath.
     *
     * @param resource to get input stream for
     * @return input stream for resource
     */
    protected static InputStream getResourceAsStream(String resource) {
        String stripped = resource.startsWith("/") ?
                resource.substring(1) : resource;

        InputStream stream = null;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            stream = classLoader.getResourceAsStream(stripped);
        }
        if (stream == null) {
            ConfigurationHTTPAdapterParser.class.getResourceAsStream(resource);
        }
        if (stream == null) {
            stream = ConfigurationHTTPAdapterParser.class.getClassLoader().getResourceAsStream(stripped);
        }
        if (stream == null) {
            throw new RuntimeException(resource + " not found");
        }
        return stream;
    }

    private static String getOptionalAttribute(Node node, String key) {
        Node valueNode = node.getAttributes().getNamedItem(key);
        if (valueNode != null) {
            return valueNode.getTextContent();
        }
        return null;
    }

    private static String getRequiredAttribute(Node node, String key) {
        Node valueNode = node.getAttributes().getNamedItem(key);
        if (valueNode == null) {
            throw new ConfigurationException("Required attribute by name '" + key + "' not found");
        }
        return valueNode.getTextContent();
    }

    private final static Logger log = LoggerFactory.getLogger(ConfigurationHTTPAdapterParser.class);
}
