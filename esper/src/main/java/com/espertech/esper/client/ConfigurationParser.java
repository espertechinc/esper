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
package com.espertech.esper.client;

import com.espertech.esper.client.soda.StreamSelector;
import com.espertech.esper.client.util.ClassForNameProviderDefault;
import com.espertech.esper.client.util.EventUnderlyingType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.type.StringPatternSet;
import com.espertech.esper.type.StringPatternSetLike;
import com.espertech.esper.type.StringPatternSetRegex;
import com.espertech.esper.util.DOMElementIterator;
import com.espertech.esper.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.math.MathContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Parser for configuration XML.
 */
class ConfigurationParser {

    /**
     * Use the configuration specified in the given input stream.
     *
     * @param configuration is the configuration object to populate
     * @param stream        Inputstream to be read from
     * @param resourceName  The name to use in warning/error messages
     * @throws EPException is thrown when the configuration could not be parsed
     */
    protected static void doConfigure(Configuration configuration, InputStream stream, String resourceName) throws EPException {
        Document document = getDocument(stream, resourceName);
        doConfigure(configuration, document);
    }

    protected static Document getDocument(InputStream stream, String resourceName) throws EPException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;

        Document document = null;

        try {
            builder = factory.newDocumentBuilder();
            document = builder.parse(stream);
        } catch (ParserConfigurationException ex) {
            throw new EPException("Could not get a DOM parser configuration: " + resourceName, ex);
        } catch (SAXException ex) {
            throw new EPException("Could not parse configuration: " + resourceName, ex);
        } catch (IOException ex) {
            throw new EPException("Could not read configuration: " + resourceName, ex);
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
    protected static void doConfigure(Configuration configuration, Document doc) throws EPException {
        Element root = doc.getDocumentElement();

        DOMElementIterator eventTypeNodeIterator = new DOMElementIterator(root.getChildNodes());
        while (eventTypeNodeIterator.hasNext()) {
            Element element = eventTypeNodeIterator.next();
            String nodeName = element.getNodeName();
            if (nodeName.equals("event-type-auto-name")) {
                handleEventTypeAutoNames(configuration, element);
            } else if (nodeName.equals("event-type")) {
                handleEventTypes(configuration, element);
            } else if (nodeName.equals("auto-import")) {
                handleAutoImports(configuration, element);
            } else if (nodeName.equals("auto-import-annotations")) {
                handleAutoImportAnnotations(configuration, element);
            } else if (nodeName.equals("method-reference")) {
                handleMethodReference(configuration, element);
            } else if (nodeName.equals("database-reference")) {
                handleDatabaseRefs(configuration, element);
            } else if (nodeName.equals("plugin-view")) {
                handlePlugInView(configuration, element);
            } else if (nodeName.equals("plugin-virtualdw")) {
                handlePlugInVirtualDW(configuration, element);
            } else if (nodeName.equals("plugin-aggregation-function")) {
                handlePlugInAggregation(configuration, element);
            } else if (nodeName.equals("plugin-aggregation-multifunction")) {
                handlePlugInMultiFunctionAggregation(configuration, element);
            } else if (nodeName.equals("plugin-singlerow-function")) {
                handlePlugInSingleRow(configuration, element);
            } else if (nodeName.equals("plugin-pattern-guard")) {
                handlePlugInPatternGuard(configuration, element);
            } else if (nodeName.equals("plugin-pattern-observer")) {
                handlePlugInPatternObserver(configuration, element);
            } else if (nodeName.equals("variable")) {
                handleVariable(configuration, element);
            } else if (nodeName.equals("plugin-loader")) {
                handlePluginLoaders(configuration, element);
            } else if (nodeName.equals("engine-settings")) {
                handleEngineSettings(configuration, element);
            } else if (nodeName.equals("plugin-event-representation")) {
                handlePlugInEventRepresentation(configuration, element);
            } else if (nodeName.equals("plugin-event-type")) {
                handlePlugInEventType(configuration, element);
            } else if (nodeName.equals("plugin-event-type-name-resolution")) {
                handlePlugIneventTypeNameResolution(configuration, element);
            } else if (nodeName.equals("revision-event-type")) {
                handleRevisionEventType(configuration, element);
            } else if (nodeName.equals("variant-stream")) {
                handleVariantStream(configuration, element);
            }
        }
    }

    private static void handleEventTypeAutoNames(Configuration configuration, Element element) {
        String name = getRequiredAttribute(element, "package-name");
        configuration.addEventTypeAutoName(name);
    }

    private static void handleEventTypes(Configuration configuration, Element element) {
        String name = getRequiredAttribute(element, "name");
        Node classNode = element.getAttributes().getNamedItem("class");

        String optionalClassName = null;
        if (classNode != null) {
            optionalClassName = classNode.getTextContent();
            configuration.addEventType(name, optionalClassName);
        }

        handleEventTypeDef(name, optionalClassName, configuration, element);
    }

    private static void handleEventTypeDef(String name, String optionalClassName, Configuration configuration, Node parentNode) {
        DOMElementIterator eventTypeNodeIterator = new DOMElementIterator(parentNode.getChildNodes());
        while (eventTypeNodeIterator.hasNext()) {
            Element eventTypeElement = eventTypeNodeIterator.next();
            String nodeName = eventTypeElement.getNodeName();
            if (nodeName.equals("xml-dom")) {
                handleXMLDOM(name, configuration, eventTypeElement);
            } else if (nodeName.equals("java-util-map")) {
                handleMap(name, configuration, eventTypeElement);
            } else if (nodeName.equals("objectarray")) {
                handleObjectArray(name, configuration, eventTypeElement);
            } else if (nodeName.equals("legacy-type")) {
                handleLegacy(name, optionalClassName, configuration, eventTypeElement);
            } else if (nodeName.equals("avro")) {
                handleAvro(name, configuration, eventTypeElement);
            }
        }
    }

    private static void handleMap(String name, Configuration configuration, Element eventTypeElement) {
        ConfigurationEventTypeMap config;
        String startTimestampProp = getOptionalAttribute(eventTypeElement, "start-timestamp-property-name");
        String endTimestampProp = getOptionalAttribute(eventTypeElement, "end-timestamp-property-name");
        Node superTypesList = eventTypeElement.getAttributes().getNamedItem("supertype-names");
        if (superTypesList != null || startTimestampProp != null || endTimestampProp != null) {
            config = new ConfigurationEventTypeMap();
            if (superTypesList != null) {
                String value = superTypesList.getTextContent();
                String[] names = value.split(",");
                for (String superTypeName : names) {
                    config.getSuperTypes().add(superTypeName.trim());
                }
            }
            config.setEndTimestampPropertyName(endTimestampProp);
            config.setStartTimestampPropertyName(startTimestampProp);
            configuration.addMapConfiguration(name, config);
        }

        Properties propertyTypeNames = new Properties();
        NodeList propertyList = eventTypeElement.getElementsByTagName("map-property");
        for (int i = 0; i < propertyList.getLength(); i++) {
            String nameProperty = getRequiredAttribute(propertyList.item(i), "name");
            String clazz = getRequiredAttribute(propertyList.item(i), "class");
            propertyTypeNames.put(nameProperty, clazz);
        }
        configuration.addEventType(name, propertyTypeNames);
    }

    private static void handleObjectArray(String name, Configuration configuration, Element eventTypeElement) {
        ConfigurationEventTypeObjectArray config;
        String startTimestampProp = getOptionalAttribute(eventTypeElement, "start-timestamp-property-name");
        String endTimestampProp = getOptionalAttribute(eventTypeElement, "end-timestamp-property-name");
        Node superTypesList = eventTypeElement.getAttributes().getNamedItem("supertype-names");
        if (superTypesList != null || startTimestampProp != null || endTimestampProp != null) {
            config = new ConfigurationEventTypeObjectArray();
            if (superTypesList != null) {
                String value = superTypesList.getTextContent();
                String[] names = value.split(",");
                for (String superTypeName : names) {
                    config.getSuperTypes().add(superTypeName.trim());
                }
            }
            config.setEndTimestampPropertyName(endTimestampProp);
            config.setStartTimestampPropertyName(startTimestampProp);
            configuration.addObjectArrayConfiguration(name, config);
        }

        List<String> propertyNames = new ArrayList<String>();
        List<Object> propertyTypes = new ArrayList<Object>();
        NodeList propertyList = eventTypeElement.getElementsByTagName("objectarray-property");
        for (int i = 0; i < propertyList.getLength(); i++) {
            String nameProperty = getRequiredAttribute(propertyList.item(i), "name");
            String clazz = getRequiredAttribute(propertyList.item(i), "class");
            propertyNames.add(nameProperty);
            propertyTypes.add(clazz);
        }
        configuration.addEventType(name, propertyNames.toArray(new String[propertyNames.size()]), propertyTypes.toArray());
    }

    private static void handleXMLDOM(String name, Configuration configuration, Element xmldomElement) {
        String rootElementName = getRequiredAttribute(xmldomElement, "root-element-name");
        String rootElementNamespace = getOptionalAttribute(xmldomElement, "root-element-namespace");
        String schemaResource = getOptionalAttribute(xmldomElement, "schema-resource");
        String schemaText = getOptionalAttribute(xmldomElement, "schema-text");
        String defaultNamespace = getOptionalAttribute(xmldomElement, "default-namespace");
        String resolvePropertiesAbsoluteStr = getOptionalAttribute(xmldomElement, "xpath-resolve-properties-absolute");
        String propertyExprXPathStr = getOptionalAttribute(xmldomElement, "xpath-property-expr");
        String eventSenderChecksRootStr = getOptionalAttribute(xmldomElement, "event-sender-validates-root");
        String xpathFunctionResolverClass = getOptionalAttribute(xmldomElement, "xpath-function-resolver");
        String xpathVariableResolverClass = getOptionalAttribute(xmldomElement, "xpath-variable-resolver");
        String autoFragmentStr = getOptionalAttribute(xmldomElement, "auto-fragment");
        String startTimestampProperty = getOptionalAttribute(xmldomElement, "start-timestamp-property-name");
        String endTimestampProperty = getOptionalAttribute(xmldomElement, "end-timestamp-property-name");

        ConfigurationEventTypeXMLDOM xmlDOMEventTypeDesc = new ConfigurationEventTypeXMLDOM();
        xmlDOMEventTypeDesc.setRootElementName(rootElementName);
        xmlDOMEventTypeDesc.setSchemaResource(schemaResource);
        xmlDOMEventTypeDesc.setSchemaText(schemaText);
        xmlDOMEventTypeDesc.setRootElementNamespace(rootElementNamespace);
        xmlDOMEventTypeDesc.setDefaultNamespace(defaultNamespace);
        xmlDOMEventTypeDesc.setXPathFunctionResolver(xpathFunctionResolverClass);
        xmlDOMEventTypeDesc.setXPathVariableResolver(xpathVariableResolverClass);
        xmlDOMEventTypeDesc.setStartTimestampPropertyName(startTimestampProperty);
        xmlDOMEventTypeDesc.setEndTimestampPropertyName(endTimestampProperty);
        if (resolvePropertiesAbsoluteStr != null) {
            xmlDOMEventTypeDesc.setXPathResolvePropertiesAbsolute(Boolean.parseBoolean(resolvePropertiesAbsoluteStr));
        }
        if (propertyExprXPathStr != null) {
            xmlDOMEventTypeDesc.setXPathPropertyExpr(Boolean.parseBoolean(propertyExprXPathStr));
        }
        if (eventSenderChecksRootStr != null) {
            xmlDOMEventTypeDesc.setEventSenderValidatesRoot(Boolean.parseBoolean(eventSenderChecksRootStr));
        }
        if (autoFragmentStr != null) {
            xmlDOMEventTypeDesc.setAutoFragment(Boolean.parseBoolean(autoFragmentStr));
        }
        configuration.addEventType(name, xmlDOMEventTypeDesc);

        DOMElementIterator propertyNodeIterator = new DOMElementIterator(xmldomElement.getChildNodes());
        while (propertyNodeIterator.hasNext()) {
            Element propertyElement = propertyNodeIterator.next();
            if (propertyElement.getNodeName().equals("namespace-prefix")) {
                String prefix = getRequiredAttribute(propertyElement, "prefix");
                String namespace = getRequiredAttribute(propertyElement, "namespace");
                xmlDOMEventTypeDesc.addNamespacePrefix(prefix, namespace);
            }
            if (propertyElement.getNodeName().equals("xpath-property")) {
                String propertyName = getRequiredAttribute(propertyElement, "property-name");
                String xPath = getRequiredAttribute(propertyElement, "xpath");
                String propertyType = getRequiredAttribute(propertyElement, "type");
                QName xpathConstantType;
                if (propertyType.toUpperCase(Locale.ENGLISH).equals("NUMBER")) {
                    xpathConstantType = XPathConstants.NUMBER;
                } else if (propertyType.toUpperCase(Locale.ENGLISH).equals("STRING")) {
                    xpathConstantType = XPathConstants.STRING;
                } else if (propertyType.toUpperCase(Locale.ENGLISH).equals("BOOLEAN")) {
                    xpathConstantType = XPathConstants.BOOLEAN;
                } else if (propertyType.toUpperCase(Locale.ENGLISH).equals("NODE")) {
                    xpathConstantType = XPathConstants.NODE;
                } else if (propertyType.toUpperCase(Locale.ENGLISH).equals("NODESET")) {
                    xpathConstantType = XPathConstants.NODESET;
                } else {
                    throw new IllegalArgumentException("Invalid xpath property type for property '" +
                            propertyName + "' and type '" + propertyType + '\'');
                }

                String castToClass = null;
                if (propertyElement.getAttributes().getNamedItem("cast") != null) {
                    castToClass = propertyElement.getAttributes().getNamedItem("cast").getTextContent();
                }

                String optionaleventTypeName = null;
                if (propertyElement.getAttributes().getNamedItem("event-type-name") != null) {
                    optionaleventTypeName = propertyElement.getAttributes().getNamedItem("event-type-name").getTextContent();
                }

                if (optionaleventTypeName != null) {
                    xmlDOMEventTypeDesc.addXPathPropertyFragment(propertyName, xPath, xpathConstantType, optionaleventTypeName);
                } else {
                    xmlDOMEventTypeDesc.addXPathProperty(propertyName, xPath, xpathConstantType, castToClass);
                }
            }
        }
    }

    private static void handleAvro(String name, Configuration configuration, Element element) {
        String schemaText = getOptionalAttribute(element, "schema-text");

        ConfigurationEventTypeAvro avroEventTypeDesc = new ConfigurationEventTypeAvro();
        avroEventTypeDesc.setAvroSchemaText(schemaText);
        configuration.addEventTypeAvro(name, avroEventTypeDesc);

        avroEventTypeDesc.setStartTimestampPropertyName(getOptionalAttribute(element, "start-timestamp-property-name"));
        avroEventTypeDesc.setEndTimestampPropertyName(getOptionalAttribute(element, "end-timestamp-property-name"));

        String names = getOptionalAttribute(element, "supertype-names");
        if (names != null) {
            String[] split = names.split(",");
            for (int i = 0; i < split.length; i++) {
                avroEventTypeDesc.getSuperTypes().add(split[i].trim());
            }
        }
    }

    private static void handleLegacy(String name, String className, Configuration configuration, Element xmldomElement) {
        // Class name is required for legacy classes
        if (className == null) {
            throw new ConfigurationException("Required class name not supplied for legacy type definition");
        }

        String accessorStyle = getRequiredAttribute(xmldomElement, "accessor-style");
        String codeGeneration = getRequiredAttribute(xmldomElement, "code-generation");
        String propertyResolution = getRequiredAttribute(xmldomElement, "property-resolution-style");
        String factoryMethod = getOptionalAttribute(xmldomElement, "factory-method");
        String copyMethod = getOptionalAttribute(xmldomElement, "copy-method");
        String startTimestampProp = getOptionalAttribute(xmldomElement, "start-timestamp-property-name");
        String endTimestampProp = getOptionalAttribute(xmldomElement, "end-timestamp-property-name");

        ConfigurationEventTypeLegacy legacyDesc = new ConfigurationEventTypeLegacy();
        if (accessorStyle != null) {
            legacyDesc.setAccessorStyle(ConfigurationEventTypeLegacy.AccessorStyle.valueOf(accessorStyle.toUpperCase(Locale.ENGLISH)));
        }
        if (codeGeneration != null) {
            legacyDesc.setCodeGeneration(ConfigurationEventTypeLegacy.CodeGeneration.valueOf(codeGeneration.toUpperCase(Locale.ENGLISH)));
        }
        if (propertyResolution != null) {
            legacyDesc.setPropertyResolutionStyle(Configuration.PropertyResolutionStyle.valueOf(propertyResolution.toUpperCase(Locale.ENGLISH)));
        }
        legacyDesc.setFactoryMethod(factoryMethod);
        legacyDesc.setCopyMethod(copyMethod);
        legacyDesc.setStartTimestampPropertyName(startTimestampProp);
        legacyDesc.setEndTimestampPropertyName(endTimestampProp);
        configuration.addEventType(name, className, legacyDesc);

        DOMElementIterator propertyNodeIterator = new DOMElementIterator(xmldomElement.getChildNodes());
        while (propertyNodeIterator.hasNext()) {
            Element propertyElement = propertyNodeIterator.next();
            if (propertyElement.getNodeName().equals("method-property")) {
                String nameProperty = getRequiredAttribute(propertyElement, "name");
                String method = getRequiredAttribute(propertyElement, "accessor-method");
                legacyDesc.addMethodProperty(nameProperty, method);
            } else if (propertyElement.getNodeName().equals("field-property")) {
                String nameProperty = getRequiredAttribute(propertyElement, "name");
                String field = getRequiredAttribute(propertyElement, "accessor-field");
                legacyDesc.addFieldProperty(nameProperty, field);
            } else {
                throw new ConfigurationException("Invalid node " + propertyElement.getNodeName()
                        + " encountered while parsing legacy type definition");
            }
        }
    }

    private static void handleAutoImports(Configuration configuration, Element element) {
        String name = getRequiredAttribute(element, "import-name");
        configuration.addImport(name);
    }

    private static void handleAutoImportAnnotations(Configuration configuration, Element element) {
        String name = getRequiredAttribute(element, "import-name");
        configuration.addAnnotationImport(name);
    }

    private static void handleDatabaseRefs(Configuration configuration, Element element) {
        String name = getRequiredAttribute(element, "name");
        ConfigurationDBRef configDBRef = new ConfigurationDBRef();
        configuration.addDatabaseReference(name, configDBRef);

        DOMElementIterator nodeIterator = new DOMElementIterator(element.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("datasource-connection")) {
                String lookup = getRequiredAttribute(subElement, "context-lookup-name");
                Properties properties = handleProperties(subElement, "env-property");
                configDBRef.setDataSourceConnection(lookup, properties);
            }
            if (subElement.getNodeName().equals("datasourcefactory-connection")) {
                String className = getRequiredAttribute(subElement, "class-name");
                Properties properties = handleProperties(subElement, "env-property");
                configDBRef.setDataSourceFactory(properties, className);
            } else if (subElement.getNodeName().equals("drivermanager-connection")) {
                String className = getRequiredAttribute(subElement, "class-name");
                String url = getRequiredAttribute(subElement, "url");
                String userName = getRequiredAttribute(subElement, "user");
                String password = getRequiredAttribute(subElement, "password");
                Properties properties = handleProperties(subElement, "connection-arg");
                configDBRef.setDriverManagerConnection(className, url, userName, password, properties);
            } else if (subElement.getNodeName().equals("connection-lifecycle")) {
                String value = getRequiredAttribute(subElement, "value");
                configDBRef.setConnectionLifecycleEnum(ConfigurationDBRef.ConnectionLifecycleEnum.valueOf(value.toUpperCase(Locale.ENGLISH)));
            } else if (subElement.getNodeName().equals("connection-settings")) {
                if (subElement.getAttributes().getNamedItem("auto-commit") != null) {
                    String autoCommit = subElement.getAttributes().getNamedItem("auto-commit").getTextContent();
                    configDBRef.setConnectionAutoCommit(Boolean.parseBoolean(autoCommit));
                }
                if (subElement.getAttributes().getNamedItem("transaction-isolation") != null) {
                    String transactionIsolation = subElement.getAttributes().getNamedItem("transaction-isolation").getTextContent();
                    configDBRef.setConnectionTransactionIsolation(Integer.parseInt(transactionIsolation));
                }
                if (subElement.getAttributes().getNamedItem("catalog") != null) {
                    String catalog = subElement.getAttributes().getNamedItem("catalog").getTextContent();
                    configDBRef.setConnectionCatalog(catalog);
                }
                if (subElement.getAttributes().getNamedItem("read-only") != null) {
                    String readOnly = subElement.getAttributes().getNamedItem("read-only").getTextContent();
                    configDBRef.setConnectionReadOnly(Boolean.parseBoolean(readOnly));
                }
            } else if (subElement.getNodeName().equals("column-change-case")) {
                String value = getRequiredAttribute(subElement, "value");
                ConfigurationDBRef.ColumnChangeCaseEnum parsed = ConfigurationDBRef.ColumnChangeCaseEnum.valueOf(value.toUpperCase(Locale.ENGLISH));
                configDBRef.setColumnChangeCase(parsed);
            } else if (subElement.getNodeName().equals("metadata-origin")) {
                String value = getRequiredAttribute(subElement, "value");
                ConfigurationDBRef.MetadataOriginEnum parsed = ConfigurationDBRef.MetadataOriginEnum.valueOf(value.toUpperCase(Locale.ENGLISH));
                configDBRef.setMetadataOrigin(parsed);
            } else if (subElement.getNodeName().equals("sql-types-mapping")) {
                String sqlType = getRequiredAttribute(subElement, "sql-type");
                String javaType = getRequiredAttribute(subElement, "java-type");
                Integer sqlTypeInt;
                try {
                    sqlTypeInt = Integer.parseInt(sqlType);
                } catch (NumberFormatException ex) {
                    throw new ConfigurationException("Error converting sql type '" + sqlType + "' to integer java.sql.Types constant");
                }
                configDBRef.addSqlTypesBinding(sqlTypeInt, javaType);
            } else if (subElement.getNodeName().equals("expiry-time-cache")) {
                String maxAge = getRequiredAttribute(subElement, "max-age-seconds");
                String purgeInterval = getRequiredAttribute(subElement, "purge-interval-seconds");
                ConfigurationCacheReferenceType refTypeEnum = ConfigurationCacheReferenceType.getDefault();
                if (subElement.getAttributes().getNamedItem("ref-type") != null) {
                    String refType = subElement.getAttributes().getNamedItem("ref-type").getTextContent();
                    refTypeEnum = ConfigurationCacheReferenceType.valueOf(refType.toUpperCase(Locale.ENGLISH));
                }
                configDBRef.setExpiryTimeCache(Double.parseDouble(maxAge), Double.parseDouble(purgeInterval), refTypeEnum);
            } else if (subElement.getNodeName().equals("lru-cache")) {
                String size = getRequiredAttribute(subElement, "size");
                configDBRef.setLRUCache(Integer.parseInt(size));
            }
        }
    }

