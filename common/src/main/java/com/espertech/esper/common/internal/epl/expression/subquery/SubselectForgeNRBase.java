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
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.subquery.SubselectForgeCodegenUtil.ReturnIfNoMatch;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.localMethod;
import static com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenNames.*;
import static com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectEvalMatchSymbol.NAME_MATCHINGEVENTS;
import static com.espertech.esper.common.internal.epl.expression.subquery.SubselectForgeCodegenUtil.DECLARE_EVENTS_SHIFTED;
import static com.espertech.esper.common.internal.epl.expression.subquery.SubselectForgeCodegenUtil.REF_EVENTS_SHIFTED;
import static com.espertech.esper.common.internal.epl.expression.subquery.SubselectForgeNRSymbol.NAME_LEFTRESULT;
import static com.espertech.esper.common.internal.epl.expression.subquery.SubselectForgeNRSymbol.REF_LEFTRESULT;

public abstract class SubselectForgeNRBase implements SubselectForgeNR {
    protected final ExprSubselectNode subselect;
    protected final ExprForge valueEval;
    protected final ExprForge selectEval;
    private final boolean resultWhenNoMatchingEvents;

    protected abstract CodegenExpression codegenEvaluateInternal(CodegenMethodScope parent, SubselectForgeNRSymbol symbols, CodegenClassScope classScope);

    public SubselectForgeNRBase(ExprSubselectNode subselect, ExprForge valueEval, ExprForge selectEval, boolean resultWhenNoMatchingEvents) {
        this.subselect = subselect;
        this.valueEval = valueEval;
        this.selectEval = selectEval;
        this.resultWhenNoMatchingEvents = resultWhenNoMatchingEvents;
    }

    public CodegenExpression evaluateMatchesCodegen(CodegenMethodScope parent, ExprSubselectEvalMatchSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(subselect.getEvaluationType(), this.getClass(), classScope);
        method.getBlock()
                .applyTri(new ReturnIfNoMatch(constant(resultWhenNoMatchingEvents), constant(resultWhenNoMatchingEvents)), method, symbols)
                .declareVar(valueEval.getEvaluationType(), "leftResult", valueEval.evaluateCodegen(valueEval.getEvaluationType(), parent, symbols, classScope))
                .applyTri(DECLARE_EVENTS_SHIFTED, method, symbols);

        Class leftResultType = JavaClassHelper.getBoxedType(valueEval.getEvaluationType());
        SubselectForgeNRSymbol nrSymbols = new SubselectForgeNRSymbol(leftResultType);
        CodegenMethod child = parent.makeChildWithScope(subselect.getEvaluationType(), this.getClass(), nrSymbols, classScope)
                .addParam(leftResultType, NAME_LEFTRESULT).addParam(EventBean[].class, NAME_EPS).addParam(boolean.class, NAME_ISNEWDATA)
                .addParam(Collection.class, NAME_MATCHINGEVENTS).addParam(ExprEvaluatorContext.class, NAME_EXPREVALCONTEXT);
        child.getBlock().methodReturn(codegenEvaluateInternal(child, nrSymbols, classScope));
        method.getBlock().methodReturn(localMethod(child, REF_LEFTRESULT, REF_EVENTS_SHIFTED, symbols.getAddIsNewData(method), symbols.getAddMatchingEvents(method), symbols.getAddExprEvalCtx(method)));

        return localMethod(method);
    }
}
