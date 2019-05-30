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
package com.espertech.esper.common.internal.bytecodemodel.model.expression;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationHelper.appendClassName;

public class CodegenExpressionEnumOrPublicConstantValue implements CodegenExpression {
    private final Class enumType;
    private final String enumTypeString;
    private final String enumValue;

    public CodegenExpressionEnumOrPublicConstantValue(Class enumType, String enumValue) {
        this.enumType = enumType;
        this.enumTypeString = null;
        this.enumValue = enumValue;
    }

    public CodegenExpressionEnumOrPublicConstantValue(String enumTypeString, String enumValue) {
        this.enumTypeString = enumTypeString;
        this.enumValue = enumValue;
        this.enumType = null;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        if (enumType != null) {
            appendClassName(builder, enumType, null, imports);
        } else {
            builder.append(enumTypeString);
        }
        builder.append(".");
        builder.append(enumValue);
    }

    public void mergeClasses(Set<Class> classes) {
        classes.add(enumType);
    }

    public void traverseExpressions(Consumer<CodegenExpression> consumer) {
    }
}
