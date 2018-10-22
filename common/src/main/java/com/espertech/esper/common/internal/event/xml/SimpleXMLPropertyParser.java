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
package com.espertech.esper.common.internal.event.xml;

import com.espertech.esper.common.internal.event.property.*;

/**
 * Parses event property names and transforms to XPath expressions. Supports
 * nested, indexed and mapped event properties.
 */
public class SimpleXMLPropertyParser {
    /**
     * Return the xPath corresponding to the given property.
     * The propertyName String may be simple, nested, indexed or mapped.
     *
     * @param property                    is the property
     * @param rootElementName             is the name of the root element for generating the XPath expression
     * @param defaultNamespacePrefix      is the prefix of the default namespace
     * @param isResolvePropertiesAbsolute is true to indicate to resolve XPath properties as absolute props
     *                                    or relative props
     * @return xpath expression
     */
    public static String walk(Property property, String rootElementName, String defaultNamespacePrefix, boolean isResolvePropertiesAbsolute) {
        StringBuilder xPathBuf = new StringBuilder();
        xPathBuf.append('/');
        if (isResolvePropertiesAbsolute) {
            if (defaultNamespacePrefix != null) {
                xPathBuf.append(defaultNamespacePrefix);
                xPathBuf.append(':');
            }
            xPathBuf.append(rootElementName);
        }

        if (!(property instanceof NestedProperty)) {
            xPathBuf.append(makeProperty(property, defaultNamespacePrefix));
        } else {
            NestedProperty nestedProperty = (NestedProperty) property;
            for (Property propertyNested : nestedProperty.getProperties()) {
                xPathBuf.append(makeProperty(propertyNested, defaultNamespacePrefix));
            }
        }

        return xPathBuf.toString();
    }

    private static String makeProperty(Property property, String defaultNamespacePrefix) {
        String prefix = "";
        if (defaultNamespacePrefix != null) {
            prefix = defaultNamespacePrefix + ":";
        }

        String unescapedIdent = property.getPropertyNameAtomic();
        if (property instanceof PropertyWithIndex) {
            int index = ((PropertyWithIndex) property).getIndex();
            int xPathPosition = index + 1;
            return '/' + prefix + unescapedIdent + "[position() = " + xPathPosition + ']';
        }

        if (property instanceof MappedProperty) {
            String key = ((PropertyWithKey) property).getKey();
            return '/' + prefix + unescapedIdent + "[@id='" + key + "']";
        }

        return '/' + prefix + unescapedIdent;
    }
}
