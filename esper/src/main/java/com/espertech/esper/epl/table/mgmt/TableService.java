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
package com.espertech.esper.epl.table.mgmt;

import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.expression.core.ExprChainedSpec;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.table.ExprTableIdentNode;
import com.espertech.esper.epl.join.plan.QueryPlanIndexItem;
import com.espertech.esper.epl.lookup.IndexMultiKey;
import com.espertech.esper.epl.table.strategy.TableAndLockProvider;
import com.espertech.esper.epl.table.upd.TableUpdateStrategy;
import com.espertech.esper.epl.table.upd.TableUpdateStrategyReceiver;
import com.espertech.esper.epl.updatehelper.EventBeanUpdateHelper;
import com.espertech.esper.event.arr.ObjectArrayEventType;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface TableService {
    public final static String INTERNAL_RESERVED_PROPERTY = "internal-reserved";

    public String[] getTables();

    public TableExprEvaluatorContext getTableExprEvaluatorContext();

    public TableMetadata getTableMetadata(String tableName);

    public TableStateInstance getState(String name, int agentInstanceId);

    public void removeTableIfFound(String tableName);

    public ExprTableIdentNode getTableIdentNode(StreamTypeService streamTypeService, String unresolvedPropertyName, String streamOrPropertyName) throws ExprValidationException;

    public TableMetadata getTableMetadataFromEventType(EventType type);

    public Pair<ExprNode, List<ExprChainedSpec>> getTableNodeChainable(StreamTypeService streamTypeService, List<ExprChainedSpec> chainSpec, EngineImportService engineImportService) throws ExprValidationException;

    public Collection<Integer> getAgentInstanceIds(String tableName);

    public TableUpdateStrategy getTableUpdateStrategy(TableMetadata tableMetadata, EventBeanUpdateHelper updateHelper, boolean isOnMerge) throws ExprValidationException;

    public void addTableUpdateStrategyReceiver(TableMetadata tableMetadata, String statementName, TableUpdateStrategyReceiver receiver, EventBeanUpdateHelper updateHelper, boolean isOnMerge);

    public void removeTableUpdateStrategyReceivers(TableMetadata tableMetadata, String statementName);

    public void validateAddIndex(String createIndexStatementName, TableMetadata tableMetadata, String explicitIndexName, QueryPlanIndexItem explicitIndexDesc, IndexMultiKey imk) throws ExprValidationException;

    public void removeIndexReferencesStmtMayRemoveIndex(String statementName, TableMetadata tableMetadata);

    public TableMetadata addTable(String tableName, String eplExpression, String statementName, Class[] keyTypes, Map<String, TableMetadataColumn> tableColumns, TableStateRowFactory tableStateRowFactory, int numberMethodAggregations, StatementContext statementContext, ObjectArrayEventType internalEventType, ObjectArrayEventType publicEventType, TableMetadataInternalEventToPublic eventToPublic, boolean queryPlanLogging) throws ExprValidationException;

    public TableAndLockProvider getStateProvider(String tableName, int agentInstanceId, boolean writesToTables);
}
