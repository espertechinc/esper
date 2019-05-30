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
package com.espertech.esper.common.internal.bytecodemodel.model.statement;

import com.espertech.esper.common.internal.bytecodemodel.core.CodegenIndent;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class CodegenStatementIfNullReturnNull implements CodegenStatement {

    private final CodegenExpression ref;

    public CodegenStatementIfNullReturnNull(CodegenExpression ref) {
        this.ref = ref;
    }

    public void render(StringBuilder builder, Map<Class, String> imports, boolean isInnerClass, int level, CodegenIndent indent) {
        builder.append("if (");
        ref.render(builder, imports, isInnerClass);
        builder.append(" == null) {return null;}\n");
    }

    public void mergeClasses(Set<Class> classes) {
    }

    public void traverseExpressions(Consumer<CodegenExpression> consumer) {
        consumer.accept(ref);
    }
}
