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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenField;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class CodegenExpressionField implements CodegenExpression {
    private final CodegenField field;

    public CodegenExpressionField(CodegenField field) {
        if (field == null) {
            throw new IllegalArgumentException("Null field");
        }
        this.field = field;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        field.render(builder);
    }

    public void mergeClasses(Set<Class> classes) {
        field.mergeClasses(classes);
    }

    public void traverseExpressions(Consumer<CodegenExpression> consumer) {
    }
}
