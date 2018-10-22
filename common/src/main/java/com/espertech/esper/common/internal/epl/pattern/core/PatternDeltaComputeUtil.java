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
package com.espertech.esper.common.internal.epl.pattern.core;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedParam;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.common.internal.epl.expression.codegen.CodegenLegoMethodExpression;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.time.abacus.TimeAbacus;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodCompute;
import com.espertech.esper.common.internal.epl.expression.time.node.ExprTimePeriod;
import com.espertech.esper.common.internal.filterspec.MatchedEventMap;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class PatternDeltaComputeUtil {
    public static CodegenExpression makePatternDeltaAnonymous(ExprNode parameter, MatchedEventConvertorForge convertor, TimeAbacus timeAbacus,
                                                              CodegenMethod method, CodegenClassScope classScope) {
        CodegenExpressionNewAnonymousClass compute = newAnonymousClass(method.getBlock(), PatternDeltaCompute.class);
        CodegenMethod computeDelta = CodegenMethod.makeParentNode(long.class, PatternDeltaComputeUtil.class, classScope).addParam(CodegenNamedParam.from(MatchedEventMap.class, "beginState", PatternAgentInstanceContext.class, "context"));
        compute.addMethod("computeDelta", computeDelta);

        if (parameter instanceof ExprTimePeriod) {
            ExprTimePeriod timePeriod = (ExprTimePeriod) parameter;
            CodegenExpression time = exprDotMethod(ref("context"), "getTime");
            if (timePeriod.isConstantResult()) {
                CodegenExpressionField delta = classScope.addFieldUnshared(true, TimePeriodCompute.class, timePeriod.getTimePeriodComputeForge().makeEvaluator(classScope.getPackageScope().getInitMethod(), classScope));
                computeDelta.getBlock().methodReturn(exprDotMethod(delta, "deltaAdd", time, constantNull(), constantTrue(), exprDotMethod(ref("context"), "getAgentInstanceContext")));
            } else {
                CodegenExpressionField delta = classScope.addFieldUnshared(true, TimePeriodCompute.class, timePeriod.getTimePeriodComputeForge().makeEvaluator(classScope.getPackageScope().getInitMethod(), classScope));
                computeDelta.getBlock()
                        .declareVar(EventBean[].class, "events", localMethod(convertor.make(computeDelta, classScope), ref("beginState")))
                        .methodReturn(exprDotMethod(delta, "deltaAdd", time, ref("events"), constantTrue(), exprDotMethod(ref("context"), "getAgentInstanceContext")));
            }
        } else {
            CodegenMethod eval = CodegenLegoMethodExpression.codegenExpression(parameter.getForge(), method, classScope);
            CodegenExpression events;
            if (parameter.getForge().getForgeConstantType().isConstant()) {
                events = constantNull();
            } else {
                events = localMethod(convertor.make(computeDelta, classScope), ref("beginState"));
            }
            computeDelta.getBlock()
                    .declareVar(EventBean[].class, "events", events)
                    .declareVar(parameter.getForge().getEvaluationType(), "result", localMethod(eval, ref("events"), constantTrue(), exprDotMethod(ref("context"), "getAgentInstanceContext")));
            if (!parameter.getForge().getEvaluationType().isPrimitive()) {
                computeDelta.getBlock().ifRefNull("result").blockThrow(newInstance(EPException.class, constant("Null value returned for guard expression")));
            }
            computeDelta.getBlock().methodReturn(timeAbacus.deltaForSecondsDoubleCodegen(ref("result"), classScope));
        }
        return compute;
    }
}
