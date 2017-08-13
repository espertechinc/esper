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

import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class CodegenExpressionLocalMethodParamSet implements CodegenExpression {
    private final CodegenMethodId methodId;
    private final List<CodegenPassSet> parameterSets;

    public CodegenExpressionLocalMethodParamSet(CodegenMethodId methodId, List<CodegenPassSet> parameterSets) {
        this.methodId = methodId;
        this.parameterSets = parameterSets;
    }

    public void render(StringBuilder builder, Map<Class, String> imports) {
        methodId.render(builder);
        builder.append("(");
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
