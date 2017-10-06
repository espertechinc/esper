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
package com.espertech.esper.epl.approx;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.agg.access.AggregationAgentCodegenSymbols;
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.util.JavaClassHelper;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class CountMinSketchAggAgentAddFilter extends CountMinSketchAggAgentAdd {

    private final ExprEvaluator filter;

    public CountMinSketchAggAgentAddFilter(ExprEvaluator stringEvaluator, ExprEvaluator filter) {
        super(stringEvaluator);
        this.filter = filter;
    }

    @Override
    public void applyEnter(EventBean[] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext, AggregationState aggregationState) {
        Boolean pass = (Boolean) filter.evaluate(eventsPerStream, true, exprEvaluatorContext);
        if (pass != null && pass) {
            Object value = stringEvaluator.evaluate(eventsPerStream, true, exprEvaluatorContext);
            CountMinSketchAggState state = (CountMinSketchAggState) aggregationState;
            state.add(value);
        }
    }

    public static CodegenExpression applyEnterCodegen(CountMinSketchAggAgentAddForge forge, CodegenMethodScope parent, AggregationAgentCodegenSymbols symbols, CodegenClassScope classScope) {
        CodegenMethodNode method = parent.makeChild(void.class, CountMinSketchAggAgentAddFilter.class, classScope);
        if (forge.optionalFilterForge != null) {
            Class evalType = forge.optionalFilterForge.getEvaluationType();
            method.getBlock().declareVar(evalType, "pass", forge.optionalFilterForge.evaluateCodegen(evalType, method, symbols, classScope));
            if (!evalType.isPrimitive()) {
                method.getBlock().ifRefNull("pass").blockReturnNoValue();
            }
            method.getBlock().ifCondition(not(ref("pass"))).blockReturnNoValue();
        }
        Class evaluationType = forge.getStringEvaluator().getEvaluationType();
        method.getBlock()
                .declareVar(JavaClassHelper.getBoxedType(evaluationType), "value", forge.getStringEvaluator().evaluateCodegen(evaluationType, method, symbols, classScope))
                .declareVar(CountMinSketchAggState.class, "countMinSketch", cast(CountMinSketchAggState.class, symbols.getAddState(method)))
                .expression(exprDotMethod(ref("countMinSketch"), "add", ref("value")));
        return localMethod(method);
    }
}
