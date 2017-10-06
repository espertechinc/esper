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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.service.resource.StatementResourceHolder;
import com.espertech.esper.core.service.resource.StatementResourceService;
import com.espertech.esper.epl.agg.service.common.AggregationServiceTable;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.join.plan.QueryPlanIndexItem;
import com.espertech.esper.epl.lookup.EventTableIndexMetadata;
import com.espertech.esper.epl.lookup.IndexMultiKey;
import com.espertech.esper.epl.table.upd.TableUpdateStrategy;
import com.espertech.esper.epl.table.upd.TableUpdateStrategyFactory;
import com.espertech.esper.epl.table.upd.TableUpdateStrategyReceiver;
import com.espertech.esper.epl.updatehelper.EventBeanUpdateHelper;
import com.espertech.esper.event.arr.ObjectArrayEventType;

import java.util.*;

public class TableMetadata {

    private final String tableName;
    private final String eplExpression;
    private final String statementName;
    private final Class[] keyTypes;
    private final Map<String, TableMetadataColumn> tableColumns;
    private final TableStateRowFactory rowFactory;
    private final int numberMethodAggregations;
    private final StatementContext statementContextCreateTable;
    private final ObjectArrayEventType internalEventType;
    private final ObjectArrayEventType publicEventType;
    private final TableMetadataInternalEventToPublic eventToPublic;
    private final boolean queryPlanLogging;

    private final Map<String, List<TableUpdateStrategyReceiverDesc>> stmtNameToUpdateStrategyReceivers = new HashMap<String, List<TableUpdateStrategyReceiverDesc>>();
    private final EventTableIndexMetadata eventTableIndexMetadataRepo = new EventTableIndexMetadata();

    private TableStateFactory tableStateFactory;
    private TableMetadataContext tableMetadataContext;
    private TableRowKeyFactory tableRowKeyFactory;

    public TableMetadata(String tableName, String eplExpression, String statementName, Class[] keyTypes, Map<String, TableMetadataColumn> tableColumns, TableStateRowFactory rowFactory, int numberMethodAggregations, StatementContext createTableStatementContext, ObjectArrayEventType internalEventType, ObjectArrayEventType publicEventType, TableMetadataInternalEventToPublic eventToPublic, boolean queryPlanLogging)
            throws ExprValidationException {
        this.tableName = tableName;
        this.eplExpression = eplExpression;
        this.statementName = statementName;
        this.keyTypes = keyTypes;
        this.tableColumns = tableColumns;
        this.rowFactory = rowFactory;
        this.numberMethodAggregations = numberMethodAggregations;
        this.statementContextCreateTable = createTableStatementContext;
        this.internalEventType = internalEventType;
        this.publicEventType = publicEventType;
        this.eventToPublic = eventToPublic;
        this.queryPlanLogging = queryPlanLogging;

        if (keyTypes.length > 0) {
            Pair<int[], IndexMultiKey> pair = TableServiceUtil.getIndexMultikeyForKeys(tableColumns, internalEventType);
            QueryPlanIndexItem queryPlanIndexItem = QueryPlanIndexItem.fromIndexMultikeyTablePrimaryKey(pair.getSecond());
            eventTableIndexMetadataRepo.addIndexExplicit(true, pair.getSecond(), tableName, queryPlanIndexItem, createTableStatementContext.getStatementName());
            tableRowKeyFactory = new TableRowKeyFactory(pair.getFirst());
        }
    }

    public Class[] getKeyTypes() {
        return keyTypes;
    }

    public TableStateFactory getTableStateFactory() {
        return tableStateFactory;
    }

    public Map<String, TableMetadataColumn> getTableColumns() {
        return tableColumns;
    }

    public TableStateRowFactory getRowFactory() {
        return rowFactory;
    }

    public int getNumberMethodAggregations() {
        return numberMethodAggregations;
    }

    public String getContextName() {
        return statementContextCreateTable.getContextName();
    }

    public ObjectArrayEventType getInternalEventType() {
        return internalEventType;
    }

    public boolean isQueryPlanLogging() {
        return queryPlanLogging;
    }

    public Set<String> getUniqueKeyProps() {
        Set<String> keys = new LinkedHashSet<String>();
        for (Map.Entry<String, TableMetadataColumn> entry : tableColumns.entrySet()) {
            if (entry.getValue().isKey()) {
                keys.add(entry.getKey());
            }
        }
        return keys;
    }

    public void setTableStateFactory(TableStateFactory tableStateFactory) {
        this.tableStateFactory = tableStateFactory;
    }

    public String getTableName() {
        return tableName;
    }

    public EventTableIndexMetadata getEventTableIndexMetadataRepo() {
        return eventTableIndexMetadataRepo;
    }

    public EventBean getPublicEventBean(EventBean event, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return eventToPublic.convert(event, eventsPerStream, isNewData, context);
    }

    public EventType getPublicEventType() {
        return publicEventType;
    }

    public TableMetadataInternalEventToPublic getEventToPublic() {
        return eventToPublic;
    }