    private static void handleMethodReference(Configuration configuration, Element element) {
        String className = getRequiredAttribute(element, "class-name");
        ConfigurationMethodRef configMethodRef = new ConfigurationMethodRef();
        configuration.addMethodRef(className, configMethodRef);

        DOMElementIterator nodeIterator = new DOMElementIterator(element.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("expiry-time-cache")) {
                String maxAge = getRequiredAttribute(subElement, "max-age-seconds");
                String purgeInterval = getRequiredAttribute(subElement, "purge-interval-seconds");
                ConfigurationCacheReferenceType refTypeEnum = ConfigurationCacheReferenceType.getDefault();
                if (subElement.getAttributes().getNamedItem("ref-type") != null) {
                    String refType = subElement.getAttributes().getNamedItem("ref-type").getTextContent();
                    refTypeEnum = ConfigurationCacheReferenceType.valueOf(refType.toUpperCase(Locale.ENGLISH));
                }
                configMethodRef.setExpiryTimeCache(Double.parseDouble(maxAge), Double.parseDouble(purgeInterval), refTypeEnum);
            } else if (subElement.getNodeName().equals("lru-cache")) {
                String size = getRequiredAttribute(subElement, "size");
                configMethodRef.setLRUCache(Integer.parseInt(size));
            }
        }
    }

    private static void handlePlugInView(Configuration configuration, Element element) {
        String namespace = getRequiredAttribute(element, "namespace");
        String name = getRequiredAttribute(element, "name");
        String factoryClassName = getRequiredAttribute(element, "factory-class");
        configuration.addPlugInView(namespace, name, factoryClassName);
    }

    private static void handlePlugInVirtualDW(Configuration configuration, Element element) {
        String namespace = getRequiredAttribute(element, "namespace");
        String name = getRequiredAttribute(element, "name");
        String factoryClassName = getRequiredAttribute(element, "factory-class");
        String config = getOptionalAttribute(element, "config");
        configuration.addPlugInVirtualDataWindow(namespace, name, factoryClassName, config);
    }

    private static void handlePlugInAggregation(Configuration configuration, Element element) {
        String name = getRequiredAttribute(element, "name");
        String factoryClassName = getRequiredAttribute(element, "factory-class");
        configuration.addPlugInAggregationFunctionFactory(name, factoryClassName);
    }

    private static void handlePlugInMultiFunctionAggregation(Configuration configuration, Element element) {
        String functionNames = getRequiredAttribute(element, "function-names");
        String factoryClassName = getOptionalAttribute(element, "factory-class");

        DOMElementIterator nodeIterator = new DOMElementIterator(element.getChildNodes());
        Map<String, Object> additionalProps = null;
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("init-arg")) {
                String name = getRequiredAttribute(subElement, "name");
                String value = getRequiredAttribute(subElement, "value");
                if (additionalProps == null) {
                    additionalProps = new HashMap<String, Object>();
                }
                additionalProps.put(name, value);
            }
        }

        ConfigurationPlugInAggregationMultiFunction config = new ConfigurationPlugInAggregationMultiFunction(functionNames.split(","), factoryClassName);
        config.setAdditionalConfiguredProperties(additionalProps);
        configuration.addPlugInAggregationMultiFunction(config);
    }

    private static void handlePlugInSingleRow(Configuration configuration, Element element) {
        String name = element.getAttributes().getNamedItem("name").getTextContent();
        String functionClassName = element.getAttributes().getNamedItem("function-class").getTextContent();
        String functionMethodName = element.getAttributes().getNamedItem("function-method").getTextContent();
        ConfigurationPlugInSingleRowFunction.ValueCache valueCache = ConfigurationPlugInSingleRowFunction.ValueCache.DISABLED;
        ConfigurationPlugInSingleRowFunction.FilterOptimizable filterOptimizable = ConfigurationPlugInSingleRowFunction.FilterOptimizable.ENABLED;
        String valueCacheStr = getOptionalAttribute(element, "value-cache");
        if (valueCacheStr != null) {
            valueCache = ConfigurationPlugInSingleRowFunction.ValueCache.valueOf(valueCacheStr.toUpperCase(Locale.ENGLISH));
        }
        String filterOptimizableStr = getOptionalAttribute(element, "filter-optimizable");
        if (filterOptimizableStr != null) {
            filterOptimizable = ConfigurationPlugInSingleRowFunction.FilterOptimizable.valueOf(filterOptimizableStr.toUpperCase(Locale.ENGLISH));
        }
        String rethrowExceptionsStr = getOptionalAttribute(element, "rethrow-exceptions");
        boolean rethrowExceptions = false;
        if (rethrowExceptionsStr != null) {
            rethrowExceptions = Boolean.parseBoolean(rethrowExceptionsStr);
        }
        String eventTypeName = getOptionalAttribute(element, "event-type-name");
        configuration.addPlugInSingleRowFunction(new ConfigurationPlugInSingleRowFunction(name, functionClassName, functionMethodName, valueCache, filterOptimizable, rethrowExceptions, eventTypeName));
    }

    private static void handlePlugInPatternGuard(Configuration configuration, Element element) {
        String namespace = getRequiredAttribute(element, "namespace");
        String name = getRequiredAttribute(element, "name");
        String factoryClassName = getRequiredAttribute(element, "factory-class");
        configuration.addPlugInPatternGuard(namespace, name, factoryClassName);
    }

    private static void handlePlugInPatternObserver(Configuration configuration, Element element) {
        String namespace = getRequiredAttribute(element, "namespace");
        String name = getRequiredAttribute(element, "name");
        String factoryClassName = getRequiredAttribute(element, "factory-class");
        configuration.addPlugInPatternObserver(namespace, name, factoryClassName);
    }

    private static void handleVariable(Configuration configuration, Element element) {
        String variableName = getRequiredAttribute(element, "name");
        String type = getRequiredAttribute(element, "type");

        Class variableType = JavaClassHelper.getClassForSimpleName(type, ClassForNameProviderDefault.INSTANCE);
        if (variableType == null) {
            throw new ConfigurationException("Invalid variable type for variable '" + variableName + "', the type is not recognized");
        }

        Node initValueNode = element.getAttributes().getNamedItem("initialization-value");
        String initValue = null;
        if (initValueNode != null) {
            initValue = initValueNode.getTextContent();
        }

        boolean isConstant = false;
        if (getOptionalAttribute(element, "constant") != null) {
            isConstant = Boolean.parseBoolean(getOptionalAttribute(element, "constant"));
        }

        configuration.addVariable(variableName, variableType, initValue, isConstant);
    }

    private static void handlePluginLoaders(Configuration configuration, Element element) {
        String loaderName = getRequiredAttribute(element, "name");
        String className = getRequiredAttribute(element, "class-name");
        Properties properties = new Properties();
        String configXML = null;
        DOMElementIterator nodeIterator = new DOMElementIterator(element.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("init-arg")) {
                String name = getRequiredAttribute(subElement, "name");
                String value = getRequiredAttribute(subElement, "value");
                properties.put(name, value);
            }
            if (subElement.getNodeName().equals("config-xml")) {
                DOMElementIterator nodeIter = new DOMElementIterator(subElement.getChildNodes());
                if (!nodeIter.hasNext()) {
                    throw new ConfigurationException("Error handling config-xml for plug-in loader '" + loaderName + "', no child node found under initializer element, expecting an element node");
                }

                StringWriter output = new StringWriter();
                try {
                    TransformerFactory.newInstance().newTransformer().transform(new DOMSource(nodeIter.next()), new StreamResult(output));
                } catch (TransformerException e) {
                    throw new ConfigurationException("Error handling config-xml for plug-in loader '" + loaderName + "' :" + e.getMessage(), e);
                }
                configXML = output.toString();
            }
        }
        configuration.addPluginLoader(loaderName, className, properties, configXML);
    }

    private static void handlePlugInEventRepresentation(Configuration configuration, Element element) {
        DOMElementIterator nodeIterator = new DOMElementIterator(element.getChildNodes());
        String uri = getRequiredAttribute(element, "uri");
        String className = getRequiredAttribute(element, "class-name");
        String initializer = null;
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("initializer")) {
                DOMElementIterator nodeIter = new DOMElementIterator(subElement.getChildNodes());
                if (!nodeIter.hasNext()) {
                    throw new ConfigurationException("Error handling initializer for plug-in event representation '" + uri + "', no child node found under initializer element, expecting an element node");
                }

                StringWriter output = new StringWriter();
                try {
                    TransformerFactory.newInstance().newTransformer().transform(new DOMSource(nodeIter.next()), new StreamResult(output));
                } catch (TransformerException e) {
                    throw new ConfigurationException("Error handling initializer for plug-in event representation '" + uri + "' :" + e.getMessage(), e);
                }
                initializer = output.toString();
            }
        }

        URI uriParsed;
        try {
            uriParsed = new URI(uri);
        } catch (URISyntaxException ex) {
            throw new ConfigurationException("Error parsing URI '" + uri + "' as a valid java.net.URI string:" + ex.getMessage(), ex);
        }
        configuration.addPlugInEventRepresentation(uriParsed, className, initializer);
    }

    private static void handlePlugInEventType(Configuration configuration, Element element) {
        DOMElementIterator nodeIterator = new DOMElementIterator(element.getChildNodes());
        List<URI> uris = new ArrayList<URI>();
        String name = getRequiredAttribute(element, "name");
        String initializer = null;
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("resolution-uri")) {
                String uriValue = getRequiredAttribute(subElement, "value");
                URI uri;
                try {
                    uri = new URI(uriValue);
                } catch (URISyntaxException ex) {
                    throw new ConfigurationException("Error parsing URI '" + uriValue + "' as a valid java.net.URI string:" + ex.getMessage(), ex);
                }
                uris.add(uri);
            }
            if (subElement.getNodeName().equals("initializer")) {
                DOMElementIterator nodeIter = new DOMElementIterator(subElement.getChildNodes());
                if (!nodeIter.hasNext()) {
                    throw new ConfigurationException("Error handling initializer for plug-in event type '" + name + "', no child node found under initializer element, expecting an element node");
                }

                StringWriter output = new StringWriter();
                try {
                    TransformerFactory.newInstance().newTransformer().transform(new DOMSource(nodeIter.next()), new StreamResult(output));
                } catch (TransformerException e) {
                    throw new ConfigurationException("Error handling initializer for plug-in event type '" + name + "' :" + e.getMessage(), e);
                }
                initializer = output.toString();
            }
        }

        configuration.addPlugInEventType(name, uris.toArray(new URI[uris.size()]), initializer);
    }

    private static void handlePlugIneventTypeNameResolution(Configuration configuration, Element element) {
        DOMElementIterator nodeIterator = new DOMElementIterator(element.getChildNodes());
        List<URI> uris = new ArrayList<URI>();
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("resolution-uri")) {
                String uriValue = getRequiredAttribute(subElement, "value");
                URI uri;
                try {
                    uri = new URI(uriValue);
                } catch (URISyntaxException ex) {
                    throw new ConfigurationException("Error parsing URI '" + uriValue + "' as a valid java.net.URI string:" + ex.getMessage(), ex);
                }
                uris.add(uri);
            }
        }

        configuration.setPlugInEventTypeResolutionURIs(uris.toArray(new URI[uris.size()]));
    }

    private static void handleRevisionEventType(Configuration configuration, Element element) {
        ConfigurationRevisionEventType revEventType = new ConfigurationRevisionEventType();
        String revTypeName = getRequiredAttribute(element, "name");

        if (element.getAttributes().getNamedItem("property-revision") != null) {
            String propertyRevision = element.getAttributes().getNamedItem("property-revision").getTextContent();
            ConfigurationRevisionEventType.PropertyRevision propertyRevisionEnum;
            try {
                propertyRevisionEnum = ConfigurationRevisionEventType.PropertyRevision.valueOf(propertyRevision.trim().toUpperCase(Locale.ENGLISH));
                revEventType.setPropertyRevision(propertyRevisionEnum);
            } catch (RuntimeException ex) {
                throw new ConfigurationException("Invalid enumeration value for property-revision attribute '" + propertyRevision + "'");
            }
        }

        DOMElementIterator nodeIterator = new DOMElementIterator(element.getChildNodes());
        Set<String> keyProperties = new HashSet<String>();

        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("base-event-type")) {
                String name = getRequiredAttribute(subElement, "name");
                revEventType.addNameBaseEventType(name);
            }
            if (subElement.getNodeName().equals("delta-event-type")) {
                String name = getRequiredAttribute(subElement, "name");
                revEventType.addNameDeltaEventType(name);
            }
            if (subElement.getNodeName().equals("key-property")) {
                String name = getRequiredAttribute(subElement, "name");
                keyProperties.add(name);
            }
        }

        String[] keyProps = keyProperties.toArray(new String[keyProperties.size()]);
        revEventType.setKeyPropertyNames(keyProps);

        configuration.addRevisionEventType(revTypeName, revEventType);
    }

    private static void handleVariantStream(Configuration configuration, Element element) {
        ConfigurationVariantStream variantStream = new ConfigurationVariantStream();
        String varianceName = getRequiredAttribute(element, "name");

        if (element.getAttributes().getNamedItem("type-variance") != null) {
            String typeVar = element.getAttributes().getNamedItem("type-variance").getTextContent();
            ConfigurationVariantStream.TypeVariance typeVarianceEnum;
            try {
                typeVarianceEnum = ConfigurationVariantStream.TypeVariance.valueOf(typeVar.trim().toUpperCase(Locale.ENGLISH));
                variantStream.setTypeVariance(typeVarianceEnum);
            } catch (RuntimeException ex) {
                throw new ConfigurationException("Invalid enumeration value for type-variance attribute '" + typeVar + "'");
            }
        }

        DOMElementIterator nodeIterator = new DOMElementIterator(element.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("variant-event-type")) {
                String name = subElement.getAttributes().getNamedItem("name").getTextContent();
                variantStream.addEventTypeName(name);
            }
        }

        configuration.addVariantStream(varianceName, variantStream);
    }

    private static void handleEngineSettings(Configuration configuration, Element element) {
        DOMElementIterator nodeIterator = new DOMElementIterator(element.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("defaults")) {
                handleEngineSettingsDefaults(configuration, subElement);
            }
        }
    }

    private static void handleEngineSettingsDefaults(Configuration configuration, Element parentElement) {
        DOMElementIterator nodeIterator = new DOMElementIterator(parentElement.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("threading")) {
                handleDefaultsThreading(configuration, subElement);
            }
            if (subElement.getNodeName().equals("event-meta")) {
                handleDefaultsEventMeta(configuration, subElement);
            }
            if (subElement.getNodeName().equals("view-resources")) {
                handleDefaultsViewResources(configuration, subElement);
            }
            if (subElement.getNodeName().equals("logging")) {
                handleDefaultsLogging(configuration, subElement);
            }
            if (subElement.getNodeName().equals("variables")) {
                handleDefaultsVariables(configuration, subElement);
            }
            if (subElement.getNodeName().equals("patterns")) {
                handleDefaultsPatterns(configuration, subElement);
            }
            if (subElement.getNodeName().equals("match-recognize")) {
                handleDefaultsMatchRecognize(configuration, subElement);
            }
            if (subElement.getNodeName().equals("stream-selection")) {
                handleDefaultsStreamSelection(configuration, subElement);
            }
            if (subElement.getNodeName().equals("time-source")) {
                handleDefaultsTimeSource(configuration, subElement);
            }
            if (subElement.getNodeName().equals("metrics-reporting")) {
                handleMetricsReporting(configuration, subElement);
            }
            if (subElement.getNodeName().equals("language")) {
                handleLanguage(configuration, subElement);
            }
            if (subElement.getNodeName().equals("expression")) {
                handleExpression(configuration, subElement);
            }
            if (subElement.getNodeName().equals("execution")) {
                handleExecution(configuration, subElement);
            }
            if (subElement.getNodeName().equals("bytecodegen")) {
                handleByteCodeGen(configuration, subElement);
            }
            if (subElement.getNodeName().equals("exceptionHandling")) {
                configuration.getEngineDefaults().getExceptionHandling().addClasses(getHandlerFactories(subElement));
                String enableUndeployRethrowStr = getOptionalAttribute(subElement, "undeploy-rethrow-policy");
                if (enableUndeployRethrowStr != null) {
                    configuration.getEngineDefaults().getExceptionHandling().setUndeployRethrowPolicy(ConfigurationEngineDefaults.ExceptionHandling.UndeployRethrowPolicy.valueOf(enableUndeployRethrowStr.toUpperCase(Locale.ENGLISH)));
                }
            }
            if (subElement.getNodeName().equals("conditionHandling")) {
                configuration.getEngineDefaults().getConditionHandling().addClasses(getHandlerFactories(subElement));
            }
            if (subElement.getNodeName().equals("scripts")) {
                handleDefaultScriptConfig(configuration, subElement);
            }
        }
    }

    private static void handleDefaultsThreading(Configuration configuration, Element parentElement) {
        String engineFairlockStr = getOptionalAttribute(parentElement, "engine-fairlock");
        if (engineFairlockStr != null) {
            boolean isEngineFairlock = Boolean.parseBoolean(engineFairlockStr);
            configuration.getEngineDefaults().getThreading().setEngineFairlock(isEngineFairlock);
        }

        DOMElementIterator nodeIterator = new DOMElementIterator(parentElement.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("listener-dispatch")) {
                String preserveOrderText = getRequiredAttribute(subElement, "preserve-order");
                Boolean preserveOrder = Boolean.parseBoolean(preserveOrderText);
                configuration.getEngineDefaults().getThreading().setListenerDispatchPreserveOrder(preserveOrder);

                if (subElement.getAttributes().getNamedItem("timeout-msec") != null) {
                    String timeoutMSecText = subElement.getAttributes().getNamedItem("timeout-msec").getTextContent();
                    Long timeoutMSec = Long.parseLong(timeoutMSecText);
                    configuration.getEngineDefaults().getThreading().setListenerDispatchTimeout(timeoutMSec);
                }

                if (subElement.getAttributes().getNamedItem("locking") != null) {
                    String value = subElement.getAttributes().getNamedItem("locking").getTextContent();
                    configuration.getEngineDefaults().getThreading().setListenerDispatchLocking(
                            ConfigurationEngineDefaults.Threading.Locking.valueOf(value.toUpperCase(Locale.ENGLISH)));
                }
            }
            if (subElement.getNodeName().equals("insert-into-dispatch")) {
                String preserveOrderText = getRequiredAttribute(subElement, "preserve-order");
                Boolean preserveOrder = Boolean.parseBoolean(preserveOrderText);
                configuration.getEngineDefaults().getThreading().setInsertIntoDispatchPreserveOrder(preserveOrder);

                if (subElement.getAttributes().getNamedItem("timeout-msec") != null) {
                    String timeoutMSecText = subElement.getAttributes().getNamedItem("timeout-msec").getTextContent();
                    Long timeoutMSec = Long.parseLong(timeoutMSecText);
                    configuration.getEngineDefaults().getThreading().setInsertIntoDispatchTimeout(timeoutMSec);
                }

                if (subElement.getAttributes().getNamedItem("locking") != null) {
                    String value = subElement.getAttributes().getNamedItem("locking").getTextContent();
                    configuration.getEngineDefaults().getThreading().setInsertIntoDispatchLocking(
                            ConfigurationEngineDefaults.Threading.Locking.valueOf(value.toUpperCase(Locale.ENGLISH)));
                }
            }
            if (subElement.getNodeName().equals("named-window-consumer-dispatch")) {
                String preserveOrderText = getRequiredAttribute(subElement, "preserve-order");
                Boolean preserveOrder = Boolean.parseBoolean(preserveOrderText);
                configuration.getEngineDefaults().getThreading().setNamedWindowConsumerDispatchPreserveOrder(preserveOrder);

                if (subElement.getAttributes().getNamedItem("timeout-msec") != null) {
                    String timeoutMSecText = subElement.getAttributes().getNamedItem("timeout-msec").getTextContent();
                    Long timeoutMSec = Long.parseLong(timeoutMSecText);
                    configuration.getEngineDefaults().getThreading().setNamedWindowConsumerDispatchTimeout(timeoutMSec);
                }

                if (subElement.getAttributes().getNamedItem("locking") != null) {
                    String value = subElement.getAttributes().getNamedItem("locking").getTextContent();
                    configuration.getEngineDefaults().getThreading().setNamedWindowConsumerDispatchLocking(
                            ConfigurationEngineDefaults.Threading.Locking.valueOf(value.toUpperCase(Locale.ENGLISH)));
                }
            }
            if (subElement.getNodeName().equals("internal-timer")) {
                String enabledText = getRequiredAttribute(subElement, "enabled");
                Boolean enabled = Boolean.parseBoolean(enabledText);
                String msecResolutionText = getRequiredAttribute(subElement, "msec-resolution");
                Long msecResolution = Long.parseLong(msecResolutionText);
                configuration.getEngineDefaults().getThreading().setInternalTimerEnabled(enabled);
                configuration.getEngineDefaults().getThreading().setInternalTimerMsecResolution(msecResolution);
            }
            if (subElement.getNodeName().equals("threadpool-inbound")) {
                ThreadPoolConfig result = parseThreadPoolConfig(subElement);
                configuration.getEngineDefaults().getThreading().setThreadPoolInbound(result.isEnabled());
                configuration.getEngineDefaults().getThreading().setThreadPoolInboundNumThreads(result.getNumThreads());
                configuration.getEngineDefaults().getThreading().setThreadPoolInboundCapacity(result.getCapacity());
            }
            if (subElement.getNodeName().equals("threadpool-outbound")) {
                ThreadPoolConfig result = parseThreadPoolConfig(subElement);
                configuration.getEngineDefaults().getThreading().setThreadPoolOutbound(result.isEnabled());
                configuration.getEngineDefaults().getThreading().setThreadPoolOutboundNumThreads(result.getNumThreads());
                configuration.getEngineDefaults().getThreading().setThreadPoolOutboundCapacity(result.getCapacity());
            }
            if (subElement.getNodeName().equals("threadpool-timerexec")) {
                ThreadPoolConfig result = parseThreadPoolConfig(subElement);
                configuration.getEngineDefaults().getThreading().setThreadPoolTimerExec(result.isEnabled());
                configuration.getEngineDefaults().getThreading().setThreadPoolTimerExecNumThreads(result.getNumThreads());
                configuration.getEngineDefaults().getThreading().setThreadPoolTimerExecCapacity(result.getCapacity());
            }
            if (subElement.getNodeName().equals("threadpool-routeexec")) {
                ThreadPoolConfig result = parseThreadPoolConfig(subElement);
                configuration.getEngineDefaults().getThreading().setThreadPoolRouteExec(result.isEnabled());
                configuration.getEngineDefaults().getThreading().setThreadPoolRouteExecNumThreads(result.getNumThreads());
                configuration.getEngineDefaults().getThreading().setThreadPoolRouteExecCapacity(result.getCapacity());
            }
        }
    }

    private static ThreadPoolConfig parseThreadPoolConfig(Element parentElement) {
        String enabled = getRequiredAttribute(parentElement, "enabled");
        boolean isEnabled = Boolean.parseBoolean(enabled);

        String numThreadsStr = getRequiredAttribute(parentElement, "num-threads");
        int numThreads = Integer.parseInt(numThreadsStr);

        String capacityStr = getOptionalAttribute(parentElement, "capacity");
        Integer capacity = null;
        if (capacityStr != null) {
            capacity = Integer.parseInt(capacityStr);
        }

        return new ThreadPoolConfig(isEnabled, numThreads, capacity);
    }

    private static void handleDefaultsViewResources(Configuration configuration, Element parentElement) {
        DOMElementIterator nodeIterator = new DOMElementIterator(parentElement.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("share-views")) {
                String valueText = getRequiredAttribute(subElement, "enabled");
                Boolean value = Boolean.parseBoolean(valueText);
                configuration.getEngineDefaults().getViewResources().setShareViews(value);
            }
            if (subElement.getNodeName().equals("allow-multiple-expiry-policy")) {
                String valueText = getRequiredAttribute(subElement, "enabled");
                Boolean value = Boolean.parseBoolean(valueText);
                configuration.getEngineDefaults().getViewResources().setAllowMultipleExpiryPolicies(value);
            }
            if (subElement.getNodeName().equals("iterable-unbound")) {
                String valueText = getRequiredAttribute(subElement, "enabled");
                Boolean value = Boolean.parseBoolean(valueText);
                configuration.getEngineDefaults().getViewResources().setIterableUnbound(value);
            }
            if (subElement.getNodeName().equals("outputlimitopt")) {
                String valueText = getRequiredAttribute(subElement, "enabled");
                Boolean value = Boolean.parseBoolean(valueText);
                configuration.getEngineDefaults().getViewResources().setOutputLimitOpt(value);
            }
        }
    }

    private static void handleDefaultsLogging(Configuration configuration, Element parentElement) {
        DOMElementIterator nodeIterator = new DOMElementIterator(parentElement.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("execution-path")) {
                String valueText = getRequiredAttribute(subElement, "enabled");
                Boolean value = Boolean.parseBoolean(valueText);
                configuration.getEngineDefaults().getLogging().setEnableExecutionDebug(value);
            }
            if (subElement.getNodeName().equals("timer-debug")) {
                String valueText = getRequiredAttribute(subElement, "enabled");
                Boolean value = Boolean.parseBoolean(valueText);
                configuration.getEngineDefaults().getLogging().setEnableTimerDebug(value);
            }
            if (subElement.getNodeName().equals("query-plan")) {
                String valueText = getRequiredAttribute(subElement, "enabled");
                Boolean value = Boolean.parseBoolean(valueText);
                configuration.getEngineDefaults().getLogging().setEnableQueryPlan(value);
            }
            if (subElement.getNodeName().equals("jdbc")) {
                String valueText = getRequiredAttribute(subElement, "enabled");
                Boolean value = Boolean.parseBoolean(valueText);
                configuration.getEngineDefaults().getLogging().setEnableJDBC(value);
            }
            if (subElement.getNodeName().equals("audit")) {
                configuration.getEngineDefaults().getLogging().setAuditPattern(getOptionalAttribute(subElement, "pattern"));
            }
            if (subElement.getNodeName().equals("code")) {
                String valueText = getRequiredAttribute(subElement, "enabled");
                Boolean value = Boolean.parseBoolean(valueText);
                configuration.getEngineDefaults().getLogging().setEnableCode(value);
            }
        }
    }

    private static void handleDefaultsVariables(Configuration configuration, Element parentElement) {
        DOMElementIterator nodeIterator = new DOMElementIterator(parentElement.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("msec-version-release")) {
                String valueText = getRequiredAttribute(subElement, "value");
                Long value = Long.parseLong(valueText);
                configuration.getEngineDefaults().getVariables().setMsecVersionRelease(value);
            }
        }
    }

    private static void handleDefaultsPatterns(Configuration configuration, Element parentElement) {
        DOMElementIterator nodeIterator = new DOMElementIterator(parentElement.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("max-subexpression")) {
                String valueText = getRequiredAttribute(subElement, "value");
                Long value = Long.parseLong(valueText);
                configuration.getEngineDefaults().getPatterns().setMaxSubexpressions(value);

                String preventText = getOptionalAttribute(subElement, "prevent-start");
                if (preventText != null) {
                    configuration.getEngineDefaults().getPatterns().setMaxSubexpressionPreventStart(Boolean.parseBoolean(preventText));
                }
            }
        }
    }

    private static void handleDefaultsMatchRecognize(Configuration configuration, Element parentElement) {
        DOMElementIterator nodeIterator = new DOMElementIterator(parentElement.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("max-state")) {
                String valueText = getRequiredAttribute(subElement, "value");
                Long value = Long.parseLong(valueText);
                configuration.getEngineDefaults().getMatchRecognize().setMaxStates(value);

                String preventText = getOptionalAttribute(subElement, "prevent-start");
                if (preventText != null) {
                    configuration.getEngineDefaults().getMatchRecognize().setMaxStatesPreventStart(Boolean.parseBoolean(preventText));
                }
            }
        }
    }

    private static void handleDefaultsStreamSelection(Configuration configuration, Element parentElement) {
        DOMElementIterator nodeIterator = new DOMElementIterator(parentElement.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("stream-selector")) {
                String valueText = getRequiredAttribute(subElement, "value");
                if (valueText == null) {
                    throw new ConfigurationException("No value attribute supplied for stream-selector element");
                }
                StreamSelector defaultSelector;
                if (valueText.toUpperCase(Locale.ENGLISH).trim().equals("ISTREAM")) {
                    defaultSelector = StreamSelector.ISTREAM_ONLY;
                } else if (valueText.toUpperCase(Locale.ENGLISH).trim().equals("RSTREAM")) {
                    defaultSelector = StreamSelector.RSTREAM_ONLY;
                } else if (valueText.toUpperCase(Locale.ENGLISH).trim().equals("IRSTREAM")) {
                    defaultSelector = StreamSelector.RSTREAM_ISTREAM_BOTH;
                } else {
                    throw new ConfigurationException("Value attribute for stream-selector element invalid, " +
                            "expected one of the following keywords: istream, irstream, rstream");
                }
                configuration.getEngineDefaults().getStreamSelection().setDefaultStreamSelector(defaultSelector);
            }
        }
    }

    private static void handleDefaultsTimeSource(Configuration configuration, Element parentElement) {
        DOMElementIterator nodeIterator = new DOMElementIterator(parentElement.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("time-source-type")) {
                String valueText = getRequiredAttribute(subElement, "value");
                if (valueText == null) {
                    throw new ConfigurationException("No value attribute supplied for time-source element");
                }
                ConfigurationEngineDefaults.TimeSourceType timeSourceType;
                if (valueText.toUpperCase(Locale.ENGLISH).trim().equals("NANO")) {
                    timeSourceType = ConfigurationEngineDefaults.TimeSourceType.NANO;
                } else if (valueText.toUpperCase(Locale.ENGLISH).trim().equals("MILLI")) {
                    timeSourceType = ConfigurationEngineDefaults.TimeSourceType.MILLI;
                } else {
                    throw new ConfigurationException("Value attribute for time-source element invalid, " +
                            "expected one of the following keywords: nano, milli");
                }
                configuration.getEngineDefaults().getTimeSource().setTimeSourceType(timeSourceType);
            }

            if (subElement.getNodeName().equals("time-unit")) {
                String valueText = getRequiredAttribute(subElement, "value");
                if (valueText == null) {
                    throw new ConfigurationException("No value attribute supplied for time-unit element");
                }
                try {
                    TimeUnit timeUnit = TimeUnit.valueOf(valueText.toUpperCase(Locale.ENGLISH));
                    configuration.getEngineDefaults().getTimeSource().setTimeUnit(timeUnit);
                } catch (Throwable t) {
                    throw new ConfigurationException("Value attribute for time-unit element invalid: " + t.getMessage(), t);
                }
            }
        }
    }

    private static void handleMetricsReporting(Configuration configuration, Element parentElement) {
        String enabled = getRequiredAttribute(parentElement, "enabled");
        boolean isEnabled = Boolean.parseBoolean(enabled);
        configuration.getEngineDefaults().getMetricsReporting().setEnableMetricsReporting(isEnabled);

        String engineInterval = getOptionalAttribute(parentElement, "engine-interval");
        if (engineInterval != null) {
            configuration.getEngineDefaults().getMetricsReporting().setEngineInterval(Long.parseLong(engineInterval));
        }

        String statementInterval = getOptionalAttribute(parentElement, "statement-interval");
        if (statementInterval != null) {
            configuration.getEngineDefaults().getMetricsReporting().setStatementInterval(Long.parseLong(statementInterval));
        }

        String threading = getOptionalAttribute(parentElement, "threading");
        if (threading != null) {
            configuration.getEngineDefaults().getMetricsReporting().setThreading(Boolean.parseBoolean(threading));
        }

        String jmxEngineMetrics = getOptionalAttribute(parentElement, "jmx-engine-metrics");
        if (jmxEngineMetrics != null) {
            configuration.getEngineDefaults().getMetricsReporting().setJmxEngineMetrics(Boolean.parseBoolean(jmxEngineMetrics));
        }

        DOMElementIterator nodeIterator = new DOMElementIterator(parentElement.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("stmtgroup")) {
                String name = getRequiredAttribute(subElement, "name");
                long interval = Long.parseLong(getRequiredAttribute(subElement, "interval"));

                ConfigurationMetricsReporting.StmtGroupMetrics metrics = new ConfigurationMetricsReporting.StmtGroupMetrics();
                metrics.setInterval(interval);
                configuration.getEngineDefaults().getMetricsReporting().addStmtGroup(name, metrics);

                String defaultInclude = getOptionalAttribute(subElement, "default-include");
                if (defaultInclude != null) {
                    metrics.setDefaultInclude(Boolean.parseBoolean(defaultInclude));
                }

                String numStmts = getOptionalAttribute(subElement, "num-stmts");
                if (numStmts != null) {
                    metrics.setNumStatements(Integer.parseInt(numStmts));
                }

                String reportInactive = getOptionalAttribute(subElement, "report-inactive");
                if (reportInactive != null) {
                    metrics.setReportInactive(Boolean.parseBoolean(reportInactive));
                }

                handleMetricsReportingPatterns(metrics, subElement);
            }
        }
    }

    private static void handleLanguage(Configuration configuration, Element parentElement) {
        String sortUsingCollator = getOptionalAttribute(parentElement, "sort-using-collator");
        if (sortUsingCollator != null) {
            boolean isSortUsingCollator = Boolean.parseBoolean(sortUsingCollator);
            configuration.getEngineDefaults().getLanguage().setSortUsingCollator(isSortUsingCollator);
        }
    }

    private static void handleExpression(Configuration configuration, Element parentElement) {
        String integerDivision = getOptionalAttribute(parentElement, "integer-division");
        if (integerDivision != null) {
            boolean isIntegerDivision = Boolean.parseBoolean(integerDivision);
            configuration.getEngineDefaults().getExpression().setIntegerDivision(isIntegerDivision);
        }
        String divZero = getOptionalAttribute(parentElement, "division-by-zero-is-null");
        if (divZero != null) {
            boolean isDivZero = Boolean.parseBoolean(divZero);
            configuration.getEngineDefaults().getExpression().setDivisionByZeroReturnsNull(isDivZero);
        }
        String udfCache = getOptionalAttribute(parentElement, "udf-cache");
        if (udfCache != null) {
            boolean isUdfCache = Boolean.parseBoolean(udfCache);
            configuration.getEngineDefaults().getExpression().setUdfCache(isUdfCache);
        }
        String selfSubselectPreeval = getOptionalAttribute(parentElement, "self-subselect-preeval");
        if (selfSubselectPreeval != null) {
            boolean isSelfSubselectPreeval = Boolean.parseBoolean(selfSubselectPreeval);
            configuration.getEngineDefaults().getExpression().setSelfSubselectPreeval(isSelfSubselectPreeval);
        }
        String extendedAggregationStr = getOptionalAttribute(parentElement, "extended-agg");
        if (extendedAggregationStr != null) {
            boolean extendedAggregation = Boolean.parseBoolean(extendedAggregationStr);
            configuration.getEngineDefaults().getExpression().setExtendedAggregation(extendedAggregation);
        }
        String duckTypingStr = getOptionalAttribute(parentElement, "ducktyping");
        if (duckTypingStr != null) {
            boolean duckTyping = Boolean.parseBoolean(duckTypingStr);
            configuration.getEngineDefaults().getExpression().setDuckTyping(duckTyping);
        }
        String mathContextStr = getOptionalAttribute(parentElement, "math-context");
        if (mathContextStr != null) {
            try {
                MathContext mathContext = new MathContext(mathContextStr);
                configuration.getEngineDefaults().getExpression().setMathContext(mathContext);
            } catch (IllegalArgumentException ex) {
                throw new ConfigurationException("Failed to parse '" + mathContextStr + "' as a MathContext");
            }
        }

        String timeZoneStr = getOptionalAttribute(parentElement, "time-zone");
        if (timeZoneStr != null) {
            TimeZone timeZone = TimeZone.getTimeZone(timeZoneStr);
            configuration.getEngineDefaults().getExpression().setTimeZone(timeZone);
        }
    }

    private static void handleByteCodeGen(Configuration configuration, Element element) {
        ConfigurationEngineDefaults.ByteCodeGeneration codegen = configuration.getEngineDefaults().getByteCodeGeneration();
        parseOptionalBoolean(element, "enable-aggregation", codegen::setEnableAggregation);
        parseOptionalBoolean(element, "enable-resultset", codegen::setEnableResultSet);
        parseOptionalBoolean(element, "enable-selectclause", codegen::setEnableSelectClause);
        parseOptionalBoolean(element, "enable-expression", codegen::setEnableExpression);
        parseOptionalBoolean(element, "enable-propertygetter", codegen::setEnablePropertyGetter);
        parseOptionalBoolean(element, "enable-fallback", codegen::setEnableFallback);
        parseOptionalBoolean(element, "include-debugsymbols", codegen::setIncludeDebugSymbols);
        parseOptionalBoolean(element, "include-comments", codegen::setIncludeComments);
    }

    private static void handleExecution(Configuration configuration, Element parentElement) {
        String prioritizedStr = getOptionalAttribute(parentElement, "prioritized");
        if (prioritizedStr != null) {
            boolean isPrioritized = Boolean.parseBoolean(prioritizedStr);
            configuration.getEngineDefaults().getExecution().setPrioritized(isPrioritized);
        }
        String fairlockStr = getOptionalAttribute(parentElement, "fairlock");
        if (fairlockStr != null) {
            boolean isFairlock = Boolean.parseBoolean(fairlockStr);
            configuration.getEngineDefaults().getExecution().setFairlock(isFairlock);
        }
        String disableLockingStr = getOptionalAttribute(parentElement, "disable-locking");
        if (disableLockingStr != null) {
            boolean isDisablelock = Boolean.parseBoolean(disableLockingStr);
            configuration.getEngineDefaults().getExecution().setDisableLocking(isDisablelock);
        }
        String threadingProfileStr = getOptionalAttribute(parentElement, "threading-profile");
        if (threadingProfileStr != null) {
            ConfigurationEngineDefaults.ThreadingProfile profile = ConfigurationEngineDefaults.ThreadingProfile.valueOf(threadingProfileStr.toUpperCase(Locale.ENGLISH));
            configuration.getEngineDefaults().getExecution().setThreadingProfile(profile);
        }
        String filterServiceProfileStr = getOptionalAttribute(parentElement, "filter-service-profile");
        if (filterServiceProfileStr != null) {
            ConfigurationEngineDefaults.FilterServiceProfile profile = ConfigurationEngineDefaults.FilterServiceProfile.valueOf(filterServiceProfileStr.toUpperCase(Locale.ENGLISH));
            configuration.getEngineDefaults().getExecution().setFilterServiceProfile(profile);
        }
        String filterServiceMaxFilterWidthStr = getOptionalAttribute(parentElement, "filter-service-max-filter-width");
        if (filterServiceMaxFilterWidthStr != null) {
            configuration.getEngineDefaults().getExecution().setFilterServiceMaxFilterWidth(Integer.parseInt(filterServiceMaxFilterWidthStr));
        }
        String allowIsolatedServiceStr = getOptionalAttribute(parentElement, "allow-isolated-service");
        if (allowIsolatedServiceStr != null) {
            boolean isAllowIsolatedService = Boolean.parseBoolean(allowIsolatedServiceStr);
            configuration.getEngineDefaults().getExecution().setAllowIsolatedService(isAllowIsolatedService);
        }
        String declExprValueCacheSizeStr = getOptionalAttribute(parentElement, "declared-expr-value-cache-size");
        if (declExprValueCacheSizeStr != null) {
            configuration.getEngineDefaults().getExecution().setDeclaredExprValueCacheSize(Integer.parseInt(declExprValueCacheSizeStr));
        }
    }

    private static void handleDefaultScriptConfig(Configuration configuration, Element parentElement) {
        String defaultDialect = getOptionalAttribute(parentElement, "default-dialect");
        if (defaultDialect != null) {
            configuration.getEngineDefaults().getScripts().setDefaultDialect(defaultDialect);
        }
    }

    private static List<String> getHandlerFactories(Element parentElement) {
        List<String> list = new ArrayList<String>();
        DOMElementIterator nodeIterator = new DOMElementIterator(parentElement.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("handlerFactory")) {
                String text = getRequiredAttribute(subElement, "class");
                list.add(text);
            }
        }
        return list;
    }

    private static void handleMetricsReportingPatterns(ConfigurationMetricsReporting.StmtGroupMetrics groupDef, Element parentElement) {
        DOMElementIterator nodeIterator = new DOMElementIterator(parentElement.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("include-regex")) {
                String text = subElement.getChildNodes().item(0).getTextContent();
                groupDef.getPatterns().add(new Pair<StringPatternSet, Boolean>(new StringPatternSetRegex(text), true));
            }
            if (subElement.getNodeName().equals("exclude-regex")) {
                String text = subElement.getChildNodes().item(0).getTextContent();
                groupDef.getPatterns().add(new Pair<StringPatternSet, Boolean>(new StringPatternSetRegex(text), false));
            }
            if (subElement.getNodeName().equals("include-like")) {
                String text = subElement.getChildNodes().item(0).getTextContent();
                groupDef.getPatterns().add(new Pair<StringPatternSet, Boolean>(new StringPatternSetLike(text), true));
            }
            if (subElement.getNodeName().equals("exclude-like")) {
                String text = subElement.getChildNodes().item(0).getTextContent();
                groupDef.getPatterns().add(new Pair<StringPatternSet, Boolean>(new StringPatternSetLike(text), false));
            }
        }
    }

    private static void handleDefaultsEventMeta(Configuration configuration, Element parentElement) {
        DOMElementIterator nodeIterator = new DOMElementIterator(parentElement.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("class-property-resolution")) {
                Node styleNode = subElement.getAttributes().getNamedItem("style");
                if (styleNode != null) {
                    String styleText = styleNode.getTextContent();
                    Configuration.PropertyResolutionStyle value = Configuration.PropertyResolutionStyle.valueOf(styleText.toUpperCase(Locale.ENGLISH));
                    configuration.getEngineDefaults().getEventMeta().setClassPropertyResolutionStyle(value);
                }

                Node accessorStyleNode = subElement.getAttributes().getNamedItem("accessor-style");
                if (accessorStyleNode != null) {
                    String accessorStyleText = accessorStyleNode.getTextContent();
                    ConfigurationEventTypeLegacy.AccessorStyle value = ConfigurationEventTypeLegacy.AccessorStyle.valueOf(accessorStyleText.toUpperCase(Locale.ENGLISH));
                    configuration.getEngineDefaults().getEventMeta().setDefaultAccessorStyle(value);
                }
            }

            if (subElement.getNodeName().equals("event-representation")) {
                Node typeNode = subElement.getAttributes().getNamedItem("type");
                if (typeNode != null) {
                    String typeText = typeNode.getTextContent();
                    EventUnderlyingType value = EventUnderlyingType.valueOf(typeText.toUpperCase(Locale.ENGLISH));
                    configuration.getEngineDefaults().getEventMeta().setDefaultEventRepresentation(value);
                }
            }

            if (subElement.getNodeName().equals("anonymous-cache")) {
                Node sizeNode = subElement.getAttributes().getNamedItem("size");
                if (sizeNode != null) {
                    configuration.getEngineDefaults().getEventMeta().setAnonymousCacheSize(Integer.parseInt(sizeNode.getTextContent()));
                }
            }

            if (subElement.getNodeName().equals("avro-settings")) {
                String enableAvroStr = getOptionalAttribute(subElement, "enable-avro");
                if (enableAvroStr != null) {
                    configuration.getEngineDefaults().getEventMeta().getAvroSettings().setEnableAvro(Boolean.parseBoolean(enableAvroStr));
                }

                String enableNativeStringStr = getOptionalAttribute(subElement, "enable-native-string");
                if (enableNativeStringStr != null) {
                    configuration.getEngineDefaults().getEventMeta().getAvroSettings().setEnableNativeString(Boolean.parseBoolean(enableNativeStringStr));
                }

                String enableSchemaDefaultNonNullStr = getOptionalAttribute(subElement, "enable-schema-default-nonnull");
                if (enableSchemaDefaultNonNullStr != null) {
                    configuration.getEngineDefaults().getEventMeta().getAvroSettings().setEnableSchemaDefaultNonNull(Boolean.parseBoolean(enableSchemaDefaultNonNullStr));
                }

                String objectvalueTypewidenerFactoryClass = getOptionalAttribute(subElement, "objectvalue-typewidener-factory-class");
                if (objectvalueTypewidenerFactoryClass != null && objectvalueTypewidenerFactoryClass.trim().length() > 0) {
                    configuration.getEngineDefaults().getEventMeta().getAvroSettings().setObjectValueTypeWidenerFactoryClass(objectvalueTypewidenerFactoryClass.trim());
                }

                String typeRepresentationMapperClass = getOptionalAttribute(subElement, "type-representation-mapper-class");
                configuration.getEngineDefaults().getEventMeta().getAvroSettings().setTypeRepresentationMapperClass(typeRepresentationMapperClass);
            }
        }
    }

    private static Properties handleProperties(Element element, String propElementName) {
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
            stream = ConfigurationParser.class.getResourceAsStream(resource);
        }
        if (stream == null) {
            stream = ConfigurationParser.class.getClassLoader().getResourceAsStream(stripped);
        }
        if (stream == null) {
            throw new EPException(resource + " not found");
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
            String name = node.getLocalName();
            if (name == null) {
                name = node.getNodeName();
            }
            throw new ConfigurationException("Required attribute by name '" + key + "' not found for element '" + name + "'");
        }
        return valueNode.getTextContent();
    }

    private static class ThreadPoolConfig {
        private boolean enabled;
        private int numThreads;
        private Integer capacity;

        public ThreadPoolConfig(boolean enabled, int numThreads, Integer capacity) {
            this.enabled = enabled;
            this.numThreads = numThreads;
            this.capacity = capacity;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public int getNumThreads() {
            return numThreads;
        }

        public Integer getCapacity() {
            return capacity;
        }
    }

    private static void parseOptionalBoolean(Element element, String name, Consumer<Boolean> func) {
        String str = getOptionalAttribute(element, name);
        if (str != null) {
            func.accept(Boolean.parseBoolean(str));
        }
    }

    private final static Logger log = LoggerFactory.getLogger(ConfigurationParser.class);
}
