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
package com.espertech.esper.common.client.configuration.common;

import com.espertech.esper.common.client.configuration.ConfigurationException;
import com.espertech.esper.common.client.util.*;
import com.espertech.esper.common.internal.util.DOMElementIterator;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static com.espertech.esper.common.internal.util.DOMUtil.*;

/**
 * Parser for the common section of configuration.
 */
public class ConfigurationCommonParser {
    /**
     * Configure the common section from a provided element
     *
     * @param common        common section
     * @param commonElement element
     */
    public static void doConfigure(ConfigurationCommon common, Element commonElement) {
        DOMElementIterator eventTypeNodeIterator = new DOMElementIterator(commonElement.getChildNodes());
        while (eventTypeNodeIterator.hasNext()) {
            Element element = eventTypeNodeIterator.next();
            String nodeName = element.getNodeName();
            if (nodeName.equals("event-type")) {
                handleEventTypes(common, element);
            } else if (nodeName.equals("auto-import")) {
                handleAutoImports(common, element);
            } else if (nodeName.equals("auto-import-annotations")) {
                handleAutoImportAnnotations(common, element);
            } else if (nodeName.equals("method-reference")) {
                handleMethodReference(common, element);
            } else if (nodeName.equals("database-reference")) {
                handleDatabaseRefs(common, element);
            } else if (nodeName.equals("variable")) {
                handleVariable(common, element);
            } else if (nodeName.equals("variant-stream")) {
                handleVariantStream(common, element);
            } else if (nodeName.equals("event-meta")) {
                handleEventMeta(common, element);
            } else if (nodeName.equals("logging")) {
                handleLogging(common, element);
            } else if (nodeName.equals("time-source")) {
                handleTimeSource(common, element);
            } else if (nodeName.equals("execution")) {
                handleExecution(common, element);
            } else if (nodeName.equals("event-type-auto-name")) {
                handleEventTypeAutoNames(common, element);
            }
        }
    }

    private static void handleEventTypeAutoNames(ConfigurationCommon configuration, Element element) {
        String name = getRequiredAttribute(element, "package-name");
        configuration.addEventTypeAutoName(name);
    }

    private static void handleExecution(ConfigurationCommon common, Element parentElement) {
        String threadingProfileStr = getOptionalAttribute(parentElement, "threading-profile");
        if (threadingProfileStr != null) {
            ThreadingProfile profile = ThreadingProfile.valueOf(threadingProfileStr.toUpperCase(Locale.ENGLISH));
            common.getExecution().setThreadingProfile(profile);
        }
    }

