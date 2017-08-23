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

import com.espertech.esper.client.ConfigurationPlugInAggregationMultiFunction;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.expression.baseagg.ExprAggregateNodeBase;
import com.espertech.esper.epl.expression.core.ExprChainedSpec;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.table.ExprTableIdentNode;
import com.espertech.esper.epl.expression.table.ExprTableIdentNodeSubpropAccessor;
import com.espertech.esper.epl.join.plan.QueryPlanIndexItem;
import com.espertech.esper.epl.lookup.IndexMultiKey;
import com.espertech.esper.epl.parse.ASTAggregationHelper;
import com.espertech.esper.epl.table.strategy.*;
import com.espertech.esper.epl.table.upd.TableUpdateStrategy;
import com.espertech.esper.epl.table.upd.TableUpdateStrategyFactory;
import com.espertech.esper.epl.table.upd.TableUpdateStrategyReceiver;
import com.espertech.esper.epl.updatehelper.EventBeanUpdateHelper;
import com.espertech.esper.event.arr.ObjectArrayEventType;
import com.espertech.esper.plugin.PlugInAggregationMultiFunctionFactory;
import com.espertech.esper.util.AuditPath;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.util.LazyAllocatedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.locks.Lock;

public class TableServiceImpl implements TableService {

    private static final Logger QUERY_PLAN_LOG = LoggerFactory.getLogger(AuditPath.QUERYPLAN_LOG);

    private final Map<String, TableMetadata> tables = new HashMap<String, TableMetadata>();
    private final TableExprEvaluatorContext tableExprEvaluatorContext = new TableExprEvaluatorContext();

    public TableServiceImpl() {
    }

    public void validateAddIndex(String createIndexStatementName, TableMetadata tableMetadata, String explicitIndexName, QueryPlanIndexItem explicitIndexDesc, IndexMultiKey imk) throws ExprValidationException {
        tableMetadata.validateAddIndexAssignUpdateStrategies(createIndexStatementName, imk, explicitIndexName, explicitIndexDesc);
    }

    public TableUpdateStrategy getTableUpdateStrategy(TableMetadata tableMetadata, EventBeanUpdateHelper updateHelper, boolean isOnMerge)
            throws ExprValidationException {
        return TableUpdateStrategyFactory.validateGetTableUpdateStrategy(tableMetadata, updateHelper, isOnMerge);
    }

    public Collection<Integer> getAgentInstanceIds(String name) {
        TableMetadata metadata = tables.get(name);
        if (metadata == null) {
            throw new IllegalArgumentException("Failed to find table for name '" + name + "'");
        }
        return metadata.getAgentInstanceIds();
    }

    public TableExprEvaluatorContext getTableExprEvaluatorContext() {
        return tableExprEvaluatorContext;
    }

    public TableMetadata getTableMetadata(String tableName) {
        return tables.get(tableName);
    }

    public TableMetadata addTable(String tableName, String eplExpression, String statementName, Class[] keyTypes, Map<String, TableMetadataColumn> tableColumns, TableStateRowFactory tableStateRowFactory, int numberMethodAggregations, StatementContext statementContext, ObjectArrayEventType internalEventType, ObjectArrayEventType publicEventType, TableMetadataInternalEventToPublic eventToPublic, boolean queryPlanLogging) throws ExprValidationException {
        final TableMetadata metadata = new TableMetadata(tableName, eplExpression, statementName, keyTypes, tableColumns, tableStateRowFactory, numberMethodAggregations, statementContext, internalEventType, publicEventType, eventToPublic, queryPlanLogging);

        // determine table state factory
        TableStateFactory tableStateFactory;
        if (keyTypes.length == 0) { // ungrouped
            tableStateFactory = new TableStateFactory() {
                public TableStateInstance makeTableState(AgentInstanceContext agentInstanceContext) {
                    return new TableStateInstanceUngroupedImpl(metadata, agentInstanceContext);
                }
            };
        } else {
            tableStateFactory = new TableStateFactory() {
                public TableStateInstance makeTableState(AgentInstanceContext agentInstanceContext) {
                    return new TableStateInstanceGroupedImpl(metadata, agentInstanceContext);
                }
            };
        }
        metadata.setTableStateFactory(tableStateFactory);

        tables.put(tableName, metadata);
        return metadata;
    }

    public void removeTableIfFound(String tableName) {
        TableMetadata metadata = tables.remove(tableName);
        if (metadata != null) {
            metadata.clearTableInstances();
        }
    }

