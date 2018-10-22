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
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Property evaluator that considers only level one and considers a where-clause,
 * but does not consider a select clause or N-level.
 */
public class PropertyEvaluatorSimpleForge implements PropertyEvaluatorForge {
    private final ContainedEventEvalForge containedEventEval;
    private final FragmentEventType fragmentEventType;
    private final ExprForge filter;
    private final String expressionText;

    /**
     * Ctor.
     *
     * @param containedEventEval property getter or other evaluator
     * @param fragmentEventType  property event type
     * @param filter             optional where-clause expression
     * @param expressionText     the property name
     */
    public PropertyEvaluatorSimpleForge(ContainedEventEvalForge containedEventEval, FragmentEventType fragmentEventType, ExprForge filter, String expressionText) {
        this.fragmentEventType = fragmentEventType;
        this.containedEventEval = containedEventEval;
        this.filter = filter;
        this.expressionText = expressionText;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(PropertyEvaluatorSimple.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(PropertyEvaluatorSimple.class, "pe", newInstance(PropertyEvaluatorSimple.class))
                .exprDotMethod(ref("pe"), "setFilter", filter == null ? constantNull() : ExprNodeUtilityCodegen.codegenEvaluator(filter, method, this.getClass(), classScope))
                .exprDotMethod(ref("pe"), "setContainedEventEval", containedEventEval.make(method, symbols, classScope))
                .exprDotMethod(ref("pe"), "setFragmentIsIndexed", constant(fragmentEventType.isIndexed()))
                .exprDotMethod(ref("pe"), "setExpressionText", constant(expressionText))
                .exprDotMethod(ref("pe"), "setEventType", EventTypeUtility.resolveTypeCodegen(fragmentEventType.getFragmentType(), symbols.getAddInitSvc(method)))
                .methodReturn(ref("pe"));
        return localMethod(method);
    }

    public EventType getFragmentEventType() {
        return fragmentEventType.getFragmentType();
    }

    public ExprForge getFilter() {
        return filter;
    }

    public String getExpressionText() {
        return expressionText;
    }

    public boolean compareTo(PropertyEvaluatorForge otherEval) {
        if (!(otherEval instanceof PropertyEvaluatorSimpleForge)) {
            return false;
        }
        PropertyEvaluatorSimpleForge other = (PropertyEvaluatorSimpleForge) otherEval;
        if (!other.getExpressionText().equals(this.getExpressionText())) {
            return false;
        }
        if ((other.getFilter() == null) && (this.getFilter() == null)) {
            return true;
        }
        return false;
    }
}