    public void validateAddIndexAssignUpdateStrategies(String createIndexStatementName, IndexMultiKey imk, String explicitIndexName, QueryPlanIndexItem explicitIndexDesc) throws ExprValidationException {
        // add index - for now
        eventTableIndexMetadataRepo.addIndexExplicit(false, imk, explicitIndexName, explicitIndexDesc, createIndexStatementName);

        // validate strategies, rollback if required
        for (Map.Entry<String, List<TableUpdateStrategyReceiverDesc>> stmtEntry : stmtNameToUpdateStrategyReceivers.entrySet()) {
            for (TableUpdateStrategyReceiverDesc strategyReceiver : stmtEntry.getValue()) {
                try {
                    TableUpdateStrategyFactory.validateGetTableUpdateStrategy(this, strategyReceiver.getUpdateHelper(), strategyReceiver.isOnMerge());
                } catch (ExprValidationException ex) {
                    eventTableIndexMetadataRepo.removeIndex(imk);
                    throw new ExprValidationException("Failed to validate statement '" + stmtEntry.getKey() + "' as a recipient of the proposed index: " + ex.getMessage());
                }
            }
        }

        // assign new strategies
        for (Map.Entry<String, List<TableUpdateStrategyReceiverDesc>> stmtEntry : stmtNameToUpdateStrategyReceivers.entrySet()) {
            for (TableUpdateStrategyReceiverDesc strategyReceiver : stmtEntry.getValue()) {
                TableUpdateStrategy strategy = TableUpdateStrategyFactory.validateGetTableUpdateStrategy(this, strategyReceiver.getUpdateHelper(), strategyReceiver.isOnMerge());
                strategyReceiver.getReceiver().update(strategy);
            }
        }
    }

    public void addTableUpdateStrategyReceiver(String statementName, TableUpdateStrategyReceiver receiver, EventBeanUpdateHelper updateHelper, boolean onMerge) {
        List<TableUpdateStrategyReceiverDesc> receivers = stmtNameToUpdateStrategyReceivers.get(statementName);
        if (receivers == null) {
            receivers = new ArrayList<TableUpdateStrategyReceiverDesc>(2);
            stmtNameToUpdateStrategyReceivers.put(statementName, receivers);
        }
        receivers.add(new TableUpdateStrategyReceiverDesc(receiver, updateHelper, onMerge));
    }

    public void removeTableUpdateStrategyReceivers(String statementName) {
        stmtNameToUpdateStrategyReceivers.remove(statementName);
    }

    public void addIndexReference(String indexName, String statementName) {
        eventTableIndexMetadataRepo.addIndexReference(indexName, statementName);
    }

    public void removeIndexReferencesStatement(String statementName) {
        Collection<String> indexesDereferenced = eventTableIndexMetadataRepo.getRemoveRefIndexesDereferenced(statementName);
        for (String indexDereferenced : indexesDereferenced) {
            // remove tables
            for (int agentInstanceId : getAgentInstanceIds()) {
                TableStateInstance state = getState(agentInstanceId);
                if (state != null) {
                    IndexMultiKey mk = state.getIndexRepository().getIndexByName(indexDereferenced);
                    if (mk != null) {
                        state.getIndexRepository().removeIndex(mk);
                    }
                }
            }
        }
    }

    public TableStateInstance getState(int agentInstanceId) {
        StatementResourceService createTableResources = statementContextCreateTable.getStatementExtensionServicesContext().getStmtResources();

        StatementResourceHolder holder = null;
        if (statementContextCreateTable.getContextName() == null) {
            holder = createTableResources.getResourcesUnpartitioned();
        } else {
            if (createTableResources.getResourcesPartitioned() != null) {
                holder = createTableResources.getResourcesPartitioned().get(agentInstanceId);
            }
        }
        if (holder == null) {
            return null;
        }

        AggregationServiceTable aggsvc = (AggregationServiceTable) holder.getAggregationService();
        return aggsvc.getTableState();
    }

    public Collection<Integer> getAgentInstanceIds() {
        StatementResourceService createTableResources = statementContextCreateTable.getStatementExtensionServicesContext().getStmtResources();

        if (statementContextCreateTable.getContextName() == null) {
            return Collections.singleton(-1);
        }
        if (createTableResources.getResourcesPartitioned() != null) {
            return createTableResources.getResourcesPartitioned().keySet();
        }
        return Collections.singleton(-1);
    }

    public String[][] getUniqueIndexes() {
        return eventTableIndexMetadataRepo.getUniqueIndexProps();
    }

    public void setTableMetadataContext(TableMetadataContext tableMetadataContext) {
        this.tableMetadataContext = tableMetadataContext;
    }

    public TableMetadataContext getTableMetadataContext() {
        return tableMetadataContext;
    }

    public TableRowKeyFactory getTableRowKeyFactory() {
        return tableRowKeyFactory;
    }

    public void clearTableInstances() {
        for (int agentInstanceId : getAgentInstanceIds()) {
            TableStateInstance state = getState(agentInstanceId);
            if (state != null) {
                state.destroyInstance();
            }
        }
    }

    public String getEplExpression() {
        return eplExpression;
    }

    public String getStatementName() {
        return statementName;
    }

    public StatementContext getStatementContextCreateTable() {
        return statementContextCreateTable;
    }
}
