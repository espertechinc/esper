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
import com.espertech.esper.client.util.ClassForNameProviderDefault;
import com.espertech.esper.event.EventAdapterException;
import com.espertech.esper.util.JavaClassHelper;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration object for enabling the engine to process events represented as Axiom OMNode document nodes.
 * <p>
 * Use this class to configure the engine for processing of Axiom OMNode objects that represent events
 * and contain all the data for event properties used by statements.
 * <p>
 * Minimally required is the root element name which allows the engine to map the document
 * to the event type that has been named in an EPL or pattern statement.
 * <p>
 * Event properties that are results of XPath expressions can be made known to the engine via this class.
 * For XPath expressions that must refer to namespace prefixes those prefixes and their
 * namespace name must be supplied to the engine. A default namespace can be supplied as well.
 */
public class ConfigurationEventTypeAxiom implements Serializable {
    private String rootElementName;

    // Root element namespace.
    // Used to find root element in schema. Useful and required in the case where the root element exists in
    // multiple namespaces.
    private String rootElementNamespace;

    // Default name space.
    // For XPath expression evaluation.
    private String defaultNamespace;

    private Map<String, XPathPropertyDesc> xPathProperties;
    private Map<String, String> namespacePrefixes;

    private boolean resolvePropertiesAbsolute;

    /**
     * Ctor.
     */
    public ConfigurationEventTypeAxiom() {
        xPathProperties = new HashMap<String, XPathPropertyDesc>();
        namespacePrefixes = new HashMap<String, String>();
        resolvePropertiesAbsolute = true;
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
     * Returns a map of property name and descriptor for XPath-expression properties.
     *
     * @return XPath property information
     */
    public Map<String, XPathPropertyDesc> getXPathProperties() {
        return xPathProperties;
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
            try {
                castToTypeClass = JavaClassHelper.getClassForSimpleName(castToType, ClassForNameProviderDefault.INSTANCE);
            } catch (EventAdapterException ex) {
                throw new ConfigurationException("Invalid cast-to type for xpath expression named '" + name + "': " + ex.getMessage());
            }
        }

        XPathPropertyDesc desc = new XPathPropertyDesc(name, xpath, type, castToTypeClass);
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
     * Indicates whether properties are compiled into absolute or deep XPath expressions (see setter method for more detail).
     *
     * @return true for absolute properties, false for deep properties
     */
    public boolean isResolvePropertiesAbsolute() {
        return resolvePropertiesAbsolute;
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
     * @param resolvePropertiesAbsolute true for absolute XPath for properties (default), false for deep XPath
     */
    public void setResolvePropertiesAbsolute(boolean resolvePropertiesAbsolute) {
        this.resolvePropertiesAbsolute = resolvePropertiesAbsolute;
    }

    /**
     * Descriptor class for event properties that are resolved via XPath-expression.
     */
    public static class XPathPropertyDesc implements Serializable {
        private String name;
        private String xpath;
        private QName type;
        private Class optionalCastToType;

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
    }
}
