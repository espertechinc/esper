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

import com.espertech.esper.client.util.ClassForNameProviderDefault;
import com.espertech.esper.util.JavaClassHelper;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuration object for enabling the engine to process events represented as XML DOM document nodes.
 * <p>
 * Use this class to configure the engine for processing of XML DOM objects that represent events
 * and contain all the data for event properties used by statements.
 * </p>
 * <p>
 * Minimally required is the root element name which allows the engine to map the document
 * to the event type that has been named in an EPL or pattern statement.
 * </p>
 * <p>
 * Event properties that are results of XPath expressions can be made known to the engine via this class.
 * For XPath expressions that must refer to namespace prefixes those prefixes and their
 * namespace name must be supplied to the engine. A default namespace can be supplied as well.
 * </p>
 * <p>
 * By supplying a schema resource the engine can interrogate the schema, allowing the engine to
 * verify event properties and return event properties in the type defined by the schema.
 * When a schema resource is supplied, the optional root element namespace defines the namespace in case the
 * root element name occurs in multiple namespaces.
 * </p>
 */
public class ConfigurationEventTypeXMLDOM implements Serializable {
    private String rootElementName;

    // Root element namespace.
    // Used to find root element in schema. Useful and required in the case where the root element exists in
    // multiple namespaces.
    private String rootElementNamespace;

    // Default name space.
    // For XPath expression evaluation.
    private String defaultNamespace;

    private String schemaResource;
    private String schemaText;
    private Map<String, XPathPropertyDesc> xPathProperties;
    private Map<String, String> namespacePrefixes;

    private boolean isXPathPropertyExpr;
    private boolean isXPathResolvePropertiesAbsolute;
    private boolean isEventSenderValidatesRoot;
    private boolean isAutoFragment;

    private String xPathFunctionResolver;
    private String xPathVariableResolver;

    private String startTimestampPropertyName;
    private String endTimestampPropertyName;

    private boolean updateStoredType;    // For use with EsperHA to enable new type configuration to overwrite an existing type configuration
    private static final long serialVersionUID = -7488596902855838072L;

    /**
     * Ctor.
     */
    public ConfigurationEventTypeXMLDOM() {
        xPathProperties = new LinkedHashMap<String, XPathPropertyDesc>();
        namespacePrefixes = new HashMap<String, String>();
        isXPathResolvePropertiesAbsolute = true;
        isXPathPropertyExpr = false;
        isEventSenderValidatesRoot = true;
        isAutoFragment = true;
    }

    /**
     * Returns the root element name.
     *
     * @return root element name
     */
    public String getRootElementName() {
        return rootElementName;
    }

    /**
     * Sets the root element name.
     *
     * @param rootElementName is the name of the root element
     */
    public void setRootElementName(String rootElementName) {
        this.rootElementName = rootElementName;
    }

    /**
     * Returns the root element namespace.
     *
     * @return root element namespace
     */
    public String getRootElementNamespace() {
        return rootElementNamespace;
    }

    /**
     * Sets the root element namespace.
     *
     * @param rootElementNamespace is the namespace for the root element
     */
    public void setRootElementNamespace(String rootElementNamespace) {
        this.rootElementNamespace = rootElementNamespace;
    }

    /**
     * Returns the default namespace.
     *
     * @return default namespace
     */
    public String getDefaultNamespace() {
        return defaultNamespace;
    }

    /**
     * Sets the default namespace.
     *
     * @param defaultNamespace is the default namespace
     */
    public void setDefaultNamespace(String defaultNamespace) {
        this.defaultNamespace = defaultNamespace;
    }

    /**
     * Returns the schema resource.
     *
     * @return schema resource
     */
    public String getSchemaResource() {
        return schemaResource;
    }

    /**
     * Sets the schema resource.
     *
     * @param schemaResource is the schema resource
     */
    public void setSchemaResource(String schemaResource) {
        this.schemaResource = schemaResource;
    }

    /**
     * Returns the schema text, if provided instead of a schema resource, this call returns the actual text of the schema document.
     * <p>
     * Set a schema text first. This call will not resolve the schema resource to a text.
     *
     * @return schema text, if provided, or null value
     */
    public String getSchemaText() {
        return schemaText;
    }

    /**
     * Sets the schema text, for use when the schema resource is impractical and when providing the actual text
     * of the schema instead.
     *
     * @param schemaText schema text is the actual content of an XSD schema file as a string,
     *                   provide instead of a schema resource name
     */
    public void setSchemaText(String schemaText) {
        this.schemaText = schemaText;
    }

