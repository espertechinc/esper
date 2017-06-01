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
package com.espertech.esper.codegen.core;

import java.util.Map;

public class CodeGenerationHelper {

    public static StringBuilder appendClassName(StringBuilder builder, Class clazz, Class optionalTypeParam, Map<Class, String> imports) {
        if (!clazz.isArray()) {
            String assignedName = getAssignedName(clazz, imports);
            builder.append(assignedName);
            if (optionalTypeParam != null) {
                builder.append("<");
                appendClassName(builder, optionalTypeParam, null, imports);
                builder.append(">");
            }
            return builder;
        }
        appendClassName(builder, clazz.getComponentType(), null, imports);
        builder.append("[]");
        return builder;
    }

    private static String getAssignedName(Class clazz, Map<Class, String> imports) {
        String assigned = imports.get(clazz);
        if (assigned != null) {
            return assigned;
        }
        return clazz.getName();
    }
}
