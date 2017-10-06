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
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.epl.agg.access.*;
import com.espertech.esper.epl.agg.service.common.AggregationStateFactory;
import com.espertech.esper.epl.expression.accessagg.ExprAggMultiFunctionLinearAccessNode;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;

public class AggregationStateLinearFactory implements AggregationStateFactory {

    protected final ExprAggMultiFunctionLinearAccessNode expr;
    protected final int streamNum;
    protected final ExprEvaluator optionalFilter;

    public AggregationStateLinearFactory(ExprAggMultiFunctionLinearAccessNode expr, int streamNum, ExprEvaluator optionalFilter) {
        this.expr = expr;
        this.streamNum = streamNum;
        this.optionalFilter = optionalFilter;
    }

    public AggregationState createAccess(int agentInstanceId, boolean join, Object groupKey, AggregationServicePassThru passThru) {
        if (join) {
            if (optionalFilter != null) {
                return new AggregationStateLinearJoinWFilter(streamNum, optionalFilter);
            }
            return new AggregationStateLinearJoinImpl(streamNum);
        }
        if (optionalFilter != null) {
            return new AggregationStateLinearWFilter(streamNum, optionalFilter);
        }
        return new AggregationStateLinearImpl(streamNum);
    }

    public ExprNode getAggregationExpression() {
        return expr;
    }

    public static void rowMemberCodegen(AggregationStateLinearForge forge, int stateNumber, CodegenCtor ctor, CodegenMembersColumnized membersColumnized, CodegenClassScope classScope) {
        if (forge.isJoin()) {
            AggregationStateLinearJoinImpl.rowMemberCodegen(stateNumber, ctor, membersColumnized);
        } else {
            AggregationStateLinearImpl.rowMemberCodegen(stateNumber, ctor, membersColumnized);
        }
    }

    public static void applyEnterCodegen(AggregationStateLinearForge forge, int stateNumber, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        if (forge.isJoin()) {
            AggregationStateLinearJoinImpl.applyEnterCodegen(forge, stateNumber, method, symbols, classScope);
        } else {
            AggregationStateLinearImpl.applyEnterCodegen(forge, stateNumber, method, symbols, classScope);
        }
    }

    public static void applyLeaveCodegen(AggregationStateLinearForge forge, int stateNumber, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope) {
        if (forge.isJoin()) {
            AggregationStateLinearJoinImpl.applyLeaveCodegen(forge, stateNumber, method, symbols, classScope);
        } else {
            AggregationStateLinearImpl.applyLeaveCodegen(forge, stateNumber, method, symbols, classScope);
        }
    }

    public static void clearCodegen(AggregationStateLinearForge forge, int stateNumber, CodegenMethodNode method) {
        if (forge.isJoin()) {
            AggregationStateLinearJoinImpl.clearCodegen(stateNumber, method);
        } else {
            AggregationStateLinearImpl.clearCodegen(stateNumber, method);
        }
    }

    public static CodegenExpression getFirstValueCodegen(AggregationStateLinearForge forge, int slot, CodegenClassScope classScope, CodegenMethodNode parentMethod) {
        if (forge.isJoin()) {
            return AggregationStateLinearJoinImpl.codegenGetFirstValue(slot, classScope, parentMethod);
        }
        return AggregationStateLinearImpl.codegenGetFirstValue(slot, classScope, parentMethod);
    }

    static CodegenExpression sizeCodegen(AggregationStateLinearForge forge, int slot) {
        if (forge.isJoin()) {
            return AggregationStateLinearJoinImpl.sizeCodegen(forge, slot);
        }
        return AggregationStateLinearImpl.codegenSize(slot);
    }

    static CodegenExpression iteratorCodegen(AggregationStateLinearForge forge, int slot, CodegenClassScope classScope, CodegenMethodNode parentMethod, CodegenNamedMethods namedMethods) {
        if (forge.isJoin()) {
            return AggregationStateLinearJoinImpl.iteratorCodegen(slot, classScope, parentMethod, namedMethods);
        }
        return AggregationStateLinearImpl.codegenIterator(slot);
    }

    public static CodegenExpression getLastValueCodegen(AggregationStateLinearForge forge, int slot, CodegenClassScope classScope, CodegenMethodNode parentMethod, CodegenNamedMethods namedMethods) {
        if (forge.isJoin()) {
            return AggregationStateLinearJoinImpl.codegenGetLastValue(slot, classScope, parentMethod, namedMethods);
        }
        return AggregationStateLinearImpl.codegenGetLastValue(slot, classScope, parentMethod);
    }

    static CodegenExpression getFirstNthValueCodegen(AggregationStateLinearForge forge, CodegenExpressionRef index, int slot, CodegenClassScope classScope, CodegenMethodNode parentMethod, CodegenNamedMethods namedMethods) {
        if (forge.isJoin()) {
            return AggregationStateLinearJoinImpl.getFirstNthValueCodegen(index, slot, classScope, parentMethod, namedMethods);
        }
        return AggregationStateLinearImpl.getFirstNthValueCodegen(index, slot, classScope, parentMethod);
    }

    static CodegenExpression getLastNthValueCodegen(AggregationStateLinearForge forge, CodegenExpressionRef index, int slot, CodegenClassScope classScope, CodegenMethodNode parentMethod, CodegenNamedMethods namedMethods) {
        if (forge.isJoin()) {
            return AggregationStateLinearJoinImpl.getLastNthValueCodegen(index, slot, classScope, parentMethod, namedMethods);
        }
        return AggregationStateLinearImpl.getLastNthValueCodegen(index, slot, classScope, parentMethod);
    }

    public static CodegenExpression collectionReadOnlyCodegen(AggregationStateLinearForge forge, int column, CodegenMethodNode parentMethod, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        if (forge.isJoin()) {
            return AggregationStateLinearJoinImpl.collectionReadOnlyCodegen(column, parentMethod, classScope, namedMethods);
        }
        return AggregationStateLinearImpl.collectionReadOnlyCodegen(column);
    }

    public ExprAggMultiFunctionLinearAccessNode getExpr() {
        return expr;
    }

    public int getStreamNum() {
        return streamNum;
    }

    public ExprEvaluator getOptionalFilter() {
        return optionalFilter;
    }
}
