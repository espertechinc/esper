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
package com.espertech.esper.common.internal.epl.contained;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.FragmentEventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * A property evaluator that considers nested properties and that considers where-clauses
 * but does not consider select-clauses.
 */
public class PropertyEvaluatorNestedForge implements PropertyEvaluatorForge {
    private final ContainedEventEvalForge[] containedEventEvals;
    private final FragmentEventType[] fragmentEventTypes;
    private final ExprForge[] whereClauses;
    private final String[] expressionTexts;
    private final boolean[] fragmentEventTypesIsIndexed;

    public PropertyEvaluatorNestedForge(ContainedEventEvalForge[] containedEventEvals, FragmentEventType[] fragmentEventTypes, ExprForge[] whereClauses, String[] expressionTexts) {
        this.containedEventEvals = containedEventEvals;
        this.fragmentEventTypes = fragmentEventTypes;
        this.whereClauses = whereClauses;
        this.expressionTexts = expressionTexts;
        fragmentEventTypesIsIndexed = new boolean[fragmentEventTypes.length];
        for (int i = 0; i < fragmentEventTypesIsIndexed.length; i++) {
            fragmentEventTypesIsIndexed[i] = fragmentEventTypes[i].isIndexed();
        }
    }

    public EventType getFragmentEventType() {
        return fragmentEventTypes[fragmentEventTypes.length - 1].getFragmentType();
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(PropertyEvaluatorNested.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(PropertyEvaluatorNested.class, "pe", newInstance(PropertyEvaluatorNested.class))
                .exprDotMethod(ref("pe"), "setResultEventType", EventTypeUtility.resolveTypeCodegen(fragmentEventTypes[fragmentEventTypes.length - 1].getFragmentType(), symbols.getAddInitSvc(method)))
                .exprDotMethod(ref("pe"), "setExpressionTexts", constant(expressionTexts))
                .exprDotMethod(ref("pe"), "setWhereClauses", PropertyEvaluatorAccumulativeForge.makeWhere(whereClauses, method, symbols, classScope))
                .exprDotMethod(ref("pe"), "setContainedEventEvals", PropertyEvaluatorAccumulativeForge.makeContained(containedEventEvals, method, symbols, classScope))
                .exprDotMethod(ref("pe"), "setFragmentEventTypeIsIndexed", constant(fragmentEventTypesIsIndexed))
                .methodReturn(ref("pe"));
        return localMethod(method);
    }

    public boolean compareTo(PropertyEvaluatorForge other) {
        return false;
    }
}
