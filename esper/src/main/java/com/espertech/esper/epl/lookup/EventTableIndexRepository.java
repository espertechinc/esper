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
package com.espertech.esper.epl.lookup;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.join.hint.IndexHintInstruction;
import com.espertech.esper.epl.join.plan.QueryPlanIndexItem;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.join.table.EventTableAndNamePair;
import com.espertech.esper.epl.join.table.EventTableUtil;
import com.espertech.esper.epl.spec.CreateIndexItem;

import java.util.*;

/**
 * A repository of index tables for use with anything that
 * may use the indexes to correlate triggering events with indexed events.
 * <p>
 * Maintains index tables and keeps a reference count for user. Allows reuse of indexes for multiple
 * deleting statements.
 */
public class EventTableIndexRepository {
    private final List<EventTable> tables;
    private final Map<IndexMultiKey, EventTableIndexRepositoryEntry> tableIndexesRefCount;
    private final HashMap<String, EventTable> explicitIndexes;

    /**
     * Ctor.
     */
    public EventTableIndexRepository() {
        tables = new ArrayList<EventTable>();
        tableIndexesRefCount = new HashMap<IndexMultiKey, EventTableIndexRepositoryEntry>();
        explicitIndexes = new HashMap<String, EventTable>();
    }

    public Pair<IndexMultiKey, EventTableAndNamePair> addExplicitIndexOrReuse(
            boolean unique,
            List<IndexedPropDesc> hashProps,
            List<IndexedPropDesc> btreeProps,
            Iterable<EventBean> prefilledEvents,
            EventType indexedType,
            String indexName,
            AgentInstanceContext agentInstanceContext,
            Object optionalSerde) {
        if (hashProps.isEmpty() && btreeProps.isEmpty()) {
            throw new IllegalArgumentException("Invalid zero element list for hash and btree columns");
        }

        // Get an existing table, if any, matching the exact requirement
        IndexMultiKey indexPropKeyMatch = EventTableIndexUtil.findExactMatchNameAndType(tableIndexesRefCount.keySet(), unique, hashProps, btreeProps);
        if (indexPropKeyMatch != null) {
            EventTableIndexRepositoryEntry refTablePair = tableIndexesRefCount.get(indexPropKeyMatch);
            return new Pair<IndexMultiKey, EventTableAndNamePair>(indexPropKeyMatch, new EventTableAndNamePair(refTablePair.getTable(), refTablePair.getOptionalIndexName()));
        }

        return addIndex(unique, hashProps, btreeProps, prefilledEvents, indexedType, indexName, false, agentInstanceContext, optionalSerde);
    }

    public void addIndex(IndexMultiKey indexMultiKey, EventTableIndexRepositoryEntry entry) {
        tableIndexesRefCount.put(indexMultiKey, entry);
        tables.add(entry.getTable());
    }

    /**
     * Returns a list of current index tables in the repository.
     *
     * @return index tables
     */
    public List<EventTable> getTables() {
        return tables;
    }

    /**
     * Destroy indexes.
     */
    public void destroy() {
        for (EventTable table : tables) {
            table.destroy();
        }
        tables.clear();
        tableIndexesRefCount.clear();
    }

    public Pair<IndexMultiKey, EventTableAndNamePair> findTable(Set<String> keyPropertyNames, Set<String> rangePropertyNames, List<IndexHintInstruction> optionalIndexHintInstructions) {
        Pair<IndexMultiKey, EventTableIndexEntryBase> pair = EventTableIndexUtil.findIndexBestAvailable(tableIndexesRefCount, keyPropertyNames, rangePropertyNames, optionalIndexHintInstructions);
        if (pair == null) {
            return null;
        }
        EventTable tableFound = ((EventTableIndexRepositoryEntry) pair.getSecond()).getTable();
        return new Pair<IndexMultiKey, EventTableAndNamePair>(pair.getFirst(), new EventTableAndNamePair(tableFound, pair.getSecond().getOptionalIndexName()));
    }

    public IndexMultiKey[] getIndexDescriptors() {
        Set<IndexMultiKey> keySet = tableIndexesRefCount.keySet();
        return keySet.toArray(new IndexMultiKey[keySet.size()]);
    }

