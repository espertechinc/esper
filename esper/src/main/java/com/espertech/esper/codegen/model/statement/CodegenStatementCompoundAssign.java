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

public class CodegenStatementCompoundAssign extends CodegenStatementBase {
    private final String ref;
    private final String op;
    private final CodegenExpression rhs;

    public CodegenStatementCompoundAssign(String ref, String op, CodegenExpression rhs) {
        this.ref = ref;
        this.op = op;
        this.rhs = rhs;
    }

    public void renderStatement(StringBuilder builder, Map<Class, String> imports) {
        builder.append(ref).append(" ").append(op).append("=");
        rhs.render(builder, imports);
    }

    public void mergeClasses(Set<Class> classes) {
        rhs.mergeClasses(classes);
    }
}
