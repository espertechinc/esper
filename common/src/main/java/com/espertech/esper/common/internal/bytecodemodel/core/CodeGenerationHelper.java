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
package com.espertech.esper.common.internal.bytecodemodel.core;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeClassParameterized;

import java.util.Map;

public class CodeGenerationHelper {

    public static StringBuilder appendClassName(StringBuilder builder, Class clazz, Map<Class, String> imports) {
        if (!clazz.isArray()) {
            String assignedName = getAssignedName(clazz, imports);
            builder.append(assignedName);
            return builder;
        }
        appendClassName(builder, clazz.getComponentType(), imports);
        builder.append("[]");
        return builder;
    }

    public static StringBuilder appendClassName(StringBuilder builder, EPTypeClass clazz, Map<Class, String> imports) {
        if (!clazz.getType().isArray()) {
            String assignedName = getAssignedName(clazz.getType(), imports);
            builder.append(assignedName);
            if (clazz instanceof EPTypeClassParameterized) {
                EPTypeClassParameterized parameterized = (EPTypeClassParameterized) clazz;
                builder.append("<");
                String delimiter = "";
                for (EPTypeClass param : parameterized.getParameters()) {
                    builder.append(delimiter);
                    appendClassName(builder, param, imports);
                    delimiter = ",";
                }
                builder.append(">");
            }
            return builder;
        }
        appendClassName(builder, clazz.getType().getComponentType(), imports);
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
