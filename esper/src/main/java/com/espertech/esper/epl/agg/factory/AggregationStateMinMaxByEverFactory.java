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
package com.espertech.esper.epl.agg.factory;

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMembersColumnized;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.core.CodegenCtor;
import com.espertech.esper.codegen.core.CodegenNamedMethods;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.agg.access.*;
import com.espertech.esper.epl.agg.service.common.AggregationStateFactory;
import com.espertech.esper.epl.expression.accessagg.ExprAggMultiFunctionSortedMinMaxByNode;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprNode;

public class AggregationStateMinMaxByEverFactory implements AggregationStateFactory {

    protected final ExprAggMultiFunctionSortedMinMaxByNode expr;
    protected final AggregationStateMinMaxByEverSpec spec;

    public AggregationStateMinMaxByEverFactory(ExprAggMultiFunctionSortedMinMaxByNode expr, AggregationStateMinMaxByEverSpec spec) {
        this.expr = expr;
        this.spec = spec;
    }

    public AggregationState createAccess(int agentInstanceId, boolean join, Object groupKey, AggregationServicePassThru passThru) {
        if (spec.getOptionalFilter() != null) {
            return new AggregationStateMinMaxByEverWFilter(spec);
        }
        return new AggregationStateMinMaxByEver(spec);
    }

    public ExprNode getAggregationExpression() {
        return expr;
    }

    public static void rowMemberCodegen(AggregationStateMinMaxByEverForge forge, int stateNumber, CodegenCtor ctor, CodegenMembersColumnized membersColumnized, CodegenClassScope classScope) {
        AggregationStateMinMaxByEver.rowMemberCodegen(stateNumber, membersColumnized);
    }

    public static void applyEnterCodegen(AggregationStateMinMaxByEverForge forge, int stateNumber, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        AggregationStateMinMaxByEver.applyEnterCodegen(forge, stateNumber, method, symbols, classScope, namedMethods);
    }

    public static void applyLeaveCodege() {
        // no code
    }

    public static void clearCodegen(AggregationStateMinMaxByEverForge forge, int stateNumber, CodegenMethodNode method) {
        AggregationStateMinMaxByEver.clearCodegen(stateNumber, method);
    }

    public static CodegenExpression getFirstValueCodegen(AggregationStateMinMaxByEverForge forge, int slot, CodegenClassScope classScope, CodegenMethodNode method) {
        return AggregationStateMinMaxByEver.getFirstValueCodegen(forge, slot, classScope, method);
    }

    public static CodegenExpression getLastValueCodegen(AggregationStateMinMaxByEverForge forge, int slot, CodegenClassScope classScope, CodegenMethodNode method) {
        return AggregationStateMinMaxByEver.getLastValueCodegen(forge, slot, classScope, method);
    }
}
