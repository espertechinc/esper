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
package com.espertech.esper.common.internal.epl.agg.access.plugin;

import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionState;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionStateFactory;
import com.espertech.esper.common.client.hook.aggmultifunc.AggregationMultiFunctionStateModeManaged;
import com.espertech.esper.common.client.hook.forgeinject.InjectionStrategyClassNewInstance;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMemberCol;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedMethods;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionMember;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.epl.agg.access.core.AggregatorAccessWFilterBase;
import com.espertech.esper.common.internal.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;
import static com.espertech.esper.common.internal.epl.agg.method.core.AggregatorCodegenUtil.rowDotMember;

public class AggregatorAccessPlugin extends AggregatorAccessWFilterBase {

    private final CodegenExpressionMember state;
    private final AggregationMultiFunctionStateModeManaged mode;

    public AggregatorAccessPlugin(int col, boolean join, CodegenCtor ctor, CodegenMemberCol membersColumnized, CodegenClassScope classScope, ExprNode optionalFilter, AggregationMultiFunctionStateModeManaged mode) {
        super(optionalFilter);
        state = membersColumnized.addMember(col, AggregationMultiFunctionState.class, "state");
        this.mode = mode;

        InjectionStrategyClassNewInstance injectionStrategy = (InjectionStrategyClassNewInstance) mode.getInjectionStrategyAggregationStateFactory();
        CodegenExpressionField factoryField = classScope.addFieldUnshared(true, AggregationMultiFunctionStateFactory.class, injectionStrategy.getInitializationExpression(classScope));
        ctor.getBlock().assignRef(state, exprDotMethod(factoryField, "newState", constantNull()));
    }

    protected void applyEnterFiltered(CodegenMethod method, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        method.getBlock().exprDotMethod(state, "applyEnter", symbols.getAddEPS(method), symbols.getAddExprEvalCtx(method));
    }

    protected void applyLeaveFiltered(CodegenMethod method, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        method.getBlock().exprDotMethod(state, "applyLeave", symbols.getAddEPS(method), symbols.getAddExprEvalCtx(method));
    }

    public void clearCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().exprDotMethod(state, "clear");
    }

    public void writeCodegen(CodegenExpressionRef row, int col, CodegenExpressionRef ref, CodegenExpressionRef unitKey, CodegenExpressionRef output, CodegenMethod method, CodegenClassScope classScope) {
        if (mode.isHasHA()) {
            method.getBlock().expression(staticMethod(mode.getSerde(), "write", output, rowDotMember(row, state)));
        }
    }

    public void readCodegen(CodegenExpressionRef row, int col, CodegenExpressionRef input, CodegenMethod method, CodegenExpressionRef unitKey, CodegenClassScope classScope) {
        if (mode.isHasHA()) {
            method.getBlock().assignRef(rowDotMember(row, state), staticMethod(mode.getSerde(), "read", input));
        }
    }

    public static CodegenExpression codegenGetAccessTableState(int column) {
        return memberCol("state", column);
    }
}
