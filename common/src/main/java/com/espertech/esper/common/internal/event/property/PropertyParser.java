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
package com.espertech.esper.common.internal.event.property;

import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.internal.event.propertyparser.PropertyParserNoDep;

import java.io.StringWriter;

public class PropertyParser {
    public static Property parseAndWalkLaxToSimple(String propertyName) {
        try {
            return PropertyParserNoDep.parseAndWalkLaxToSimple(propertyName, false);
        } catch (PropertyAccessException p) {
            return new SimpleProperty(propertyName);
        }
    }

    public static String unescapeBacktickForProperty(String unescapedPropertyName) {
        if (unescapedPropertyName.startsWith("`") && unescapedPropertyName.endsWith("`")) {
            return unescapedPropertyName.substring(1, unescapedPropertyName.length() - 1);
        }

        if (!unescapedPropertyName.contains("`")) {
            return unescapedPropertyName;
        }

        // parse and render
        Property property = PropertyParser.parseAndWalkLaxToSimple(unescapedPropertyName);
        if (property instanceof NestedProperty) {
            StringWriter writer = new StringWriter();
            property.toPropertyEPL(writer);
            return writer.toString();
        }

        return unescapedPropertyName;
    }

    public static Property parseAndWalk(String propertyNested, boolean isRootedDynamic) {
        return PropertyParserNoDep.parseAndWalkLaxToSimple(propertyNested, isRootedDynamic);
    }

    public static boolean isPropertyDynamic(Property prop) {
        if (prop instanceof DynamicProperty) {
            return true;
        }
        if (!(prop instanceof NestedProperty)) {
            return false;
        }
        NestedProperty nestedProperty = (NestedProperty) prop;
        for (Property property : nestedProperty.getProperties()) {
            if (isPropertyDynamic(property)) {
                return true;
            }
        }
        return false;
    }
}
