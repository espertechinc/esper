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
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.expression.subquery.SubselectForgeCodegenUtil.DECLARE_EVENTS_SHIFTED;
import static com.espertech.esper.common.internal.epl.expression.subquery.SubselectForgeCodegenUtil.REF_EVENTS_SHIFTED;

public class SubselectForgeNRExistsDefault implements SubselectForgeNR {
    private final ExprForge filterEval;
    private final ExprForge havingEval;

    public SubselectForgeNRExistsDefault(ExprForge filterEval, ExprForge havingEval) {
        this.filterEval = filterEval;
        this.havingEval = havingEval;
    }

    public CodegenExpression evaluateMatchesCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(boolean.class, this.getClass(), classScope);
        method.getBlock().applyTri(new SubselectForgeCodegenUtil.ReturnIfNoMatch(constantFalse(), constantFalse()), method, symbols);

        if (filterEval == null && havingEval == null) {
            method.getBlock().methodReturn(constantTrue());
            return localMethod(method);
        }

        method.getBlock().applyTri(DECLARE_EVENTS_SHIFTED, method, symbols);
        if (havingEval != null) {
            throw new UnsupportedOperationException();
        }

        CodegenMethod filter = CodegenLegoMethodExpression.codegenExpression(filterEval, method, classScope);
        method.getBlock()
                .forEach(EventBean.class, "subselectEvent", symbols.getAddMatchingEvents(method))
                .assignArrayElement(REF_EVENTS_SHIFTED, constant(0), ref("subselectEvent"))
                .declareVar(Boolean.class, "pass", localMethod(filter, REF_EVENTS_SHIFTED, constantTrue(), symbols.getAddExprEvalCtx(method)))
                .ifCondition(and(notEqualsNull(ref("pass")), ref("pass")))
                .blockReturn(constantTrue())
                .blockEnd()
                .methodReturn(constantFalse());
        return localMethod(method);
    }
}
