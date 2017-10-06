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
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMembersColumnized;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.core.CodegenCtor;
import com.espertech.esper.codegen.core.CodegenNamedMethods;
import com.espertech.esper.epl.agg.access.AggregationState;
import com.espertech.esper.epl.agg.service.common.AggregationStateFactory;
import com.espertech.esper.epl.agg.service.common.AggregationStateFactoryForge;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.accessagg.ExprPlugInAggMultiFunctionNodeFactory;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.plugin.*;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class AggregationStatePluginForge implements AggregationStateFactoryForge {

    protected final ExprPlugInAggMultiFunctionNodeFactory parent;
    protected final PlugInAggregationMultiFunctionStateForge stateForge;

    public AggregationStatePluginForge(ExprPlugInAggMultiFunctionNodeFactory parent) {
        this.parent = parent;
        this.stateForge = parent.getHandlerPlugin().getStateForge();
    }

    public AggregationStateFactory makeFactory(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
        return new AggregationStatePluginFactory(parent, stateForge);
    }

    public void rowMemberCodegen(int stateNumber, CodegenCtor ctor, CodegenMembersColumnized membersColumnized, CodegenClassScope classScope) {
        if (parent.getHandlerPlugin().getCodegenType() == PlugInAggregationMultiFunctionCodegenType.CODEGEN_NONE) {
            CodegenMember factory = classScope.makeAddMember(PlugInAggregationMultiFunctionStateFactory.class, stateForge.getStateFactory());
            membersColumnized.addMember(stateNumber, AggregationState.class, "state");
            ctor.getBlock().assignRef(refCol("state", stateNumber), exprDotMethod(member(factory.getMemberId()), "makeAggregationState", constantNull()));
            return;
        }
        PlugInAggregationMultiFunctionStateForgeCodegenRowMemberContext ctx = new PlugInAggregationMultiFunctionStateForgeCodegenRowMemberContext(parent, stateNumber, ctor, membersColumnized, classScope);
        stateForge.rowMemberCodegen(ctx);
    }

    public void applyEnterCodegen(int stateNumber, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        applyCodegen(true, stateNumber, method, symbols, classScope, namedMethods);
    }

    public void applyLeaveCodegen(int stateNumber, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        applyCodegen(false, stateNumber, method, symbols, classScope, namedMethods);
    }

    public void clearCodegen(int stateNumber, CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        if (parent.getHandlerPlugin().getCodegenType() == PlugInAggregationMultiFunctionCodegenType.CODEGEN_NONE) {
            method.getBlock().exprDotMethod(refCol("state", stateNumber), "clear");
            return;
        }
        PlugInAggregationMultiFunctionStateForgeCodegenClearContext ctx = new PlugInAggregationMultiFunctionStateForgeCodegenClearContext(stateNumber, method, classScope, namedMethods);
        stateForge.clearCodegen(ctx);
    }

    public ExprPlugInAggMultiFunctionNodeFactory getParent() {
        return parent;
    }

    public PlugInAggregationMultiFunctionStateForge getStateForge() {
        return stateForge;
    }

    private void applyCodegen(boolean enter, int stateNumber, CodegenMethodNode method, ExprForgeCodegenSymbol symbols, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        if (parent.getHandlerPlugin().getCodegenType() == PlugInAggregationMultiFunctionCodegenType.CODEGEN_NONE) {
            method.getBlock().exprDotMethod(refCol("state", stateNumber), enter ? "applyEnter" : "applyLeave", symbols.getAddEPS(method), symbols.getAddExprEvalCtx(method));
            return;
        }
        PlugInAggregationMultiFunctionStateForgeCodegenApplyContext ctx = new PlugInAggregationMultiFunctionStateForgeCodegenApplyContext(parent, stateNumber, method, symbols, classScope, namedMethods);
        if (enter) {
            stateForge.applyEnterCodegen(ctx);
        } else {
            stateForge.applyLeaveCodegen(ctx);
        }
    }
}

