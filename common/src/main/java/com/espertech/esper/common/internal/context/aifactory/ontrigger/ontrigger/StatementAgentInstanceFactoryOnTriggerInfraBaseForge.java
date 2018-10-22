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
package com.espertech.esper.common.internal.context.aifactory.ontrigger.ontrigger;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.context.activator.ViewableActivatorForge;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.aifactory.ontrigger.core.StatementAgentInstanceFactoryOnTriggerBaseForge;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.common.internal.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.common.internal.epl.lookupplansubord.SubordinateWMatchExprQueryPlanForge;
import com.espertech.esper.common.internal.epl.namedwindow.core.NamedWindowDeployTimeResolver;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.subselect.SubSelectFactoryForge;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.core.TableDeployTimeResolver;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategyFactoryForge;

import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constantNull;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

public abstract class StatementAgentInstanceFactoryOnTriggerInfraBaseForge extends StatementAgentInstanceFactoryOnTriggerBaseForge {
    protected final NamedWindowMetaData namedWindow;
    protected final TableMetaData table;
    private final SubordinateWMatchExprQueryPlanForge queryPlanForge;
    private final String nonSelectRSPProviderClassName;

    public StatementAgentInstanceFactoryOnTriggerInfraBaseForge(ViewableActivatorForge activator, EventType resultEventType, Map<ExprSubselectNode, SubSelectFactoryForge> subselects, Map<ExprTableAccessNode, ExprTableEvalStrategyFactoryForge> tableAccesses, String nonSelectRSPProviderClassName, NamedWindowMetaData namedWindow, TableMetaData table, SubordinateWMatchExprQueryPlanForge queryPlanForge) {
        super(activator, resultEventType, subselects, tableAccesses);
        this.nonSelectRSPProviderClassName = nonSelectRSPProviderClassName;
        this.namedWindow = namedWindow;
        this.table = table;
        this.queryPlanForge = queryPlanForge;
    }

    protected abstract void inlineInitializeOnTriggerSpecific(CodegenExpressionRef saiff, CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope);

    public void inlineInitializeOnTriggerBase(CodegenExpressionRef saiff, CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        method.getBlock()
                .exprDotMethod(saiff, "setNamedWindow", namedWindow == null ? constantNull() : NamedWindowDeployTimeResolver.makeResolveNamedWindow(namedWindow, symbols.getAddInitSvc(method)))
                .exprDotMethod(saiff, "setTable", table == null ? constantNull() : TableDeployTimeResolver.makeResolveTable(table, symbols.getAddInitSvc(method)))
                .exprDotMethod(saiff, "setQueryPlan", queryPlanForge.make(method, symbols, classScope))
                .exprDotMethod(saiff, "setNonSelectRSPFactoryProvider", nonSelectRSPProviderClassName == null ? constantNull() : newInstance(nonSelectRSPProviderClassName, symbols.getAddInitSvc(method)))
                .exprDotMethod(symbols.getAddInitSvc(method), "addReadyCallback", saiff); // add ready-callback

        inlineInitializeOnTriggerSpecific(saiff, method, symbols, classScope);
    }
}
