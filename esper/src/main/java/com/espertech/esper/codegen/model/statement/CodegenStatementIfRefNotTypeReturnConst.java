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

import java.util.Map;
import java.util.Set;

import static com.espertech.esper.codegen.core.CodeGenerationHelper.appendClassName;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionUtil.renderConstant;

public class CodegenStatementIfRefNotTypeReturnConst extends CodegenStatementBase {

    private final String var;
    private final Class type;
    private final Object constant;

    public CodegenStatementIfRefNotTypeReturnConst(String var, Class type, Object constant) {
        this.var = var;
        this.type = type;
        this.constant = constant;
    }

    public void renderStatement(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass) {
        builder.append("if (!(").append(var).append(" instanceof ");
        appendClassName(builder, type, null, imports).append(")) return ");
        renderConstant(builder, constant, imports);
    }

    public void mergeClasses(Set<Class> classes) {
        classes.add(type);
    }
}
