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
package com.espertech.esper.common.internal.context.aifactory.ontrigger.core;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.activator.ViewableActivatorForge;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.common.internal.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.common.internal.epl.subselect.SubSelectFactoryForge;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategyFactoryForge;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategyUtil;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;

import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

public abstract class StatementAgentInstanceFactoryOnTriggerBaseForge {

    private final ViewableActivatorForge activator;
    private final EventType resultEventType;
    private final Map<ExprSubselectNode, SubSelectFactoryForge> subselects;
    private final Map<ExprTableAccessNode, ExprTableEvalStrategyFactoryForge> tableAccesses;

    public StatementAgentInstanceFactoryOnTriggerBaseForge(ViewableActivatorForge activator, EventType resultEventType, Map<ExprSubselectNode, SubSelectFactoryForge> subselects, Map<ExprTableAccessNode, ExprTableEvalStrategyFactoryForge> tableAccesses) {
        this.activator = activator;
        this.resultEventType = resultEventType;
        this.subselects = subselects;
        this.tableAccesses = tableAccesses;
    }

    public abstract Class typeOfSubclass();

    public abstract void inlineInitializeOnTriggerBase(CodegenExpressionRef saiff, CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope);

    public final CodegenMethod initializeCodegen(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(typeOfSubclass(), this.getClass(), classScope);
        method.getBlock()
                .declareVar(typeOfSubclass(), "saiff", newInstance(typeOfSubclass()))
                .exprDotMethod(ref("saiff"), "setActivator", activator.makeCodegen(method, symbols, classScope))
                .exprDotMethod(ref("saiff"), "setResultEventType", EventTypeUtility.resolveTypeCodegen(resultEventType, symbols.getAddInitSvc(method)))
                .exprDotMethod(ref("saiff"), "setSubselects", SubSelectFactoryForge.codegenInitMap(subselects, this.getClass(), method, symbols, classScope))
                .exprDotMethod(ref("saiff"), "setTableAccesses", ExprTableEvalStrategyUtil.codegenInitMap(tableAccesses, this.getClass(), method, symbols, classScope));
        inlineInitializeOnTriggerBase(ref("saiff"), method, symbols, classScope);
        method.getBlock().methodReturn(ref("saiff"));
        return method;
    }

    public EventType getResultEventType() {
        return resultEventType;
    }
}
