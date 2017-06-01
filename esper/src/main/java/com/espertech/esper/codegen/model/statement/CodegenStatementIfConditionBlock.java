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

import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.model.expression.CodegenExpression;

import java.util.Set;

public class CodegenStatementIfConditionBlock {
    private final CodegenExpression condition;
    private final CodegenBlock block;

    public CodegenStatementIfConditionBlock(CodegenExpression condition, CodegenBlock block) {
        this.condition = condition;
        this.block = block;
    }

    public CodegenExpression getCondition() {
        return condition;
    }

    public CodegenBlock getBlock() {
        return block;
    }

    void mergeClasses(Set<Class> classes) {
        condition.mergeClasses(classes);
        block.mergeClasses(classes);
    }
}
