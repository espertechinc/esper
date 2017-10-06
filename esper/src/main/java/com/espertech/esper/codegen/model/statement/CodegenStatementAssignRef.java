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
package com.espertech.esper.codegen.model.statement;

import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;

import java.util.Map;
import java.util.Set;

public class CodegenStatementAssignRef extends CodegenStatementBase {
    private final CodegenExpressionRef ref;
    private final CodegenExpression assignment;

    public CodegenStatementAssignRef(CodegenExpressionRef ref, CodegenExpression assignment) {
        this.ref = ref;
        this.assignment = assignment;
    }

    public void renderStatement(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        ref.render(builder, imports, isInnerClass);
        builder.append("=");
        assignment.render(builder, imports, isInnerClass);
    }

    public void mergeClasses(Set<Class> classes) {
        assignment.mergeClasses(classes);
    }
}
