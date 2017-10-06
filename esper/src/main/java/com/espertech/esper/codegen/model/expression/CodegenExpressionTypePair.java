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

public class CodegenExpressionTypePair {
    private final Class type;
    private final CodegenExpression expression;

    public CodegenExpressionTypePair(Class type, CodegenExpression expression) {
        this.type = type;
        this.expression = expression;
    }

    public Class getType() {
        return type;
    }

    public CodegenExpression getExpression() {
        return expression;
    }
}