    private static void handleTimeSource(ConfigurationCommon common, Element element) {
        DOMElementIterator nodeIterator = new DOMElementIterator(element.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("time-unit")) {
                String valueText = getRequiredAttribute(subElement, "value");
                if (valueText == null) {
                    throw new ConfigurationException("No value attribute supplied for time-unit element");
                }
                try {
                    TimeUnit timeUnit = TimeUnit.valueOf(valueText.toUpperCase(Locale.ENGLISH));
                    common.getTimeSource().setTimeUnit(timeUnit);
                } catch (Throwable t) {
                    throw new ConfigurationException("Value attribute for time-unit element invalid: " + t.getMessage(), t);
                }
            }
        }
    }

    private static void handleLogging(ConfigurationCommon common, Element element) {
        DOMElementIterator nodeIterator = new DOMElementIterator(element.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("query-plan")) {
                String valueText = getRequiredAttribute(subElement, "enabled");
                Boolean value = Boolean.parseBoolean(valueText);
                common.getLogging().setEnableQueryPlan(value);
            }
            if (subElement.getNodeName().equals("jdbc")) {
                String valueText = getRequiredAttribute(subElement, "enabled");
                Boolean value = Boolean.parseBoolean(valueText);
                common.getLogging().setEnableJDBC(value);
            }
        }
    }

    private static void handleVariantStream(ConfigurationCommon configuration, Element element) {
        ConfigurationCommonVariantStream variantStream = new ConfigurationCommonVariantStream();
        String varianceName = getRequiredAttribute(element, "name");

        if (element.getAttributes().getNamedItem("type-variance") != null) {
            String typeVar = element.getAttributes().getNamedItem("type-variance").getTextContent();
            ConfigurationCommonVariantStream.TypeVariance typeVarianceEnum;
            try {
                typeVarianceEnum = ConfigurationCommonVariantStream.TypeVariance.valueOf(typeVar.trim().toUpperCase(Locale.ENGLISH));
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

    private static void handleDatabaseRefs(ConfigurationCommon configuration, Element element) {
        String name = getRequiredAttribute(element, "name");
        ConfigurationCommonDBRef configDBRef = new ConfigurationCommonDBRef();
        configuration.addDatabaseReference(name, configDBRef);

        DOMElementIterator nodeIterator = new DOMElementIterator(element.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("datasource-connection")) {
                String lookup = getRequiredAttribute(subElement, "context-lookup-name");
                Properties properties = getProperties(subElement, "env-property");
                configDBRef.setDataSourceConnection(lookup, properties);
            }
            if (subElement.getNodeName().equals("datasourcefactory-connection")) {
                String className = getRequiredAttribute(subElement, "class-name");
                Properties properties = getProperties(subElement, "env-property");
                configDBRef.setDataSourceFactory(properties, className);
            } else if (subElement.getNodeName().equals("drivermanager-connection")) {
                String className = getRequiredAttribute(subElement, "class-name");
                String url = getRequiredAttribute(subElement, "url");
                String userName = getRequiredAttribute(subElement, "user");
                String password = getRequiredAttribute(subElement, "password");
                Properties properties = getProperties(subElement, "connection-arg");
                configDBRef.setDriverManagerConnection(className, url, userName, password, properties);
            } else if (subElement.getNodeName().equals("connection-lifecycle")) {
                String value = getRequiredAttribute(subElement, "value");
                configDBRef.setConnectionLifecycleEnum(ConfigurationCommonDBRef.ConnectionLifecycleEnum.valueOf(value.toUpperCase(Locale.ENGLISH)));
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
                ConfigurationCommonDBRef.ColumnChangeCaseEnum parsed = ConfigurationCommonDBRef.ColumnChangeCaseEnum.valueOf(value.toUpperCase(Locale.ENGLISH));
                configDBRef.setColumnChangeCase(parsed);
            } else if (subElement.getNodeName().equals("metadata-origin")) {
                String value = getRequiredAttribute(subElement, "value");
                ConfigurationCommonDBRef.MetadataOriginEnum parsed = ConfigurationCommonDBRef.MetadataOriginEnum.valueOf(value.toUpperCase(Locale.ENGLISH));
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
                CacheReferenceType refTypeEnum = CacheReferenceType.getDefault();
                if (subElement.getAttributes().getNamedItem("ref-type") != null) {
                    String refType = subElement.getAttributes().getNamedItem("ref-type").getTextContent();
                    refTypeEnum = CacheReferenceType.valueOf(refType.toUpperCase(Locale.ENGLISH));
                }
                configDBRef.setExpiryTimeCache(Double.parseDouble(maxAge), Double.parseDouble(purgeInterval), refTypeEnum);
            } else if (subElement.getNodeName().equals("lru-cache")) {
                String size = getRequiredAttribute(subElement, "size");
                configDBRef.setLRUCache(Integer.parseInt(size));
            }
        }
    }

    private static void handleVariable(ConfigurationCommon configuration, Element element) {
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

    private static void handleEventTypes(ConfigurationCommon configuration, Element element) {
        String name = getRequiredAttribute(element, "name");

        String optionalClassName = getOptionalAttribute(element, "class");
        if (optionalClassName != null) {
            configuration.addEventType(name, optionalClassName);
        }

        handleEventTypeDef(name, optionalClassName, configuration, element);
    }

    private static void handleEventTypeDef(String name, String optionalClassName, ConfigurationCommon configuration, Node parentNode) {
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

    private static void handleXMLDOM(String name, ConfigurationCommon configuration, Element xmldomElement) {
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

        ConfigurationCommonEventTypeXMLDOM xmlDOMEventTypeDesc = new ConfigurationCommonEventTypeXMLDOM();
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

    private static void handleAvro(String name, ConfigurationCommon configuration, Element element) {
        String schemaText = getOptionalAttribute(element, "schema-text");

        ConfigurationCommonEventTypeAvro avroEventTypeDesc = new ConfigurationCommonEventTypeAvro();
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

    private static void handleLegacy(String name, String className, ConfigurationCommon configuration, Element xmldomElement) {
        // Class name is required for legacy classes
        if (className == null) {
            throw new ConfigurationException("Required class name not supplied for legacy type definition");
        }

        String accessorStyle = getRequiredAttribute(xmldomElement, "accessor-style");
        String propertyResolution = getRequiredAttribute(xmldomElement, "property-resolution-style");
        String factoryMethod = getOptionalAttribute(xmldomElement, "factory-method");
        String copyMethod = getOptionalAttribute(xmldomElement, "copy-method");
        String startTimestampProp = getOptionalAttribute(xmldomElement, "start-timestamp-property-name");
        String endTimestampProp = getOptionalAttribute(xmldomElement, "end-timestamp-property-name");

        ConfigurationCommonEventTypeBean legacyDesc = new ConfigurationCommonEventTypeBean();
        if (accessorStyle != null) {
            legacyDesc.setAccessorStyle(AccessorStyle.valueOf(accessorStyle.toUpperCase(Locale.ENGLISH)));
        }
        if (propertyResolution != null) {
            legacyDesc.setPropertyResolutionStyle(PropertyResolutionStyle.valueOf(propertyResolution.toUpperCase(Locale.ENGLISH)));
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

    private static void handleMap(String name, ConfigurationCommon configuration, Element eventTypeElement) {
        ConfigurationCommonEventTypeMap config;
        String startTimestampProp = getOptionalAttribute(eventTypeElement, "start-timestamp-property-name");
        String endTimestampProp = getOptionalAttribute(eventTypeElement, "end-timestamp-property-name");
        Node superTypesList = eventTypeElement.getAttributes().getNamedItem("supertype-names");
        if (superTypesList != null || startTimestampProp != null || endTimestampProp != null) {
            config = new ConfigurationCommonEventTypeMap();
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

    private static void handleObjectArray(String name, ConfigurationCommon configuration, Element eventTypeElement) {
        ConfigurationCommonEventTypeObjectArray config;
        String startTimestampProp = getOptionalAttribute(eventTypeElement, "start-timestamp-property-name");
        String endTimestampProp = getOptionalAttribute(eventTypeElement, "end-timestamp-property-name");
        Node superTypesList = eventTypeElement.getAttributes().getNamedItem("supertype-names");
        if (superTypesList != null || startTimestampProp != null || endTimestampProp != null) {
            config = new ConfigurationCommonEventTypeObjectArray();
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

    private static void handleAutoImports(ConfigurationCommon configuration, Element element) {
        String name = getRequiredAttribute(element, "import-name");
        configuration.addImport(name);
    }

    private static void handleAutoImportAnnotations(ConfigurationCommon configuration, Element element) {
        String name = getRequiredAttribute(element, "import-name");
        configuration.addAnnotationImport(name);
    }

    private static void handleMethodReference(ConfigurationCommon configuration, Element element) {
        String className = getRequiredAttribute(element, "class-name");
        ConfigurationCommonMethodRef configMethodRef = new ConfigurationCommonMethodRef();
        configuration.addMethodRef(className, configMethodRef);

        DOMElementIterator nodeIterator = new DOMElementIterator(element.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("expiry-time-cache")) {
                String maxAge = getRequiredAttribute(subElement, "max-age-seconds");
                String purgeInterval = getRequiredAttribute(subElement, "purge-interval-seconds");
                CacheReferenceType refTypeEnum = CacheReferenceType.getDefault();
                if (subElement.getAttributes().getNamedItem("ref-type") != null) {
                    String refType = subElement.getAttributes().getNamedItem("ref-type").getTextContent();
                    refTypeEnum = CacheReferenceType.valueOf(refType.toUpperCase(Locale.ENGLISH));
                }
                configMethodRef.setExpiryTimeCache(Double.parseDouble(maxAge), Double.parseDouble(purgeInterval), refTypeEnum);
            } else if (subElement.getNodeName().equals("lru-cache")) {
                String size = getRequiredAttribute(subElement, "size");
                configMethodRef.setLRUCache(Integer.parseInt(size));
            }
        }
    }

    private static void handleEventMeta(ConfigurationCommon common, Element parentElement) {
        DOMElementIterator nodeIterator = new DOMElementIterator(parentElement.getChildNodes());
        while (nodeIterator.hasNext()) {
            Element subElement = nodeIterator.next();
            if (subElement.getNodeName().equals("class-property-resolution")) {
                Node styleNode = subElement.getAttributes().getNamedItem("style");
                if (styleNode != null) {
                    String styleText = styleNode.getTextContent();
                    PropertyResolutionStyle value = PropertyResolutionStyle.valueOf(styleText.toUpperCase(Locale.ENGLISH));
                    common.getEventMeta().setClassPropertyResolutionStyle(value);
                }

                Node accessorStyleNode = subElement.getAttributes().getNamedItem("accessor-style");
                if (accessorStyleNode != null) {
                    String accessorStyleText = accessorStyleNode.getTextContent();
                    AccessorStyle value = AccessorStyle.valueOf(accessorStyleText.toUpperCase(Locale.ENGLISH));
                    common.getEventMeta().setDefaultAccessorStyle(value);
                }
            }

            if (subElement.getNodeName().equals("event-representation")) {
                Node typeNode = subElement.getAttributes().getNamedItem("type");
                if (typeNode != null) {
                    String typeText = typeNode.getTextContent();
                    EventUnderlyingType value = EventUnderlyingType.valueOf(typeText.toUpperCase(Locale.ENGLISH));
                    common.getEventMeta().setDefaultEventRepresentation(value);
                }
            }

            if (subElement.getNodeName().equals("avro-settings")) {
                String enableAvroStr = getOptionalAttribute(subElement, "enable-avro");
                if (enableAvroStr != null) {
                    common.getEventMeta().getAvroSettings().setEnableAvro(Boolean.parseBoolean(enableAvroStr));
                }

                String enableNativeStringStr = getOptionalAttribute(subElement, "enable-native-string");
                if (enableNativeStringStr != null) {
                    common.getEventMeta().getAvroSettings().setEnableNativeString(Boolean.parseBoolean(enableNativeStringStr));
                }

                String enableSchemaDefaultNonNullStr = getOptionalAttribute(subElement, "enable-schema-default-nonnull");
                if (enableSchemaDefaultNonNullStr != null) {
                    common.getEventMeta().getAvroSettings().setEnableSchemaDefaultNonNull(Boolean.parseBoolean(enableSchemaDefaultNonNullStr));
                }

                String objectvalueTypewidenerFactoryClass = getOptionalAttribute(subElement, "objectvalue-typewidener-factory-class");
                if (objectvalueTypewidenerFactoryClass != null && objectvalueTypewidenerFactoryClass.trim().length() > 0) {
                    common.getEventMeta().getAvroSettings().setObjectValueTypeWidenerFactoryClass(objectvalueTypewidenerFactoryClass.trim());
                }

                String typeRepresentationMapperClass = getOptionalAttribute(subElement, "type-representation-mapper-class");
                common.getEventMeta().getAvroSettings().setTypeRepresentationMapperClass(typeRepresentationMapperClass);
            }
        }
    }
}
