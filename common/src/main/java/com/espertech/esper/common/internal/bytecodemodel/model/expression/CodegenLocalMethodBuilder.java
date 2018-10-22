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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;

import java.util.ArrayList;
import java.util.List;

public class CodegenLocalMethodBuilder {

    private final CodegenMethod methodNode;
    private final List<CodegenExpression> parameters = new ArrayList<>(2);

    public CodegenLocalMethodBuilder(CodegenMethod methodNode) {
        this.methodNode = methodNode;
    }

    public CodegenLocalMethodBuilder pass(CodegenExpression expression) {
        parameters.add(expression);
        return this;
    }

    public CodegenExpression call() {
        return new CodegenExpressionLocalMethod(methodNode, parameters);
    }
}
