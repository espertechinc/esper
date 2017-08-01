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
package com.espertech.esper.codegen.model.expression;

import java.util.Map;
import java.util.Set;

import static com.espertech.esper.codegen.core.CodeGenerationHelper.appendClassName;

public class CodegenExpressionEnumValue implements CodegenExpression {
    private final Class enumType;
    private final String enumValue;

    public CodegenExpressionEnumValue(Class enumType, String enumValue) {
        this.enumType = enumType;
        this.enumValue = enumValue;
    }

    public void render(StringBuilder builder, Map<Class, String> imports) {
        appendClassName(builder, enumType, null, imports);
        builder.append(".");
        builder.append(enumValue);
    }

    public void mergeClasses(Set<Class> classes) {
        classes.add(enumType);
    }
}