    public void validateAddExplicitIndex(boolean unique, String indexName, List<CreateIndexItem> columns, EventType eventType, Iterable<EventBean> dataWindowContents, AgentInstanceContext agentInstanceContext, boolean allowIndexExists, Object optionalSerde)
            throws ExprValidationException {
        if (explicitIndexes.containsKey(indexName)) {
            if (allowIndexExists) {
                return;
            }
            throw new ExprValidationException("Index by name '" + indexName + "' already exists");
        }

        EventTableCreateIndexDesc desc = EventTableIndexUtil.validateCompileExplicitIndex(unique, columns, eventType);
        addExplicitIndex(indexName, desc, eventType, dataWindowContents, agentInstanceContext, optionalSerde);
    }

    public void addExplicitIndex(String indexName, EventTableCreateIndexDesc desc, EventType eventType, Iterable<EventBean> dataWindowContents, AgentInstanceContext agentInstanceContext, Object optionalSerde) {
        Pair<IndexMultiKey, EventTableAndNamePair> pair = addExplicitIndexOrReuse(desc.isUnique(), desc.getHashProps(), desc.getBtreeProps(), dataWindowContents, eventType, indexName, agentInstanceContext, optionalSerde);
        explicitIndexes.put(indexName, pair.getSecond().getEventTable());
    }

    public EventTable getExplicitIndexByName(String indexName) {
        return explicitIndexes.get(indexName);
    }

    public EventTable getIndexByDesc(IndexMultiKey indexKey) {
        EventTableIndexRepositoryEntry entry = tableIndexesRefCount.get(indexKey);
        if (entry == null) {
            return null;
        }
        return entry.getTable();
    }

    private Pair<IndexMultiKey, EventTableAndNamePair> addIndex(boolean unique, List<IndexedPropDesc> hashProps, List<IndexedPropDesc> btreeProps, Iterable<EventBean> prefilledEvents, EventType indexedType, String indexName, boolean mustCoerce, AgentInstanceContext agentInstanceContext, Object optionalSerde) {

        // not resolved as full match and not resolved as unique index match, allocate
        IndexMultiKey indexPropKey = new IndexMultiKey(unique, hashProps, btreeProps);

        IndexedPropDesc[] indexedPropDescs = hashProps.toArray(new IndexedPropDesc[hashProps.size()]);
        String[] indexProps = IndexedPropDesc.getIndexProperties(indexedPropDescs);
        Class[] indexCoercionTypes = IndexedPropDesc.getCoercionTypes(indexedPropDescs);
        if (!mustCoerce) {
            indexCoercionTypes = null;
        }

        IndexedPropDesc[] rangePropDescs = btreeProps.toArray(new IndexedPropDesc[btreeProps.size()]);
        String[] rangeProps = IndexedPropDesc.getIndexProperties(rangePropDescs);
        Class[] rangeCoercionTypes = IndexedPropDesc.getCoercionTypes(rangePropDescs);

        QueryPlanIndexItem indexItem = new QueryPlanIndexItem(indexProps, indexCoercionTypes, rangeProps, rangeCoercionTypes, false);
        EventTable table = EventTableUtil.buildIndex(agentInstanceContext, 0, indexItem, indexedType, true, unique, indexName, optionalSerde, false);

        // fill table since its new
        EventBean[] events = new EventBean[1];
        for (EventBean prefilledEvent : prefilledEvents) {
            events[0] = prefilledEvent;
            table.add(events);
        }

        // add table
        tables.add(table);

        // add index, reference counted
        tableIndexesRefCount.put(indexPropKey, new EventTableIndexRepositoryEntry(indexName, table));

        return new Pair<IndexMultiKey, EventTableAndNamePair>(indexPropKey, new EventTableAndNamePair(table, indexName));
    }

    public String[] getExplicitIndexNames() {
        Set<String> names = explicitIndexes.keySet();
        return names.toArray(new String[names.size()]);
    }

    public void removeIndex(IndexMultiKey index) {
        EventTableIndexRepositoryEntry entry = tableIndexesRefCount.remove(index);
        if (entry != null) {
            tables.remove(entry.getTable());
            if (entry.getOptionalIndexName() != null) {
                explicitIndexes.remove(entry.getOptionalIndexName());
            }
            entry.getTable().destroy();
        }
    }

    public IndexMultiKey getIndexByName(String indexName) {
        for (Map.Entry<IndexMultiKey, EventTableIndexRepositoryEntry> entry : tableIndexesRefCount.entrySet()) {
            if (entry.getValue().getOptionalIndexName().equals(indexName)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void removeExplicitIndex(String indexName) {
        EventTable eventTable = explicitIndexes.remove(indexName);
        if (eventTable != null) {
            eventTable.destroy();
        }
    }
}
