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
package com.espertech.esper.common.internal.util;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommonParser;
import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompilerParser;
import com.espertech.esper.common.client.configuration.runtime.ConfigurationRuntimeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Parser for configuration XML.
 */
public class ConfigurationParser {

    /**
     * Use the configuration specified in the given input stream.
     *
     * @param configuration is the configuration object to populate
     * @param stream        Inputstream to be read from
     * @param resourceName  The name to use in warning/error messages
     * @throws EPException is thrown when the configuration could not be parsed
     */
    public static void doConfigure(Configuration configuration, InputStream stream, String resourceName) throws EPException {
        Document document = getDocument(stream, resourceName);
        doConfigure(configuration, document);
    }

    public static Document getDocument(InputStream stream, String resourceName) throws EPException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;

        Document document;
        try {
            builder = factory.newDocumentBuilder();
            document = builder.parse(stream);
        } catch (ParserConfigurationException ex) {
            throw new EPException("Could not get a DOM parser configuration " + resourceName + ": " + ex.getMessage(), ex);
        } catch (SAXException ex) {
            throw new EPException("Could not parse configuration " + resourceName + ": " + ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new EPException("Could not read configuration " + resourceName + ": " + ex.getMessage(), ex);
        } finally {
            try {
                stream.close();
            } catch (IOException ioe) {
                ConfigurationParser.log.warn("could not close input stream for: " + resourceName, ioe);
            }
        }

        return document;
    }

    /**
     * Parse the W3C DOM document.
     *
     * @param configuration is the configuration object to populate
     * @param doc           to parse
     * @throws EPException to indicate parse errors
     */
    public static void doConfigure(Configuration configuration, Document doc) throws EPException {
        Element root = doc.getDocumentElement();

        DOMElementIterator eventTypeNodeIterator = new DOMElementIterator(root.getChildNodes());
        while (eventTypeNodeIterator.hasNext()) {
            Element element = eventTypeNodeIterator.next();
            String nodeName = element.getNodeName();
            if (nodeName.equals("common")) {
                ConfigurationCommonParser.doConfigure(configuration.getCommon(), element);
            } else if (nodeName.equals("compiler")) {
                ConfigurationCompilerParser.doConfigure(configuration.getCompiler(), element);
            } else if (nodeName.equals("runtime")) {
                ConfigurationRuntimeParser.doConfigure(configuration.getRuntime(), element);
            } else if (nodeName.equals("event-type") ||
                    nodeName.equals("auto-import") ||
                    nodeName.equals("auto-import-annotations") ||
                    nodeName.equals("method-reference") ||
                    nodeName.equals("database-reference") ||
                    nodeName.equals("plugin-view") ||
                    nodeName.equals("plugin-virtualdw") ||
                    nodeName.equals("plugin-aggregation-function") ||
                    nodeName.equals("plugin-aggregation-multifunction") ||
                    nodeName.equals("plugin-singlerow-function") ||
                    nodeName.equals("plugin-pattern-guard") ||
                    nodeName.equals("plugin-pattern-observer") ||
                    nodeName.equals("variable") ||
                    nodeName.equals("plugin-loader") ||
                    nodeName.equals("engine-settings") ||
                    nodeName.equals("variant-stream")) {
                log.warn("The configuration file appears outdated as it has element '" + nodeName + "' among top-level elements. Please convert to the newest schema using the online converter.");
            }
        }
    }

    private final static Logger log = LoggerFactory.getLogger(ConfigurationParser.class);
}
