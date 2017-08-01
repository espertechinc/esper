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
package com.espertech.esper.codegen.model.method;

import com.espertech.esper.codegen.model.expression.CodegenExpression;

import java.util.Map;
import java.util.Set;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.renderExpressions;

public class CodegenPassSetMulti extends CodegenPassSet {

    private final CodegenExpression[] expressions;

    public CodegenPassSetMulti(CodegenExpression[] expressions) {
        this.expressions = expressions;
    }

    public void render(StringBuilder builder, Map<Class, String> imports) {
        renderExpressions(builder, expressions, imports);
    }

    public void mergeClasses(Set<Class> classes) {
        for (CodegenExpression expression : expressions) {
            expression.mergeClasses(classes);
        }
    }
}
