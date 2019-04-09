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
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyCodegen;
import com.espertech.esper.common.internal.context.activator.ViewableActivatorForge;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.common.internal.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.common.internal.epl.lookupplansubord.SubordinateWMatchExprQueryPlanForge;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.subselect.SubSelectFactoryForge;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.core.TableDeployTimeResolver;
import com.espertech.esper.common.internal.epl.table.strategy.ExprTableEvalStrategyFactoryForge;

import java.util.Map;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constant;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constantNull;

public class StatementAgentInstanceFactoryOnTriggerInfraSelectForge extends StatementAgentInstanceFactoryOnTriggerInfraBaseForge {
    private final String resultSetProcessorProviderClassName;
    private final boolean insertInto;
    private final boolean addToFront;
    private final TableMetaData optionalInsertIntoTable;
    private final boolean selectAndDelete;
    private final boolean distinct;
    private final MultiKeyClassRef distinctMultiKey;

    public StatementAgentInstanceFactoryOnTriggerInfraSelectForge(ViewableActivatorForge activator, EventType resultEventType, Map<ExprSubselectNode, SubSelectFactoryForge> subselects, Map<ExprTableAccessNode, ExprTableEvalStrategyFactoryForge> tableAccesses, NamedWindowMetaData namedWindow, TableMetaData table, SubordinateWMatchExprQueryPlanForge queryPlanForge, String resultSetProcessorProviderClassName, boolean insertInto, boolean addToFront, TableMetaData optionalInsertIntoTable, boolean selectAndDelete, boolean distinct, MultiKeyClassRef distinctMultiKey) {
        super(activator, resultEventType, subselects, tableAccesses, null, namedWindow, table, queryPlanForge);
        this.resultSetProcessorProviderClassName = resultSetProcessorProviderClassName;
        this.insertInto = insertInto;
        this.addToFront = addToFront;
        this.optionalInsertIntoTable = optionalInsertIntoTable;
        this.selectAndDelete = selectAndDelete;
        this.distinct = distinct;
        this.distinctMultiKey = distinctMultiKey;
    }

    public Class typeOfSubclass() {
        return StatementAgentInstanceFactoryOnTriggerInfraSelect.class;
    }

    protected void inlineInitializeOnTriggerSpecific(CodegenExpressionRef saiff, CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        method.getBlock()
                .exprDotMethod(saiff, "setResultSetProcessorFactoryProvider", CodegenExpressionBuilder.newInstance(resultSetProcessorProviderClassName, symbols.getAddInitSvc(method)))
                .exprDotMethod(saiff, "setInsertInto", constant(insertInto))
                .exprDotMethod(saiff, "setAddToFront", constant(addToFront))
                .exprDotMethod(saiff, "setSelectAndDelete", constant(selectAndDelete))
                .exprDotMethod(saiff, "setDistinct", constant(distinct))
                .exprDotMethod(saiff, "setDistinctKeyGetter", MultiKeyCodegen.codegenGetterEventDistinct(distinct, getResultEventType(), distinctMultiKey, method, classScope))
                .exprDotMethod(saiff, "setOptionalInsertIntoTable", optionalInsertIntoTable == null ? constantNull() : TableDeployTimeResolver.makeResolveTable(optionalInsertIntoTable, symbols.getAddInitSvc(method)));

    }
}
