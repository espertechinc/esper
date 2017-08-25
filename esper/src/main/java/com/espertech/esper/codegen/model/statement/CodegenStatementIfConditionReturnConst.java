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

import java.util.Map;
import java.util.Set;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionUtil.renderConstant;

public class CodegenStatementIfConditionReturnConst extends CodegenStatementBase {

    private final CodegenExpression condition;
    private final Object constant;

    public CodegenStatementIfConditionReturnConst(CodegenExpression condition, Object constant) {
        this.condition = condition;
        this.constant = constant;
    }

    public void renderStatement(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        builder.append("if (");
        condition.render(builder, imports, isInnerClass);
        builder.append(") return ");
        renderConstant(builder, constant, imports);
    }

    public void mergeClasses(Set<Class> classes) {
        condition.mergeClasses(classes);
    }
}
