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
package com.espertech.esper.common.internal.epl.join.base;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanForge;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public abstract class JoinSetComposerPrototypeForge {
    private final EventType[] streamTypes;
    private final ExprNode postJoinEvaluator;
    private final boolean outerJoins;

    protected abstract Class implementation();

    protected abstract void populateInline(CodegenExpression impl, CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope);

    public abstract QueryPlanForge getOptionalQueryPlan();

    public JoinSetComposerPrototypeForge(EventType[] streamTypes, ExprNode postJoinEvaluator, boolean outerJoins) {
        this.streamTypes = streamTypes;
        this.postJoinEvaluator = postJoinEvaluator;
        this.outerJoins = outerJoins;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(implementation(), this.getClass(), classScope);

        method.getBlock()
                .declareVar(implementation(), "impl", newInstance(implementation()))
                .exprDotMethod(ref("impl"), "setStreamTypes", EventTypeUtility.resolveTypeArrayCodegen(streamTypes, symbols.getAddInitSvc(method)))
                .exprDotMethod(ref("impl"), "setOuterJoins", constant(outerJoins));

        if (postJoinEvaluator != null) {
            method.getBlock().exprDotMethod(ref("impl"), "setPostJoinFilterEvaluator", ExprNodeUtilityCodegen.codegenEvaluatorNoCoerce(postJoinEvaluator.getForge(), method, this.getClass(), classScope));
        }

        populateInline(ref("impl"), method, symbols, classScope);

        method.getBlock().methodReturn(ref("impl"));

        return localMethod(method);
    }
}
