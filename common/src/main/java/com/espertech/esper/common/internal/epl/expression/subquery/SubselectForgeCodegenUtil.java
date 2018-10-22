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
package com.espertech.esper.common.internal.epl.expression.subquery;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.event.core.EventBeanUtility;
import com.espertech.esper.common.internal.util.TriConsumer;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class SubselectForgeCodegenUtil {
    public final static String EVENTS_SHIFTED = "shift";
    public final static CodegenExpressionRef REF_EVENTS_SHIFTED = ref(EVENTS_SHIFTED);

    final static TriConsumer<CodegenMethod, CodegenBlock, ExprSubselectEvalMatchSymbol> DECLARE_EVENTS_SHIFTED = (method, block, symbols) -> {
        block.declareVar(EventBean[].class, EVENTS_SHIFTED, staticMethod(EventBeanUtility.class, "allocatePerStreamShift", symbols.getAddEPS(method)));
    };

    public static class ReturnIfNoMatch implements TriConsumer<CodegenMethod, CodegenBlock, ExprSubselectEvalMatchSymbol> {
        private final CodegenExpression valueIfNull;
        private final CodegenExpression valueIfEmpty;

        public ReturnIfNoMatch(CodegenExpression valueIfNull, CodegenExpression valueIfEmpty) {
            this.valueIfNull = valueIfNull;
            this.valueIfEmpty = valueIfEmpty;
        }

        public void accept(CodegenMethod method, CodegenBlock block, ExprSubselectEvalMatchSymbol symbols) {
            CodegenExpression matching = symbols.getAddMatchingEvents(method);
            block.ifCondition(equalsNull(matching))
                    .blockReturn(valueIfNull)
                    .ifCondition(exprDotMethod(matching, "isEmpty"))
                    .blockReturn(valueIfEmpty);
        }
    }
}
