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
package com.espertech.esper.common.internal.metrics.instrumentation;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;

import java.util.function.Consumer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class InstrumentationCode {
    public static Consumer<CodegenBlock> instblock(CodegenClassScope codegenClassScope, String name, CodegenExpression... expressions) {
        if (!codegenClassScope.isInstrumented()) {
            return block -> {
            };
        }
        return block -> generate(block, name, expressions);
    }

    private static void generate(CodegenBlock block, String name, CodegenExpression... expressions) {
        block.ifCondition(publicConstValue(InstrumentationCommon.RUNTIME_HELPER_CLASS, "ENABLED"))
                .expression(exprDotMethodChain(staticMethod(InstrumentationCommon.RUNTIME_HELPER_CLASS, "get")).add(name, expressions))
                .blockEnd();
    }
}
