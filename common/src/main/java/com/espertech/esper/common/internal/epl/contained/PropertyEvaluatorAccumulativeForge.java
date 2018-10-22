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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * A property evaluator that returns a full row of events for each stream, i.e. flattened inner-join results for
 * property-upon-property.
 */
public class PropertyEvaluatorAccumulativeForge {

    private final ContainedEventEvalForge[] containedEventEvals;
    private final boolean[] fragmentEventTypeIsIndexed;
    private final ExprForge[] whereClauses;
    private final List<String> propertyNames;

    /**
     * Ctor.
     *
     * @param containedEventEvals        property getters or other evaluators
     * @param fragmentEventTypeIsIndexed property fragment types is indexed
     * @param whereClauses               filters, if any
     * @param propertyNames              the property names that are staggered
     */
    public PropertyEvaluatorAccumulativeForge(ContainedEventEvalForge[] containedEventEvals, boolean[] fragmentEventTypeIsIndexed, ExprForge[] whereClauses, List<String> propertyNames) {
        this.fragmentEventTypeIsIndexed = fragmentEventTypeIsIndexed;
        this.containedEventEvals = containedEventEvals;
        this.whereClauses = whereClauses;
        this.propertyNames = propertyNames;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(PropertyEvaluatorAccumulative.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(PropertyEvaluatorAccumulative.class, "pe", newInstance(PropertyEvaluatorAccumulative.class))
                .exprDotMethod(ref("pe"), "setContainedEventEvals", makeContained(containedEventEvals, method, symbols, classScope))
                .exprDotMethod(ref("pe"), "setWhereClauses", makeWhere(whereClauses, method, symbols, classScope))
                .exprDotMethod(ref("pe"), "setPropertyNames", constant(propertyNames.toArray(new String[propertyNames.size()])))
                .exprDotMethod(ref("pe"), "setFragmentEventTypeIsIndexed", constant(fragmentEventTypeIsIndexed))
                .methodReturn(ref("pe"));
        return localMethod(method);
    }

    protected static CodegenExpression makeWhere(ExprForge[] whereClauses, CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenExpression[] expressions = new CodegenExpression[whereClauses.length];
        for (int i = 0; i < whereClauses.length; i++) {
            expressions[i] = whereClauses[i] == null ? constantNull() : ExprNodeUtilityCodegen.codegenEvaluator(whereClauses[i], method, PropertyEvaluatorAccumulativeForge.class, classScope);
        }
        return newArrayWithInit(ExprEvaluator.class, expressions);
    }

    protected static CodegenExpression makeContained(ContainedEventEvalForge[] evals, CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenExpression[] expressions = new CodegenExpression[evals.length];
        for (int i = 0; i < evals.length; i++) {
            expressions[i] = evals[i].make(parent, symbols, classScope);
        }
        return newArrayWithInit(ContainedEventEval.class, expressions);
    }
}
