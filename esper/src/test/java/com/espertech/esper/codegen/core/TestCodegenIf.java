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
package com.espertech.esper.codegen.core;

import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import junit.framework.TestCase;

import java.util.HashMap;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.equalsIdentity;
import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.ref;

public class TestCodegenIf extends TestCase {
    public void testIf() {
        CodegenBlock block = new CodegenBlock((CodegenMethodNode) null);
        block.ifCondition(equalsIdentity(ref("a"), constant(1)))
                .declareVar(int.class, "b", constant(1))
            .ifElseIf(equalsIdentity(ref("a"), constant(2)))
                .declareVar(int.class, "b", constant(2))
            .ifElse()
                .declareVar(int.class, "b", constant(3));

        StringBuilder builder = new StringBuilder();
        block.render(builder, new HashMap<>(), false, 1, new CodegenIndent(true));
        System.out.println(builder);
    }
}
