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

public class AggregationStateSortedFactory implements AggregationStateFactory {

    protected final ExprAggMultiFunctionSortedMinMaxByNode expr;
    protected final AggregationStateSortedSpec spec;

    public AggregationStateSortedFactory(ExprAggMultiFunctionSortedMinMaxByNode expr, AggregationStateSortedSpec spec) {
        this.expr = expr;
        this.spec = spec;
    }

    public AggregationState createAccess(int agentInstanceId, boolean join, Object groupKey, AggregationServicePassThru passThru) {
        if (join) {
            if (spec.getOptionalFilter() != null) {
                return new AggregationStateSortedJoinWFilter(spec);
            }
            return new AggregationStateSortedJoin(spec);
        }
        if (spec.getOptionalFilter() != null) {
            return new AggregationStateSortedWFilter(spec);
        }
        return new AggregationStateSortedImpl(spec);
    }

    public ExprNode getAggregationExpression() {
        return expr;
    }

    public static void rowMemberCodegen(AggregationStateSortedForge forge, int stateNumber, CodegenCtor ctor, CodegenMembersColumnized membersColumnized, CodegenClassScope classScope) {
        if (forge.getSpec().isJoin()) {
            AggregationStateSortedJoin.rowMemberCodegenJoin(forge, stateNumber, ctor, membersColumnized, classScope);
        } else {
            AggregationStateSortedImpl.rowMemberCodegen(forge, stateNumber, ctor, membersColumnized, classScope);
        }
    }

    public static void applyEnterCodegen(AggregationStateSortedForge forge, int stateNumber, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        if (forge.getSpec().isJoin()) {
            AggregationStateSortedJoin.applyEnterCodegen(forge, stateNumber, method, symbols, namedMethods, classScope);
        } else {
            AggregationStateSortedImpl.applyEnterCodegen(forge, stateNumber, method, symbols, namedMethods, classScope);
        }
    }

    public static void applyLeaveCodegen(AggregationStateSortedForge forge, int stateNumber, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        if (forge.getSpec().isJoin()) {
            AggregationStateSortedJoin.applyLeaveCodegen(forge, stateNumber, method, symbols, namedMethods, classScope);
        } else {
            AggregationStateSortedImpl.applyLeaveCodegen(forge, stateNumber, method, symbols, namedMethods, classScope);
        }
    }

    public static void clearCodegen(AggregationStateSortedForge forge, int stateNumber, CodegenMethodNode method) {
        if (forge.getSpec().isJoin()) {
            AggregationStateSortedJoin.clearCodegen(stateNumber, method);
        } else {
            AggregationStateSortedImpl.clearCodegen(stateNumber, method);
        }
    }

    public AggregationStateSortedSpec getSpec() {
        return spec;
    }

    static CodegenExpression iteratorCodegen(AggregationStateSortedForge forge, int slot) {
        return AggregationStateSortedImpl.iteratorCodegen(forge, slot);
    }

    static CodegenExpression getReverseIteratorCodegen(AggregationStateSortedForge forge, int slot) {
        return AggregationStateSortedImpl.getReverseIteratorCodegen(forge, slot);
    }

    static CodegenExpression sizeCodegen(AggregationStateSortedForge forge, int slot) {
        return AggregationStateSortedImpl.sizeCodegen(forge, slot);
    }

    public static CodegenExpression getFirstValueCodegen(AggregationStateSortedForge forge, int slot, CodegenClassScope classScope, CodegenMethodNode parent) {
        return AggregationStateSortedImpl.getFirstValueCodegen(forge, slot, classScope, parent);
    }

    public static CodegenExpression getLastValueCodegen(AggregationStateSortedForge forge, int slot, CodegenClassScope classScope, CodegenMethodNode parent) {
        return AggregationStateSortedImpl.getLastValueCodegen(forge, slot, classScope, parent);
    }

    public static CodegenExpression collectionReadOnlyCodegen(int column) {
        return AggregationStateSortedImpl.collectionReadOnlyCodegen(column);
    }
}