    /**
     * Returns a map of property name and descriptor for XPath-expression properties.
     *
     * @return XPath property information
     */
    public Map<String, XPathPropertyDesc> getXPathProperties() {
        return xPathProperties;
    }

    /**
     * Returns false to indicate that property expressions are evaluated by the DOM-walker
     * implementation (the default), or true to  indicate that property expressions are rewritten into XPath expressions.
     *
     * @return indicator how property expressions are evaluated
     */
    public boolean isXPathPropertyExpr() {
        return isXPathPropertyExpr;
    }

    /**
     * Set to false to indicate that property expressions are evaluated by the DOM-walker
     * implementation (the default), or set to true to indicate that property expressions are rewritten into XPath expressions.
     *
     * @param xPathPropertyExpr indicator how property expressions are evaluated
     */
    public void setXPathPropertyExpr(boolean xPathPropertyExpr) {
        isXPathPropertyExpr = xPathPropertyExpr;
    }

    /**
     * Returns true to indicate that an {@link EventSender} returned for this event type validates
     * the root document element name against the one configured (the default), or false to not validate the root document
     * element name as configured.
     *
     * @return true for validation of root document element name by event sender, false for no validation
     */
    public boolean isEventSenderValidatesRoot() {
        return isEventSenderValidatesRoot;
    }

    /**
     * Set to true to indicate that an {@link EventSender} returned for this event type validates
     * the root document element name against the one configured (the default), or false to not validate the root document
     * element name as configured.
     *
     * @param eventSenderValidatesRoot true for validation of root document element name by event sender, false for no validation
     */
    public void setEventSenderValidatesRoot(boolean eventSenderValidatesRoot) {
        isEventSenderValidatesRoot = eventSenderValidatesRoot;
    }

    /**
     * Set to true (the default) to look up or create event types representing fragments of an XML document
     * automatically upon request for fragment event type information; Or false when only explicit
     * properties may return fragments.
     *
     * @return indicator whether to allow splitting-up (fragmenting) properties (nodes) in an document
     */
    public boolean isAutoFragment() {
        return isAutoFragment;
    }

    /**
     * Set to true (the default) to look up or create event types representing fragments of an XML document
     * automatically upon request for fragment event type information; Or false when only explicit
     * properties may return fragments.
     *
     * @param autoFragment indicator whether to allow splitting-up (fragmenting) properties (nodes) in an document
     */
    public void setAutoFragment(boolean autoFragment) {
        isAutoFragment = autoFragment;
    }

    /**
     * Adds an event property for which the engine uses the supplied XPath expression against
     * a DOM document node to resolve a property value.
     *
     * @param name  of the event property
     * @param xpath is an arbitrary xpath expression
     * @param type  is a constant obtained from javax.xml.xpath.XPathConstants. Typical values are
     *              XPathConstants.NUMBER, STRING and BOOLEAN.
     */
    public void addXPathProperty(String name, String xpath, QName type) {
        XPathPropertyDesc desc = new XPathPropertyDesc(name, xpath, type);
        xPathProperties.put(name, desc);
    }

    /**
     * Adds an event property for which the engine uses the supplied XPath expression against
     * a DOM document node to resolve a property value.
     *
     * @param name       of the event property
     * @param xpath      is an arbitrary xpath expression
     * @param type       is a constant obtained from javax.xml.xpath.XPathConstants. Typical values are
     *                   XPathConstants.NUMBER, STRING and BOOLEAN.
     * @param castToType is the type name of the type that the return value of the xpath expression is casted to
     */
    public void addXPathProperty(String name, String xpath, QName type, String castToType) {
        Class castToTypeClass = null;

        if (castToType != null) {
            boolean isArray = false;
            if (castToType.trim().endsWith("[]")) {
                isArray = true;
                castToType = castToType.replace("[]", "");
            }

            castToTypeClass = JavaClassHelper.getClassForSimpleName(castToType, ClassForNameProviderDefault.INSTANCE);
            if (castToTypeClass == null) {
                throw new ConfigurationException("Invalid cast-to type for xpath expression named '" + name + "', the type is not recognized");
            }

            if (isArray) {
                castToTypeClass = Array.newInstance(castToTypeClass, 0).getClass();
            }
        }

        XPathPropertyDesc desc = new XPathPropertyDesc(name, xpath, type, castToTypeClass);
        xPathProperties.put(name, desc);
    }

