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
import com.espertech.esper.common.internal.epl.table.strategy.TableAndLockProvider;
import com.espertech.esper.common.internal.epl.table.update.TableUpdateStrategyRedoCallback;

import java.util.Collection;

public interface Table {
    void setEventToPublic(TableMetadataInternalEventToPublic eventToPublic);

    void setStatementContextCreateTable(StatementContext statementContextCreateTable);

    void setAggregationRowFactory(AggregationRowFactory aggregationRowFactory);

    void setTableSerdes(TableSerdes tableSerdes);

    void setPrimaryKeyGetter(EventPropertyValueGetter primaryKeyGetter);

    void setPrimaryKeySerde(DataInputOutputSerde<Object> primaryKeySerde);

    void setPrimaryKeyObjectArrayTransform(MultiKeyFromObjectArray primaryKeyObjectArrayTransform);

    void setPrimaryKeyIntoTableTransform(MultiKeyFromMultiKey primaryKeyIntoTableTransform);

    void tableReady();

    String getName();

    TableMetadataInternalEventToPublic getEventToPublic();

    TableMetaData getMetaData();

    TableSerdes getTableSerdes();

    AggregationRowFactory getAggregationRowFactory();

    TableAndLockProvider getStateProvider(int agentInstanceId, boolean writesToTables);

    StatementContext getStatementContextCreateTable();

    boolean isGrouped();

    EventTableIndexMetadata getEventTableIndexMetadata();

    PropertyHashedEventTableFactory getPrimaryIndexFactory();

    TableInstance getTableInstance(int agentInstanceId);

    TableInstance getTableInstanceNoContext();

    void validateAddIndex(String deploymentId, String statementName, String indexName, String indexModuleName, QueryPlanIndexItem explicitIndexDesc, IndexMultiKey indexMultiKey) throws ExprValidationException;

    void removeIndexReferencesStmtMayRemoveIndex(IndexMultiKey indexMultiKey, String deploymentId, String statementName);

    void removeAllInstanceIndexes(IndexMultiKey indexMultiKey);

    void addUpdateStrategyCallback(TableUpdateStrategyRedoCallback callback);

    void removeUpdateStrategyCallback(TableUpdateStrategyRedoCallback callback);

    Collection<TableUpdateStrategyRedoCallback> getUpdateStrategyCallbacks();

    DataInputOutputSerde<Object> getPrimaryKeySerde();

    MultiKeyFromMultiKey getPrimaryKeyIntoTableTransform();

    MultiKeyFromObjectArray getPrimaryKeyObjectArrayTransform();
}
