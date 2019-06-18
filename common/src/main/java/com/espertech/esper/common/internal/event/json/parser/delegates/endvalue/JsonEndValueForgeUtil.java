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
package com.espertech.esper.common.internal.event.json.parser.delegates.endvalue;

import com.espertech.esper.common.client.EPException;

public class JsonEndValueForgeUtil {
    public static EPException handleNumberException(String name, Class boxedType, String value, NumberFormatException ex) {
        String innerMsg = ex.getMessage() == null ? "" : " " + ex.getMessage().replace("For", "for");
        return new EPException("Failed to parse json member name '" + name + "' as a " + boxedType.getSimpleName() + "-type from value '" + value + "': NumberFormatException" + innerMsg, ex);
    }

    public static EPException handleBooleanException(String name, String value) {
        return new EPException("Failed to parse json member name '" + name + "' as a boolean-type from value '" + value + "'");
    }

    public static EPException handleParseException(String name, Class boxedType, String value, Exception ex) {
        String innerMsg = ex.getMessage() == null ? "" : ex.getMessage();
        return new EPException("Failed to parse json member name '" + name + "' as a " + boxedType.getSimpleName() + "-type from value '" + value + "': " + innerMsg, ex);
    }
}
