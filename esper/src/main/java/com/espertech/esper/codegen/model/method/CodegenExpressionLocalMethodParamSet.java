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

import java.util.List;
import java.util.Map;
import java.util.Set;

public class CodegenExpressionLocalMethodParamSet implements CodegenExpression {
    private final String method;
    private final List<CodegenPassSet> parameterSets;

    public CodegenExpressionLocalMethodParamSet(String method, List<CodegenPassSet> parameterSets) {
        this.method = method;
        this.parameterSets = parameterSets;
    }

    public void render(StringBuilder builder, Map<Class, String> imports) {
        builder.append(method).append("(");
        String delimiter = "";
        for (CodegenPassSet set : parameterSets) {
            builder.append(delimiter);
            set.render(builder, imports);
            delimiter = ",";
        }
        builder.append(")");
    }

    public void mergeClasses(Set<Class> classes) {
        for (CodegenPassSet set : parameterSets) {
            set.mergeClasses(classes);
        }
    }
}
