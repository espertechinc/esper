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
package com.espertech.esperio.db.config;

import com.espertech.esper.client.ConfigurationDBRef;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ConfigurationDBAdapterParser {
    /**
     * Use the configuration specified in the given input stream.
     *
     * @param configuration is the configuration object to populate
     * @param stream        Inputstream to be read from
     * @param resourceName  The name to use in warning/error messages
     * @throws RuntimeException is thrown when the configuration could not be parsed
     */
    protected static void doConfigure(ConfigurationDBAdapter configuration, InputStream stream, String resourceName) throws RuntimeException {
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
    protected static void doConfigure(ConfigurationDBAdapter configuration, Document doc) throws RuntimeException {
        Element root = doc.getDocumentElement();

        DOMElementIterator eventTypeNodeIterator = new DOMElementIterator(root.getChildNodes());
        while (eventTypeNodeIterator.hasNext()) {
            Element element = eventTypeNodeIterator.next();
            String nodeName = element.getNodeName();
            if (nodeName.equals("jdbc-connection")) {
                handleConnection(configuration, element);
            } else if (nodeName.equals("dml")) {
                handleDml(configuration, element);
            } else if (nodeName.equals("upsert")) {
                handleUpsert(configuration, element);
            } else if (nodeName.equals("executors")) {
                handleExecutors(configuration, element);
            }
        }
    }

    private static void handleConnection(ConfigurationDBAdapter configuration, Node parentNode) {
        DOMElementIterator eventTypeNodeIterator = new DOMElementIterator(parentNode.getChildNodes());
        ConfigurationDBRef connection = new ConfigurationDBRef();
        String name = getRequiredAttribute(parentNode, "name");
        configuration.getJdbcConnections().put(name, connection);

        while (eventTypeNodeIterator.hasNext()) {
            Element subElement = eventTypeNodeIterator.next();

            if (subElement.getNodeName().equals("datasource-connection")) {
                String lookup = subElement.getAttributes().getNamedItem("context-lookup-name").getTextContent();
                Properties properties = handleProperties(subElement, "env-property");
                connection.setDataSourceConnection(lookup, properties);
            }
            if (subElement.getNodeName().equals("datasourcefactory-connection")) {
                String className = subElement.getAttributes().getNamedItem("class-name").getTextContent();
                Properties properties = handleProperties(subElement, "env-property");
                connection.setDataSourceFactory(properties, className);
            } else if (subElement.getNodeName().equals("drivermanager-connection")) {
                String className = subElement.getAttributes().getNamedItem("class-name").getTextContent();
                String url = subElement.getAttributes().getNamedItem("url").getTextContent();
                String userName = subElement.getAttributes().getNamedItem("user").getTextContent();
                String password = subElement.getAttributes().getNamedItem("password").getTextContent();
                Properties properties = handleProperties(subElement, "connection-arg");
                connection.setDriverManagerConnection(className, url, userName, password, properties);
            } else if (subElement.getNodeName().equals("connection-settings")) {
                if (subElement.getAttributes().getNamedItem("auto-commit") != null) {
                    String autoCommit = subElement.getAttributes().getNamedItem("auto-commit").getTextContent();
                    connection.setConnectionAutoCommit(Boolean.parseBoolean(autoCommit));
                }
                if (subElement.getAttributes().getNamedItem("transaction-isolation") != null) {
                    String transactionIsolation = subElement.getAttributes().getNamedItem("transaction-isolation").getTextContent();
                    connection.setConnectionTransactionIsolation(Integer.parseInt(transactionIsolation));
                }
                if (subElement.getAttributes().getNamedItem("catalog") != null) {
                    String catalog = subElement.getAttributes().getNamedItem("catalog").getTextContent();
                    connection.setConnectionCatalog(catalog);
                }
                if (subElement.getAttributes().getNamedItem("read-only") != null) {
                    String readOnly = subElement.getAttributes().getNamedItem("read-only").getTextContent();
                    connection.setConnectionReadOnly(Boolean.parseBoolean(readOnly));
                }
            }
        }
    }

    private static void handleDml(ConfigurationDBAdapter configuration, Node parentNode) {
        DOMElementIterator eventTypeNodeIterator = new DOMElementIterator(parentNode.getChildNodes());
        DMLQuery dmlQuery = new DMLQuery();
        String connection = getRequiredAttribute(parentNode, "connection");
        String stream = getRequiredAttribute(parentNode, "stream");
        String name = getOptionalAttribute(parentNode, "name");
        String executorName = getOptionalAttribute(parentNode, "executor-name");
        String retry = getOptionalAttribute(parentNode, "retry");
        String retryInterval = getOptionalAttribute(parentNode, "retry-interval-sec");
        List<BindingParameter> bindings = new ArrayList<BindingParameter>();

        String sql = null;
        while (eventTypeNodeIterator.hasNext()) {
            Element subElement = eventTypeNodeIterator.next();

            if (subElement.getNodeName().equals("sql")) {
                sql = subElement.getTextContent();
            } else if (subElement.getNodeName().equals("bindings")) {
                handleBindings(bindings, subElement);
            }
        }

        dmlQuery.setName(name);
        dmlQuery.setExecutorName(executorName);
        dmlQuery.setRetry(retry == null ? null : Integer.parseInt(retry));
        dmlQuery.setRetryIntervalSec(retryInterval == null ? null : Double.parseDouble(retryInterval));
        dmlQuery.setStream(stream);
        dmlQuery.setConnection(connection);
        dmlQuery.setSql(sql);
        dmlQuery.setBindings(bindings);
        if (sql == null) {
            throw new ConfigurationException("Sql is a required element");
        }
        configuration.getDmlQueries().add(dmlQuery);
    }

    private static void handleUpsert(ConfigurationDBAdapter configuration, Node parentNode) {
        DOMElementIterator eventTypeNodeIterator = new DOMElementIterator(parentNode.getChildNodes());
        UpsertQuery upsertQuery = new UpsertQuery();
        String connection = getRequiredAttribute(parentNode, "connection");
        String stream = getRequiredAttribute(parentNode, "stream");
        String name = getOptionalAttribute(parentNode, "name");
        String executorName = getOptionalAttribute(parentNode, "executor-name");
        String retry = getOptionalAttribute(parentNode, "retry");
        String retryInterval = getOptionalAttribute(parentNode, "retry-interval-sec");
        String tableName = getRequiredAttribute(parentNode, "table-name");
        List<Column> keys = new ArrayList<Column>();
        List<Column> values = new ArrayList<Column>();
        List<BindingParameter> bindings = new ArrayList<BindingParameter>();

        while (eventTypeNodeIterator.hasNext()) {
            Element subElement = eventTypeNodeIterator.next();

            if (subElement.getNodeName().equals("keys")) {
                handleColumns(keys, subElement);
            } else if (subElement.getNodeName().equals("values")) {
                handleColumns(values, subElement);
            } else if (subElement.getNodeName().equals("bindings")) {
                handleBindings(bindings, subElement);
            }
        }

        upsertQuery.setName(name);
        upsertQuery.setExecutorName(executorName);
        upsertQuery.setRetry(retry == null ? null : Integer.parseInt(retry));
        upsertQuery.setStream(stream);
        upsertQuery.setConnection(connection);
        upsertQuery.setTableName(tableName);
        upsertQuery.setKeys(keys);
        upsertQuery.setValues(values);
        upsertQuery.setRetryIntervalSec(retryInterval == null ? null : Double.parseDouble(retryInterval));
        configuration.getUpsertQueries().add(upsertQuery);
    }

    private static void handleExecutors(ConfigurationDBAdapter configuration, Node parentNode) {
        DOMElementIterator iterator = new DOMElementIterator(parentNode.getChildNodes());
        while (iterator.hasNext()) {
            Element subElement = iterator.next();
            if (subElement.getNodeName().equals("executor")) {
                handleExecutor(configuration, subElement);
            }
        }
    }

    private static void handleExecutor(ConfigurationDBAdapter configuration, Node parentNode) {
        Executor workQueue = new Executor();
        String name = getRequiredAttribute(parentNode, "name");
        String threads = getRequiredAttribute(parentNode, "threads");

        workQueue.setNumThreads(threads == null ? null : Integer.parseInt(threads));
        configuration.getExecutors().put(name, workQueue);
    }

    private static void handleBindings(List<BindingParameter> bindings, Node parentNode) {
        DOMElementIterator iterator = new DOMElementIterator(parentNode.getChildNodes());
        while (iterator.hasNext()) {
            Element subElement = iterator.next();

            if (subElement.getNodeName().equals("parameter")) {
                String position = getRequiredAttribute(subElement, "pos");
                String property = getRequiredAttribute(subElement, "property");
                bindings.add(new BindingParameter(Integer.parseInt(position), property));
            }
        }
    }

    private static void handleColumns(List<Column> columns, Node parentNode) {
        DOMElementIterator iterator = new DOMElementIterator(parentNode.getChildNodes());
        while (iterator.hasNext()) {
            Element subElement = iterator.next();

            if (subElement.getNodeName().equals("column")) {
                String property = getRequiredAttribute(subElement, "property");
                String column = getRequiredAttribute(subElement, "column");
                String type = getRequiredAttribute(subElement, "type");
                columns.add(new Column(property, column, type));
            }
        }
    }

    private static Properties handleProperties(Element element, String propElementName) {
        Properties properties = new Properties();
        DOMElementIterator nodeIterator = new DOMElementIterator(element.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals(propElementName)) {
                String name = subElement.getAttributes().getNamedItem("name").getTextContent();
                String value = subElement.getAttributes().getNamedItem("value").getTextContent();
                properties.put(name, value);
            }
        }
        return properties;
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
            ConfigurationDBAdapterParser.class.getResourceAsStream(resource);
        }
        if (stream == null) {
            stream = ConfigurationDBAdapterParser.class.getClassLoader().getResourceAsStream(stripped);
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

    private final static Logger log = LoggerFactory.getLogger(ConfigurationDBAdapterParser.class);
}
