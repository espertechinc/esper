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
package com.espertech.esper.common.client.configuration;

import com.espertech.esper.common.internal.util.ConfigurationParser;
import com.espertech.esper.common.internal.util.DOMElementIterator;
import org.w3c.dom.*;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.*;

/**
 * Helper to migrate schema version 7 XML configuration to schema version 8 XML configuration.
 */
public class ConfigurationSchema7To8Upgrade {

    /**
     * Convert a schema from the input stream
     *
     * @param inputStream input stream for XML document text
     * @param name        for information purposes to name the document passed in
     * @return converted XML text
     * @throws ConfigurationException if the conversion failed
     */
    public static String upgrade(InputStream inputStream, String name) throws ConfigurationException {

        Document document = ConfigurationParser.getDocument(inputStream, name);

        try {
            upgradeInternal(document);

            String result = prettyPrint(document);
            result = result.replaceAll("esper-configuration-\\d-\\d.xsd", "esper-configuration-8.0.xsd");
            return result;
        } catch (Throwable t) {
            throw new ConfigurationException("Failed to transform document " + name + ": " + t.getMessage(), t);
        }
    }

    private static void upgradeInternal(Document document) throws ConfigurationException {
        Element top = document.getDocumentElement();
        if (!top.getNodeName().equals("esper-configuration")) {
            throw new ConfigurationException("Expected root node 'esper-configuration'");
        }
        trimWhitespace(top);

        Element common = createIfNotFound("common", top, document);
        Element compiler = createIfNotFound("compiler", top, document);
        Element runtime = createIfNotFound("runtime", top, document);

        removeNodes("revision-event-type", top);
        removeNodes("plugin-event-representation", top);
        removeNodes("plugin-event-type", top);
        removeNodes("plugin-event-type-name-resolution", top);
        removeNodes("bytecodegen", top);

        moveNodes("event-type-auto-name", top, common);
        moveNodes("event-type", top, common);
        moveNodes("variant-stream", top, common);
        moveNodes("auto-import", top, common);
        moveNodes("auto-import-annotations", top, common);
        moveNodes("method-reference", top, common);
        moveNodes("database-reference", top, common);
        moveNodes("variable", top, common);

        List<Node> views = moveNodes("plugin-view", top, compiler);
        List<Node> vdw = moveNodes("plugin-virtualdw", top, compiler);
        List<Node> aggs = moveNodes("plugin-aggregation-function", top, compiler);
        List<Node> aggsMF = moveNodes("plugin-aggregation-multifunction", top, compiler);
        moveNodes("plugin-singlerow-function", top, compiler);
        List<Node> guards = moveNodes("plugin-pattern-guard", top, compiler);
        List<Node> observers = moveNodes("plugin-pattern-observer", top, compiler);
        updateAttributeName("factory-class", "forge-class", views, vdw, aggs, aggsMF, guards, observers);

        moveNodes("plugin-loader", top, runtime);

        handleSettings(top, common, compiler, runtime);
    }

    private static void updateAttributeName(String oldName, String newName, List<Node>... nodes) {
        for (List<Node> list : nodes) {
            for (Node node : list) {
                Element element = (Element) node;
                String value = element.getAttribute(oldName);
                if (value == null) {
                    continue;
                }
                element.removeAttribute(oldName);
                element.setAttribute(newName, value);
            }
        }
    }

    private static void handleSettings(Element top, Element common, Element compiler, Element runtime) {
        Element settings = findNode("engine-settings", top);
        if (settings == null) {
            return;
        }
        settings.getParentNode().removeChild(settings);
        Element defaults = findNode("defaults", settings);
        defaults.getParentNode().removeChild(defaults);

        DOMElementIterator iterator = new DOMElementIterator(defaults.getChildNodes());
        while (iterator.hasNext()) {
            Element element = iterator.next();
            String nodeName = element.getNodeName();
            if (nodeName.equals("event-meta")) {
                moveChild(element, common);
            }
            if (nodeName.equals("view-resources")) {
                removeNodes("share-views", element);
                removeNodes("allow-multiple-expiry-policy", element);
                moveChild(element, compiler);
            }
            if (nodeName.equals("logging")) {
                element.getParentNode().removeChild(element);
                cloneMove(element, "query-plan,jdbc", common);
                cloneMove(element, "code", compiler);
                cloneMove(element, "execution-path,timer-debug,audit", runtime);
            }
            if (nodeName.equals("stream-selection") || nodeName.equals("language") || nodeName.equals("scripts")) {
                moveChild(element, compiler);
            }
            if (nodeName.equals("time-source")) {
                element.getParentNode().removeChild(element);
                cloneMove(element, "time-unit", common);
                cloneMove(element, "time-source-type", runtime);
            }
            if (nodeName.equals("expression")) {
                element.getParentNode().removeChild(element);
                Element compilerExpr = cloneMove(element, "", compiler);
                Element runtimeExpr = cloneMove(element, "", runtime);
                removeAttributes(compilerExpr, "self-subselect-preeval,time-zone");
                removeAttributesAllBut(runtimeExpr, "self-subselect-preeval,time-zone");
            }
            if (nodeName.equals("execution")) {
                element.getParentNode().removeChild(element);
                removeAttributes(element, "allow-isolated-service");
                Element commonExec = cloneMove(element, "", common);
                Element compilerExec = cloneMove(element, "", compiler);
                Element runtimeExec = cloneMove(element, "", runtime);
                removeAttributesAllBut(commonExec, "threading-profile");
                removeAttributesAllBut(compilerExec, "filter-service-max-filter-width");
                removeAttributes(runtimeExec, "filter-service-max-filter-width,threading-profile");
            }
            if (nodeName.equals("patterns") ||
                    nodeName.equals("match-recognize") ||
                    nodeName.equals("metrics-reporting") ||
                    nodeName.equals("exceptionHandling") ||
                    nodeName.equals("variables") ||
                    nodeName.equals("threading") ||
                    nodeName.equals("conditionHandling")) {

                if (nodeName.equals("threading")) {
                    renameAttribute(element, "engine-fairlock", "runtime-fairlock");
                }
                if (nodeName.equals("metrics-reporting")) {
                    renameAttribute(element, "engine-interval", "runtime-interval");
                    renameAttribute(element, "jmx-engine-metrics", "jmx-runtime-metrics");
                }
                moveChild(element, runtime);
            }
        }
    }

