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

import static com.espertech.esper.codegen.model.expression.CodegenExpressionUtil.renderConstant;

public class CodegenExpressionConstant implements CodegenExpression {

    private final Object constant;

    public CodegenExpressionConstant(Object constant) {
        this.constant = constant;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        renderConstant(builder, constant, imports);
    }

    public void mergeClasses(Set<Class> classes) {
    }
}