    /**
     * Adds an event property for which the engine uses the supplied XPath expression against
     * a DOM document node to resolve a property value.
     *
     * @param name          of the event property
     * @param xpath         is an arbitrary xpath expression
     * @param type          is a constant obtained from javax.xml.xpath.XPathConstants. Typical values are
     *                      XPathConstants.NODE and XPathConstants.NODESET.
     * @param eventTypeName is the name of another event type that represents the XPath nodes
     */
    public void addXPathPropertyFragment(String name, String xpath, QName type, String eventTypeName) {
        if ((type != XPathConstants.NODE) && (type != XPathConstants.NODESET)) {
            throw new IllegalArgumentException("XPath property for fragments requires an Node or Nodeset (XPathConstants.NODE/NODESET) return value for property '" + name + "'");
        }
        XPathPropertyDesc desc = new XPathPropertyDesc(name, xpath, type, eventTypeName);
        xPathProperties.put(name, desc);
    }

    /**
     * Returns the namespace prefixes in a map of prefix as key and namespace name as value.
     *
     * @return namespace prefixes
     */
    public Map<String, String> getNamespacePrefixes() {
        return namespacePrefixes;
    }

    /**
     * Add a prefix and namespace name for use in XPath expressions refering to that prefix.
     *
     * @param prefix    is the prefix of the namespace
     * @param namespace is the namespace name
     */
    public void addNamespacePrefix(String prefix, String namespace) {
        namespacePrefixes.put(prefix, namespace);
    }

    /**
     * Add prefixes and namespace names for use in XPath expressions refering to that prefix.
     *
     * @param prefixNamespaceMap map of prefixes and namespaces
     */
    public void addNamespacePrefixes(Map<String, String> prefixNamespaceMap) {
        namespacePrefixes.putAll(prefixNamespaceMap);
    }

    /**
     * Indicates whether properties are compiled into absolute or deep XPath expressions (see setter method for more detail).
     *
     * @return true for absolute properties, false for deep properties
     */
    public boolean isXPathResolvePropertiesAbsolute() {
        return isXPathResolvePropertiesAbsolute;
    }

    /**
     * When set to true (the default), indicates that when properties are compiled to XPath expressions that the
     * compilation should generate an absolute XPath expression such as "/getQuote/request" for the
     * simple request property, or "/getQuote/request/symbol" for a "request.symbol" nested property,
     * wherein the root element node is "getQuote".
     * <p>
     * When set to false, indicates that when properties are compiled to XPath expressions that the
     * compilation should generate a deep XPath expression such as "//symbol" for the
     * simple symbol property, or "//request/symbol" for a "request.symbol" nested property.
     *
     * @param xPathResolvePropertiesAbsolute true for absolute XPath for properties (default), false for deep XPath
     */
    public void setXPathResolvePropertiesAbsolute(boolean xPathResolvePropertiesAbsolute) {
        this.isXPathResolvePropertiesAbsolute = xPathResolvePropertiesAbsolute;
    }

    /**
     * Returns the class name of the XPath function resolver to be assigned to the XPath factory instance
     * upon type initialization.
     *
     * @return class name of xpath function resolver, or null if none set
     */
    public String getXPathFunctionResolver() {
        return xPathFunctionResolver;
    }

    /**
     * Sets the class name of the XPath function resolver to be assigned to the XPath factory instance
     * upon type initialization.
     *
     * @param xPathFunctionResolver class name of xpath function resolver, or null if none set
     */
    public void setXPathFunctionResolver(String xPathFunctionResolver) {
        this.xPathFunctionResolver = xPathFunctionResolver;
    }

    /**
     * Returns the class name of the XPath variable resolver to be assigned to the XPath factory instance
     * upon type initialization.
     *
     * @return class name of xpath function resolver, or null if none set
     */
    public String getXPathVariableResolver() {
        return xPathVariableResolver;
    }

    /**
     * Sets the class name of the XPath variable resolver to be assigned to the XPath factory instance
     * upon type initialization.
     *
     * @param xPathVariableResolver class name of xpath function resolver, or null if none set
     */
    public void setXPathVariableResolver(String xPathVariableResolver) {
        this.xPathVariableResolver = xPathVariableResolver;
    }

    /**
     * Indicator for use with EsperHA, false by default to indicate that stored type information takes
     * precedence over configuration type information provided at engine initialization time. Set to true to indicate that
     * configuration type information takes precedence over stored type information.
     *
     * @return indicator is false (the default) to indicate that stored type information takes precedence over configuration type information
     */
    public boolean isUpdateStoredType() {
        return updateStoredType;
    }