    public TableStateInstance getState(String name, int agentInstanceId) {
        return assertGetState(name, agentInstanceId);
    }

    private TableStateInstance assertGetState(String name, int agentInstanceId) {
        TableMetadata metadata = tables.get(name);
        if (metadata == null) {
            throw new IllegalArgumentException("Failed to find table for name '" + name + "'");
        }
        return metadata.getState(agentInstanceId);
    }

    public static Logger getQueryPlanLog() {
        return QUERY_PLAN_LOG;
    }

    public TableMetadata getTableMetadataFromEventType(EventType type) {
        String tableName = TableServiceUtil.getTableNameFromEventType(type);
        if (tableName == null) {
            return null;
        }
        return tables.get(tableName);
    }

    public Pair<ExprNode, List<ExprChainedSpec>> getTableNodeChainable(StreamTypeService streamTypeService,
                                                                       List<ExprChainedSpec> chainSpec,
                                                                       EngineImportService engineImportService)
            throws ExprValidationException {
        chainSpec = new ArrayList<ExprChainedSpec>(chainSpec);

        String unresolvedPropertyName = chainSpec.get(0).getName();
        StreamTableColWStreamName col = findTableColumnMayByPrefixed(streamTypeService, unresolvedPropertyName);
        if (col == null) {
            return null;
        }
        StreamTableColPair pair = col.getPair();
        if (pair.getColumn() instanceof TableMetadataColumnAggregation) {
            TableMetadataColumnAggregation agg = (TableMetadataColumnAggregation) pair.getColumn();

            if (chainSpec.size() > 1) {
                String candidateAccessor = chainSpec.get(1).getName();
                ExprAggregateNodeBase exprNode = (ExprAggregateNodeBase) ASTAggregationHelper.tryResolveAsAggregation(engineImportService, false, candidateAccessor, new LazyAllocatedMap<ConfigurationPlugInAggregationMultiFunction, PlugInAggregationMultiFunctionFactory>(), streamTypeService.getEngineURIQualifier());
                if (exprNode != null) {
                    ExprNode node = new ExprTableIdentNodeSubpropAccessor(pair.getStreamNum(), col.getOptionalStreamName(), agg, exprNode);
                    exprNode.addChildNodes(chainSpec.get(1).getParameters());
                    chainSpec.remove(0);
                    chainSpec.remove(0);
                    return new Pair<ExprNode, List<ExprChainedSpec>>(node, chainSpec);
                }
            }

            ExprTableIdentNode node = new ExprTableIdentNode(null, unresolvedPropertyName);
            ExprTableExprEvaluatorBase eval = ExprTableEvalStrategyFactory.getTableAccessEvalStrategy(node, pair.getTableMetadata().getTableName(), pair.getStreamNum(), agg);
            node.setEval(eval);
            chainSpec.remove(0);
            return new Pair<ExprNode, List<ExprChainedSpec>>(node, chainSpec);
        }
        return null;
    }

    public ExprTableIdentNode getTableIdentNode(StreamTypeService streamTypeService, String unresolvedPropertyName, String streamOrPropertyName)
            throws ExprValidationException {
        String propertyPrefixed = unresolvedPropertyName;
        if (streamOrPropertyName != null) {
            propertyPrefixed = streamOrPropertyName + "." + unresolvedPropertyName;
        }
        StreamTableColWStreamName col = findTableColumnMayByPrefixed(streamTypeService, propertyPrefixed);
        if (col == null) {
            return null;
        }
        StreamTableColPair pair = col.getPair();
        if (pair.getColumn() instanceof TableMetadataColumnAggregation) {
            TableMetadataColumnAggregation agg = (TableMetadataColumnAggregation) pair.getColumn();
            ExprTableIdentNode node = new ExprTableIdentNode(streamOrPropertyName, unresolvedPropertyName);
            ExprTableExprEvaluatorBase eval = ExprTableEvalStrategyFactory.getTableAccessEvalStrategy(node, pair.getTableMetadata().getTableName(), pair.getStreamNum(), agg);
            node.setEval(eval);
            return node;
        }
        return null;
    }

    public void addTableUpdateStrategyReceiver(TableMetadata tableMetadata, String statementName, TableUpdateStrategyReceiver receiver, EventBeanUpdateHelper updateHelper, boolean isOnMerge) {
        tableMetadata.addTableUpdateStrategyReceiver(statementName, receiver, updateHelper, isOnMerge);
    }

