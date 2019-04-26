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
package com.espertech.esper.common.internal.epl.table.core;

import com.espertech.esper.common.client.EventPropertyValueGetter;
import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.collection.MultiKeyFromMultiKey;
import com.espertech.esper.common.internal.collection.MultiKeyFromObjectArray;
import com.espertech.esper.common.internal.context.util.StatementContext;
import com.espertech.esper.common.internal.epl.agg.core.AggregationRowFactory;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.index.hash.PropertyHashedEventTableFactory;
import com.espertech.esper.common.internal.epl.join.lookup.IndexMultiKey;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanIndexItem;
import com.espertech.esper.common.internal.epl.lookupplansubord.EventTableIndexMetadata;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.epl.table.update.TableUpdateStrategyRedoCallback;
import com.espertech.esper.common.internal.statement.resource.StatementResourceHolder;
import com.espertech.esper.common.internal.statement.resource.StatementResourceService;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class TableBase implements Table {
    protected final TableMetaData metaData;
    protected TableMetadataInternalEventToPublic eventToPublic;
    protected StatementContext statementContextCreateTable;
    protected AggregationRowFactory aggregationRowFactory;
    protected TableSerdes tableSerdes;
    protected EventPropertyValueGetter primaryKeyGetter;
    protected DataInputOutputSerde<Object> primaryKeySerde;
    protected MultiKeyFromObjectArray primaryKeyObjectArrayTransform;
    protected MultiKeyFromMultiKey primaryKeyIntoTableTransform;
    protected Set<TableUpdateStrategyRedoCallback> updateStrategyRedoCallbacks = new HashSet<>(4);
    protected PropertyHashedEventTableFactory primaryKeyIndexFactory;

    protected abstract PropertyHashedEventTableFactory setupPrimaryKeyIndexFactory();

    public TableBase(TableMetaData metaData) {
        this.metaData = metaData;
    }

    public void setEventToPublic(TableMetadataInternalEventToPublic eventToPublic) {
        this.eventToPublic = eventToPublic;
    }

    public void setStatementContextCreateTable(StatementContext statementContextCreateTable) {
        this.statementContextCreateTable = statementContextCreateTable;
    }

    public void setAggregationRowFactory(AggregationRowFactory aggregationRowFactory) {
        this.aggregationRowFactory = aggregationRowFactory;
    }

    public void setTableSerdes(TableSerdes tableSerdes) {
        this.tableSerdes = tableSerdes;
    }

    public void setPrimaryKeyGetter(EventPropertyValueGetter primaryKeyGetter) {
        this.primaryKeyGetter = primaryKeyGetter;
    }

    public void setPrimaryKeySerde(DataInputOutputSerde<Object> primaryKeySerde) {
        this.primaryKeySerde = primaryKeySerde;
    }

    public void setPrimaryKeyObjectArrayTransform(MultiKeyFromObjectArray primaryKeyObjectArrayTransform) {
        this.primaryKeyObjectArrayTransform = primaryKeyObjectArrayTransform;
    }

    public void setPrimaryKeyIntoTableTransform(MultiKeyFromMultiKey primaryKeyIntoTableTransform) {
        this.primaryKeyIntoTableTransform = primaryKeyIntoTableTransform;
    }

    public void tableReady() {
        if (metaData.isKeyed()) {
            this.primaryKeyIndexFactory = setupPrimaryKeyIndexFactory();
        }
    }

    public TableMetadataInternalEventToPublic getEventToPublic() {
        return eventToPublic;
    }

    public TableMetaData getMetaData() {
        return metaData;
    }

    public TableSerdes getTableSerdes() {
        return tableSerdes;
    }

    public AggregationRowFactory getAggregationRowFactory() {
        return aggregationRowFactory;
    }

    public StatementContext getStatementContextCreateTable() {
        return statementContextCreateTable;
    }

    public PropertyHashedEventTableFactory getPrimaryIndexFactory() {
        return primaryKeyIndexFactory;
    }

    public boolean isGrouped() {
        return metaData.getKeyTypes() != null && metaData.getKeyTypes().length > 0;
    }

    public String getName() {
        return metaData.getTableName();
    }

    public EventTableIndexMetadata getEventTableIndexMetadata() {
        return metaData.getIndexMetadata();
    }

    public TableInstance getTableInstanceNoRemake(int agentInstanceId) {
        if (metaData.getOptionalContextName() == null) {
            return getTableInstanceNoContext();
        }

        StatementResourceService statementResourceService = statementContextCreateTable.getStatementCPCacheService().getStatementResourceService();
        StatementResourceHolder holder = statementResourceService.getPartitioned(agentInstanceId);
        return holder == null ? null : holder.getTableInstance();
    }

    public TableInstance getTableInstanceNoContextNoRemake() {
        StatementResourceService statementResourceService = statementContextCreateTable.getStatementCPCacheService().getStatementResourceService();
        StatementResourceHolder holder = statementResourceService.getUnpartitioned();
        return holder == null ? null : holder.getTableInstance();
    }

    public void validateAddIndex(String deploymentId, String statementName, String indexName, String indexModuleName, QueryPlanIndexItem explicitIndexDesc, IndexMultiKey indexMultiKey) throws ExprValidationException {
        metaData.getIndexMetadata().addIndexExplicit(false, indexMultiKey, indexName, indexModuleName, explicitIndexDesc, deploymentId);
        for (TableUpdateStrategyRedoCallback callback : updateStrategyRedoCallbacks) {
            callback.initTableUpdateStrategy(this);
        }
    }

    public void removeIndexReferencesStmtMayRemoveIndex(IndexMultiKey indexMultiKey, String deploymentId, String statementName) {
        boolean last = metaData.getIndexMetadata().removeIndexReference(indexMultiKey, deploymentId);
        if (last) {
            metaData.getIndexMetadata().removeIndex(indexMultiKey);
            removeAllInstanceIndexes(indexMultiKey);
        }
    }

    public void removeAllInstanceIndexes(IndexMultiKey index) {
        StatementResourceService statementResourceService = statementContextCreateTable.getStatementCPCacheService().getStatementResourceService();
        if (metaData.getOptionalContextName() == null) {
            StatementResourceHolder holder = statementResourceService.getUnpartitioned();
            if (holder != null && holder.getTableInstance() != null) {
                holder.getTableInstance().getIndexRepository().removeIndex(index);
            }
        } else {
            for (Map.Entry<Integer, StatementResourceHolder> entry : statementResourceService.getResourcesPartitioned().entrySet()) {
                if (entry.getValue().getTableInstance() != null) {
                    entry.getValue().getTableInstance().getIndexRepository().removeIndex(index);
                }
            }
        }
    }

    public void addUpdateStrategyCallback(TableUpdateStrategyRedoCallback callback) {
        updateStrategyRedoCallbacks.add(callback);
    }

    public void removeUpdateStrategyCallback(TableUpdateStrategyRedoCallback callback) {
        updateStrategyRedoCallbacks.remove(callback);
    }

    public Collection<TableUpdateStrategyRedoCallback> getUpdateStrategyCallbacks() {
        return updateStrategyRedoCallbacks;
    }

    public MultiKeyFromMultiKey getPrimaryKeyIntoTableTransform() {
        return primaryKeyIntoTableTransform;
    }

    public MultiKeyFromObjectArray getPrimaryKeyObjectArrayTransform() {
        return primaryKeyObjectArrayTransform;
    }

    public DataInputOutputSerde<Object> getPrimaryKeySerde() {
        return primaryKeySerde;
    }
}