    private static void renameAttribute(Element element, String oldName, String newName) {
        String value = element.getAttribute(oldName);
        if (value == null || value.isEmpty()) {
            return;
        }
        element.removeAttribute(oldName);
        element.setAttribute(newName, value);
    }

    private static void removeAttributesAllBut(Element element, String allButCSV) {
        Set<String> names = toSet(allButCSV);
        NamedNodeMap attributes = element.getAttributes();
        List<String> removed = new ArrayList<>();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node node = attributes.item(i);
            if (!names.contains(node.getNodeName())) {
                removed.add(node.getNodeName());
            }
        }
        for (String remove : removed) {
            attributes.removeNamedItem(remove);
        }
    }

    private static Element cloneMove(Element cloned, String allowedCSV, Element target) {
        Element clone = (Element) cloned.cloneNode(true);
        Element appended = (Element) target.appendChild(clone);
        removeNodesBut(allowedCSV, appended);
        return clone;
    }

    private static void moveChild(Element element, Element newParent) {
        element.getParentNode().removeChild(element);
        newParent.appendChild(element);
    }

    private static void removeAttributes(Element element, String namesCSV) {
        Set<String> names = toSet(namesCSV);
        for (String name : names) {
            String value = element.getAttribute(name);
            if (value != null) {
                element.removeAttribute(name);
            }
        }
    }

    private static List<Node> moveNodes(String name, Element from, Element to) {
        NodeList nodes = from.getChildNodes();
        if (nodes == null || nodes.getLength() == 0) {
            return Collections.emptyList();
        }
        List<Node> moved = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals(name)) {
                from.removeChild(node);
                to.appendChild(node);
                moved.add(node);
            }
        }
        return moved;
    }

    private static void removeNodes(String name, Element parent) {
        NodeList nodes = parent.getChildNodes();
        if (nodes == null || nodes.getLength() == 0) {
            return;
        }
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals(name)) {
                parent.removeChild(node);
            }
        }
    }

    private static void removeNodesBut(String allowedCSV, Element parent) {
        Set<String> allowed = toSet(allowedCSV);
        NodeList nodes = parent.getChildNodes();
        if (nodes == null || nodes.getLength() == 0) {
            return;
        }
        List<Node> toRemove = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            String name = node.getNodeName();
            if (!allowed.contains(name)) {
                toRemove.add(node);
            }
        }
        for (Node node : toRemove) {
            parent.removeChild(node);
        }
    }

    private static Element createIfNotFound(String name, Element parent, Document document) throws ConfigurationException {
        Element found = findNode(name, parent);
        if (found != null) {
            return found;
        }
        Element element = document.createElement(name);
        parent.appendChild(element);
        return element;
    }

    private static Element findNode(String name, Element parent) throws ConfigurationException {
        NodeList nodes = parent.getChildNodes();
        if (nodes == null || nodes.getLength() == 0) {
            return null;
        }
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals(name)) {
                if (!(node instanceof Element)) {
                    throw new ConfigurationException("Unexpected non-element for name '" + name + "'");
                }
                return (Element) node;
            }
        }
        return null;
    }

    private static String prettyPrint(Document document) throws ConfigurationException {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            StreamResult result = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(document);
            transformer.transform(source, result);
            return result.getWriter().toString();
        } catch (Throwable t) {
            throw new ConfigurationException("Failed to pretty-print document: " + t.getMessage(), t);
        }
    }

    private static void trimWhitespace(Node node) {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); ++i) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                child.setTextContent(child.getTextContent().trim());
            }
            trimWhitespace(child);
        }
    }

    private static Set<String> toSet(String csv) {
        return new HashSet<>(Arrays.asList(csv.split(",")));
    }
}