    public void removeTableUpdateStrategyReceivers(TableMetadata tableMetadata, String statementName) {
        tableMetadata.removeTableUpdateStrategyReceivers(statementName);
    }

    public String[] getTables() {
        return CollectionUtil.toArray(tables.keySet());
    }

    public TableAndLockProvider getStateProvider(String tableName, int agentInstanceId, boolean writesToTables) {
        TableStateInstance instance = assertGetState(tableName, agentInstanceId);
        Lock lock = writesToTables ? instance.getTableLevelRWLock().writeLock() : instance.getTableLevelRWLock().readLock();
        if (instance instanceof TableStateInstanceGrouped) {
            return new TableAndLockProviderGroupedImpl(new TableAndLockGrouped(lock, (TableStateInstanceGrouped) instance));
        } else {
            return new TableAndLockProviderUngroupedImpl(new TableAndLockUngrouped(lock, (TableStateInstanceUngrouped) instance));
        }
    }

    private StreamTableColWStreamName findTableColumnMayByPrefixed(StreamTypeService streamTypeService, String streamAndPropName)
            throws ExprValidationException {
        int indexDot = streamAndPropName.indexOf(".");
        if (indexDot == -1) {
            StreamTableColPair pair = findTableColumnAcrossStreams(streamTypeService, streamAndPropName);
            if (pair != null) {
                return new StreamTableColWStreamName(pair, null);
            }
        } else {
            String streamName = streamAndPropName.substring(0, indexDot);
            String colName = streamAndPropName.substring(indexDot + 1);
            int streamNum = streamTypeService.getStreamNumForStreamName(streamName);
            if (streamNum == -1) {
                return null;
            }
            StreamTableColPair pair = findTableColumnForType(streamNum, streamTypeService.getEventTypes()[streamNum], colName);
            if (pair != null) {
                return new StreamTableColWStreamName(pair, streamName);
            }
        }
        return null;
    }

    public void removeIndexReferencesStmtMayRemoveIndex(String statementName, TableMetadata tableMetadata) {
        tableMetadata.removeIndexReferencesStatement(statementName);
    }

    private StreamTableColPair findTableColumnAcrossStreams(StreamTypeService streamTypeService, String columnName)
            throws ExprValidationException {
        StreamTableColPair found = null;
        for (int i = 0; i < streamTypeService.getEventTypes().length; i++) {
            EventType type = streamTypeService.getEventTypes()[i];
            StreamTableColPair pair = findTableColumnForType(i, type, columnName);
            if (pair == null) {
                continue;
            }
            if (found != null) {
                if (streamTypeService.isStreamZeroUnambigous() && found.getStreamNum() == 0) {
                    continue;
                }
                throw new ExprValidationException("Ambiguous table column '" + columnName + "' should be prefixed by a stream name");
            }
            found = pair;
        }
        return found;
    }

    private StreamTableColPair findTableColumnForType(int streamNum, EventType type, String columnName) {
        TableMetadata tableMetadata = getTableMetadataFromEventType(type);
        if (tableMetadata != null) {
            TableMetadataColumn column = tableMetadata.getTableColumns().get(columnName);
            if (column != null) {
                return new StreamTableColPair(streamNum, column, tableMetadata);
            }
        }
        return null;
    }

    private static class StreamTableColPair {
        private final int streamNum;
        private final TableMetadataColumn column;
        private final TableMetadata tableMetadata;

        private StreamTableColPair(int streamNum, TableMetadataColumn column, TableMetadata tableMetadata) {
            this.streamNum = streamNum;
            this.column = column;
            this.tableMetadata = tableMetadata;
        }

        public int getStreamNum() {
            return streamNum;
        }

        public TableMetadataColumn getColumn() {
            return column;
        }

        public TableMetadata getTableMetadata() {
            return tableMetadata;
        }
    }

    private static class StreamTableColWStreamName {
        private final StreamTableColPair pair;
        private final String optionalStreamName;

        private StreamTableColWStreamName(StreamTableColPair pair, String optionalStreamName) {
            this.pair = pair;
            this.optionalStreamName = optionalStreamName;
        }

        public StreamTableColPair getPair() {
            return pair;
        }

        public String getOptionalStreamName() {
            return optionalStreamName;
        }
    }
}
