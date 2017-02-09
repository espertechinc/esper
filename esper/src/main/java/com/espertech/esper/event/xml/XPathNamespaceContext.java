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
package com.espertech.esper.event.xml;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Provides the namespace context information for compiling XPath expressions.
 */
public class XPathNamespaceContext implements NamespaceContext {

    //namespace to prefix
    private Map<String, String> namespaces;

    //prefix to namespace
    private Map<String, String> prefix;

    private String defaultNamespace;

    /**
     * Ctor.
     */
    public XPathNamespaceContext() {
        super();
        namespaces = new HashMap<String, String>();
        prefix = new HashMap<String, String>();
    }

    public String getNamespaceURI(String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("prefix can't be null");
        }
        if (prefix.equals(XMLConstants.DEFAULT_NS_PREFIX)) {
            return defaultNamespace;
        }
        if (prefix.equals(XMLConstants.XML_NS_PREFIX)) {
            return XMLConstants.XML_NS_URI;
        }
        if (prefix.equals(XMLConstants.XMLNS_ATTRIBUTE)) {
            return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
        }

        String namespace = namespaces.get(prefix);
        if (namespace == null)
            return XMLConstants.NULL_NS_URI;

        return namespace;
    }

    public String getPrefix(String namespaceURI) {
        if (namespaceURI == null) {
            return null;
        }
        if (defaultNamespace != null) {
            if (namespaceURI.equals(defaultNamespace)) {
                return XMLConstants.DEFAULT_NS_PREFIX;
            }
        }
        if (namespaceURI.equals(XMLConstants.XML_NS_URI)) {
            return XMLConstants.XML_NS_PREFIX;
        }
        if (namespaceURI.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
            return XMLConstants.XMLNS_ATTRIBUTE;
        }
        return prefix.get(namespaceURI);
    }

    public Iterator getPrefixes(String namespaceURI) {
        throw new RuntimeException("Not Implemented");
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
     * Add a namespace prefix and namespace name to context.
     *
     * @param prefix - namespace prefix
     * @param uri    - namespace name to add
     */
    public void addPrefix(String prefix, String uri) {
        namespaces.put(prefix, uri);
        this.prefix.put(uri, prefix);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder("XPathNamespaceContext default namespace '" + defaultNamespace + "' maps {");
        String delimiter = "";
        for (Map.Entry<String, String> entry : prefix.entrySet()) {
            builder.append(delimiter);
            builder.append(entry.getKey());
            builder.append("=");
            builder.append(entry.getValue());
            delimiter = ",";
        }
        builder.append("}");
        return builder.toString();
    }
}
