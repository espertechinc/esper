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
package com.espertech.esper.epl.agg.service.table;

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.agg.access.AggregationAgent;
import com.espertech.esper.epl.agg.access.AggregationAgentCodegenSymbols;
import com.espertech.esper.epl.agg.access.AggregationAgentForge;
import com.espertech.esper.epl.agg.service.common.AggregationServiceCodegenUtil;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.table.mgmt.TableColumnMethodPair;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionCodegenType;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class AggSvcGroupByWTableUtil {
    public static CodegenExpression[] getMethodEnterLeave(TableColumnMethodPair[] methodPairs, CodegenMethodNode method, AggregationAgentCodegenSymbols symbols, CodegenClassScope classScope) {
        CodegenExpression[] expressions = new CodegenExpression[methodPairs.length];
        for (int i = 0; i < methodPairs.length; i++) {
            expressions[i] = computeCompositeKeyCodegen(methodPairs[i].getForges(), method, symbols, classScope);
        }
        return expressions;
    }

    public static CodegenExpression[] getAccessEnterLeave(boolean enter, AggregationAgentForge[] agentForges, AggregationAgent[] agents, CodegenMethodNode method, AggregationAgentCodegenSymbols symbols, CodegenClassScope classScope) {
        CodegenMember agentsMember = classScope.makeAddMember(AggregationAgent[].class, agents);
        CodegenExpression[] expressions = new CodegenExpression[agentForges.length];
        for (int i = 0; i < agentForges.length; i++) {
            if (agentForges[i].getPluginCodegenType() == PlugInAggregationMultiFunctionCodegenType.CODEGEN_NONE) {
                expressions[i] = exprDotMethod(arrayAtIndex(member(agentsMember.getMemberId()), constant(i)), enter ? "applyEnter" : "applyLeave", symbols.getAddEPS(method), symbols.getAddExprEvalCtx(method), symbols.getAddState(method));
            } else {
                expressions[i] = enter ? agentForges[i].applyEnterCodegen(method, symbols, classScope) : agentForges[i].applyLeaveCodegen(method, symbols, classScope);
            }
        }
        return expressions;
    }

    private static CodegenExpression computeCompositeKeyCodegen(ExprForge[] forges, CodegenMethodScope parent, AggregationAgentCodegenSymbols symbols, CodegenClassScope classScope) {
        CodegenMethodNode method = parent.makeChild(Object.class, AggregationServiceCodegenUtil.class, classScope);
        if (forges.length == 1) {
            CodegenExpression expression = forges[0].evaluateCodegen(Object.class, method, symbols, classScope);
            method.getBlock().methodReturn(expression);
        } else {
            method.getBlock().declareVar(Object[].class, "keys", newArrayByLength(Object.class, constant(forges.length)));
            for (int i = 0; i < forges.length; i++) {
                method.getBlock().assignArrayElement("keys", constant(i), forges[i].evaluateCodegen(Object.class, method, symbols, classScope));
            }
            method.getBlock().methodReturn(ref("keys"));
        }
        return localMethod(method);
    }
}
