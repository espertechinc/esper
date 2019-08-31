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

import com.espertech.esper.common.client.EventSender;
import com.espertech.esper.common.client.configuration.ConfigurationException;
import com.espertech.esper.common.client.util.ClassForNameProviderDefault;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenSetterBuilder;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenSetterBuilderItemConsumer;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

/**
 * Configuration object for enabling the runtimeto process events represented as XML DOM document nodes.
 * <p>
 * Use this class to configure the runtimefor processing of XML DOM objects that represent events
 * and contain all the data for event properties used by statements.
 * </p>
 * <p>
 * Minimally required is the root element name which allows the runtimeto map the document
 * to the event type that has been named in an EPL or pattern statement.
 * </p>
 * <p>
 * Event properties that are results of XPath expressions can be made known to the runtimevia this class.
 * For XPath expressions that must refer to namespace prefixes those prefixes and their
 * namespace name must be supplied to the runtime. A default namespace can be supplied as well.
 * </p>
 * <p>
 * By supplying a schema resource the runtimecan interrogate the schema, allowing the runtimeto
 * verify event properties and return event properties in the type defined by the schema.
 * When a schema resource is supplied, the optional root element namespace defines the namespace in case the
 * root element name occurs in multiple namespaces.
 * </p>
 */
public class ConfigurationCommonEventTypeXMLDOM implements Serializable {
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

    private static final long serialVersionUID = -7488596902855838072L;

    /**
     * Ctor.
     */
    public ConfigurationCommonEventTypeXMLDOM() {
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
     * Set to true (the default) to indicate that an {@link EventSender} returned for this event type validates
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
     * Adds an event property for which the runtimeuses the supplied XPath expression against
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
     * Adds an event property for which the runtimeuses the supplied XPath expression against
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
     * Adds an event property for which the runtimeuses the supplied XPath expression against
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

    public void setNamespacePrefixes(Map<String, String> namespacePrefixes) {
        this.namespacePrefixes = namespacePrefixes;
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

    public void setXPathProperties(Map<String, XPathPropertyDesc> xPathProperties) {
        this.xPathProperties = xPathProperties;
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

        public XPathPropertyDesc() {
        }

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

        public void setName(String name) {
            this.name = name;
        }

        public void setXpath(String xpath) {
            this.xpath = xpath;
        }

        public void setType(QName type) {
            this.type = type;
        }

        public void setOptionalCastToType(Class optionalCastToType) {
            this.optionalCastToType = optionalCastToType;
        }

        public void setOptionaleventTypeName(String optionaleventTypeName) {
            this.optionaleventTypeName = optionaleventTypeName;
        }

        public void setOptionalEventTypeName(String optionaleventTypeName) {
            this.optionaleventTypeName = optionaleventTypeName;
        }

        public CodegenExpression toExpression(CodegenMethodScope parent, CodegenClassScope scope) {
            return new CodegenSetterBuilder(XPathPropertyDesc.class, XPathPropertyDesc.class, "desc", parent, scope)
                .constant("name", name)
                .expression("type", newInstance(QName.class, constant(type.getNamespaceURI()), constant(type.getLocalPart()), constant(type.getPrefix())))
                .constant("xpath", xpath)
                .constant("optionaleventTypeName", optionaleventTypeName)
                .constant("optionalCastToType", optionalCastToType)
                .build();
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
        if (!(otherObj instanceof ConfigurationCommonEventTypeXMLDOM)) {
            return false;
        }

        ConfigurationCommonEventTypeXMLDOM other = (ConfigurationCommonEventTypeXMLDOM) otherObj;
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

    public CodegenExpression toExpression(CodegenMethodScope parent, CodegenClassScope scope) {
        CodegenSetterBuilderItemConsumer<XPathPropertyDesc> xPathBuild = (o, parentXPath, scopeXPath) -> o.toExpression(parentXPath, scopeXPath);
        return new CodegenSetterBuilder(ConfigurationCommonEventTypeXMLDOM.class, ConfigurationCommonEventTypeXMLDOM.class, "xmlconfig", parent, scope)
            .constant("rootElementName", rootElementName)
            .map("xPathProperties", xPathProperties, xPathBuild)
            .mapOfConstants("namespacePrefixes", namespacePrefixes)
            .constant("schemaResource", schemaResource)
            .constant("schemaText", schemaText)
            .constant("eventSenderValidatesRoot", isEventSenderValidatesRoot)
            .constant("autoFragment", isAutoFragment)
            .constant("xPathPropertyExpr", isXPathPropertyExpr)
            .constant("xPathFunctionResolver", xPathFunctionResolver)
            .constant("xPathVariableResolver", xPathVariableResolver)
            .constant("xPathResolvePropertiesAbsolute", isXPathResolvePropertiesAbsolute)
            .constant("defaultNamespace", defaultNamespace)
            .constant("rootElementNamespace", rootElementNamespace)
            .constant("startTimestampPropertyName", startTimestampPropertyName)
            .constant("endTimestampPropertyName", endTimestampPropertyName)
            .build();
    }
}
