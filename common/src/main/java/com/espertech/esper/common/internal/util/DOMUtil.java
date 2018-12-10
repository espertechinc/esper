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

import com.espertech.esper.common.client.configuration.ConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Properties;
import java.util.function.Consumer;

public class DOMUtil {
    public static void parseRequiredBoolean(Element element, String name, Consumer<Boolean> func) {
        String str = getRequiredAttribute(element, name);
        boolean b = parseBoolean(name, str);
        func.accept(b);
    }

    public static void parseOptionalBoolean(Element element, String name, Consumer<Boolean> func) {
        String str = getOptionalAttribute(element, name);
        if (str != null) {
            boolean b = parseBoolean(name, str);
            func.accept(b);
        }
    }

    public static void parseOptionalInteger(Element element, String name, Consumer<Integer> func) {
        String str = getOptionalAttribute(element, name);
        if (str != null) {
            int i = parseInteger(name, str);
            func.accept(i);
        }
    }

    public static String getRequiredAttribute(Node node, String key) throws ConfigurationException {
        Node valueNode = node.getAttributes().getNamedItem(key);
        if (valueNode == null) {
            String name = node.getLocalName();
            if (name == null) {
                name = node.getNodeName();
            }
            throw new ConfigurationException("Required attribute by name '" + key + "' not found for element '" + name + "'");
        }
        return valueNode.getTextContent();
    }

    public static String getOptionalAttribute(Node node, String key) {
        Node valueNode = node.getAttributes().getNamedItem(key);
        if (valueNode != null) {
            return valueNode.getTextContent();
        }
        return null;
    }

    public static Properties getProperties(Element element, String propElementName) {
        Properties properties = new Properties();
        DOMElementIterator nodeIterator = new DOMElementIterator(element.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals(propElementName)) {
                String name = getRequiredAttribute(subElement, "name");
                String value = getRequiredAttribute(subElement, "value");
                properties.put(name, value);
            }
        }
        return properties;
    }

    private static boolean parseBoolean(String name, String str) {
        try {
            return Boolean.parseBoolean(str);
        } catch (Throwable t) {
            throw new ConfigurationException("Failed to parse value for '" + name + "' value '" + str + "' as boolean: " + t.getMessage(), t);
        }
    }

    private static int parseInteger(String name, String str) {
        try {
            return Integer.parseInt(str);
        } catch (Throwable t) {
            throw new ConfigurationException("Failed to parse value for '" + name + "' value '" + str + "' as int: " + t.getMessage(), t);
        }
    }
}