    /**
     * Indicator for use with EsperHA, false by default to indicate that stored type information takes
     * precedence over configuration type information provided at engine initialization time. Set to true to indicate that
     * configuration type information takes precedence over stored type information.
     * <p>
     * When setting this flag to true care should be taken about the compatibility of the supplied XML type
     * configuration information and the existing EPL statements and stored events, if any. For more information
     * please consult {@link ConfigurationOperations#replaceXMLEventType(String, ConfigurationEventTypeXMLDOM)}.
     *
     * @param updateStoredType set to false (the default) to indicate that stored type information takes precedence over configuration type information
     */
    public void setUpdateStoredType(boolean updateStoredType) {
        this.updateStoredType = updateStoredType;
    }

    /**
     * Descriptor class for event properties that are resolved via XPath-expression.
     */
    public static class XPathPropertyDesc implements Serializable {
        private String name;
        private String xpath;
        private QName type;
        private Class optionalCastToType;
        private String optionaleventTypeName;
        private static final long serialVersionUID = -4141721949296588319L;

        /**
         * Ctor.
         *
         * @param name  is the event property name
         * @param xpath is an arbitrary XPath expression
         * @param type  is a javax.xml.xpath.XPathConstants constant
         */
        public XPathPropertyDesc(String name, String xpath, QName type) {
            this.name = name;
            this.xpath = xpath;
            this.type = type;
        }

        /**
         * Ctor.
         *
         * @param name               is the event property name
         * @param xpath              is an arbitrary XPath expression
         * @param type               is a javax.xml.xpath.XPathConstants constant
         * @param optionalCastToType if non-null then the return value of the xpath expression is cast to this value
         */
        public XPathPropertyDesc(String name, String xpath, QName type, Class optionalCastToType) {
            this.name = name;
            this.xpath = xpath;
            this.type = type;
            this.optionalCastToType = optionalCastToType;
        }

        /**
         * Ctor.
         *
         * @param name          is the event property name
         * @param xpath         is an arbitrary XPath expression
         * @param type          is a javax.xml.xpath.XPathConstants constant
         * @param eventTypeName the name of an event type that represents the fragmented property value
         */
        public XPathPropertyDesc(String name, String xpath, QName type, String eventTypeName) {
            this.name = name;
            this.xpath = xpath;
            this.type = type;
            this.optionaleventTypeName = eventTypeName;
        }

        /**
         * Returns the event property name.
         *
         * @return event property name
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the XPath expression.
         *
         * @return XPath expression
         */
        public String getXpath() {
            return xpath;
        }

        /**
         * Returns the javax.xml.xpath.XPathConstants constant representing the event property type.
         *
         * @return type infomation
         */
        public QName getType() {
            return type;
        }

        /**
         * Returns the class that the return value of the xpath expression is cast to, or null if no casting.
         *
         * @return class to cast result of xpath expression to
         */
        public Class getOptionalCastToType() {
            return optionalCastToType;
        }

        /**
         * Returns the event type name assigned to the explicit property.
         *
         * @return type name
         */
        public String getOptionaleventTypeName() {
            return optionaleventTypeName;
        }
    }

    /**
     * Returns the property name of the property providing the start timestamp value.
     *
     * @return start timestamp property name
     */
    public String getStartTimestampPropertyName() {
        return startTimestampPropertyName;
    }

    /**
     * Sets the property name of the property providing the start timestamp value.
     *
     * @param startTimestampPropertyName start timestamp property name
     */
    public void setStartTimestampPropertyName(String startTimestampPropertyName) {
        this.startTimestampPropertyName = startTimestampPropertyName;
    }

    /**
     * Returns the property name of the property providing the end timestamp value.
     *
     * @return end timestamp property name
     */
    public String getEndTimestampPropertyName() {
        return endTimestampPropertyName;
    }

    /**
     * Sets the property name of the property providing the end timestamp value.
     *
     * @param endTimestampPropertyName start timestamp property name
     */
    public void setEndTimestampPropertyName(String endTimestampPropertyName) {
        this.endTimestampPropertyName = endTimestampPropertyName;
    }

    public boolean equals(Object otherObj) {
        if (!(otherObj instanceof ConfigurationEventTypeXMLDOM)) {
            return false;
        }

        ConfigurationEventTypeXMLDOM other = (ConfigurationEventTypeXMLDOM) otherObj;
        if (!(other.rootElementName.equals(rootElementName))) {
            return false;
        }

        if (((other.rootElementNamespace == null) && (rootElementNamespace != null)) ||
                ((other.rootElementNamespace != null) && (rootElementNamespace == null))) {
            return false;
        }
        if ((other.rootElementNamespace != null) && (rootElementNamespace != null)) {
            return rootElementNamespace.equals(other.rootElementNamespace);
        }
        return true;
    }

    public int hashCode() {
        return rootElementName.hashCode();
    }
}
