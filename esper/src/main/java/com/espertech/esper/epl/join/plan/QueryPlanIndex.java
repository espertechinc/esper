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
package com.espertech.esper.epl.join.plan;

import com.espertech.esper.collection.Pair;
import com.espertech.esper.util.UuidGenerator;

import java.util.*;

/**
 * Specifies an index to build as part of an overall query plan.
 */
public class QueryPlanIndex {
    private Map<TableLookupIndexReqKey, QueryPlanIndexItem> items;

    public QueryPlanIndex(Map<TableLookupIndexReqKey, QueryPlanIndexItem> items) {
        if (items == null) {
            throw new IllegalArgumentException("Null value not allowed for items");
        }
        this.items = items;
    }

    public Map<TableLookupIndexReqKey, QueryPlanIndexItem> getItems() {
        return items;
    }

    public static QueryPlanIndex makeIndex(QueryPlanIndexItem... items) {
        Map<TableLookupIndexReqKey, QueryPlanIndexItem> result = new LinkedHashMap<TableLookupIndexReqKey, QueryPlanIndexItem>();
        for (QueryPlanIndexItem item : items) {
            result.put(new TableLookupIndexReqKey(UuidGenerator.generate()), item);
        }
        return new QueryPlanIndex(result);
    }

    public static QueryPlanIndex makeIndex(List<QueryPlanIndexItem> indexesSet) {
        Map<TableLookupIndexReqKey, QueryPlanIndexItem> items = new LinkedHashMap<TableLookupIndexReqKey, QueryPlanIndexItem>();
        for (QueryPlanIndexItem item : indexesSet) {
            items.put(new TableLookupIndexReqKey(UuidGenerator.generate()), item);
        }
        return new QueryPlanIndex(items);
    }

    /**
     * Find a matching index for the property names supplied.
     *
     * @param indexProps - property names to search for
     * @param rangeProps - range props
     * @return -1 if not found, or offset within indexes if found
     */
    protected Pair<TableLookupIndexReqKey, int[]> getIndexNum(String[] indexProps, String[] rangeProps) {
        // find an exact match first
        QueryPlanIndexItem proposed = new QueryPlanIndexItem(indexProps, null, rangeProps, null, false, null);
        for (Map.Entry<TableLookupIndexReqKey, QueryPlanIndexItem> entry : items.entrySet()) {
            if (entry.getValue().equalsCompareSortedProps(proposed)) {
                return new Pair<TableLookupIndexReqKey, int[]>(entry.getKey(), null);
            }
        }

        // find partial match second, i.e. for unique indexes where the where-clause is overspecific
        for (Map.Entry<TableLookupIndexReqKey, QueryPlanIndexItem> entry : items.entrySet()) {
            if (entry.getValue().getRangeProps() == null || entry.getValue().getRangeProps().length == 0) {
                int[] indexes = QueryPlanIndexUniqueHelper.checkSufficientGetAssignment(entry.getValue().getIndexProps(), indexProps);
                if (indexes != null && indexes.length != 0) {
                    return new Pair<TableLookupIndexReqKey, int[]>(entry.getKey(), indexes);
                }
            }
        }

        return null;
    }

    protected TableLookupIndexReqKey getFirstIndexNum() {
        return items.keySet().iterator().next();
    }

    /**
     * Add an index specification element.
     *
     * @param indexProperties - list of property names to index
     * @param coercionTypes   - list of coercion types if required, or null if no coercion required
     * @return number indicating position of index that was added
     */
    public String addIndex(String[] indexProperties, Class[] coercionTypes) {
        String uuid = UuidGenerator.generate();
        items.put(new TableLookupIndexReqKey(uuid), new QueryPlanIndexItem(indexProperties, coercionTypes, null, null, false, null));
        return uuid;
    }

    /**
     * For testing - Returns property names of all indexes.
     *
     * @return property names array
     */
    public String[][] getIndexProps() {
        String[][] arr = new String[items.size()][];
        int count = 0;
        for (Map.Entry<TableLookupIndexReqKey, QueryPlanIndexItem> entry : items.entrySet()) {
            arr[count] = entry.getValue().getIndexProps();
            count++;
        }
        return arr;
    }


    /**
     * Returns a list of coercion types for a given index.
     *
     * @param indexProperties is the index field names
     * @return coercion types, or null if no coercion is required
     */
    public Class[] getCoercionTypes(String[] indexProperties) {
        for (Map.Entry<TableLookupIndexReqKey, QueryPlanIndexItem> entry : items.entrySet()) {
            if (Arrays.deepEquals(entry.getValue().getIndexProps(), indexProperties)) {
                return entry.getValue().getOptIndexCoercionTypes();
            }
        }
        throw new IllegalArgumentException("Index properties not found");
    }

    /**
     * Sets the coercion types for a given index.
     *
     * @param indexProperties is the index property names
     * @param coercionTypes   is the coercion types
     */
    public void setCoercionTypes(String[] indexProperties, Class[] coercionTypes) {
        boolean found = false;
        for (Map.Entry<TableLookupIndexReqKey, QueryPlanIndexItem> entry : items.entrySet()) {
            if (Arrays.deepEquals(entry.getValue().getIndexProps(), indexProperties)) {
                entry.getValue().setOptIndexCoercionTypes(coercionTypes);
                found = true;
            }
        }
        if (!found) {
            throw new IllegalArgumentException("Index properties not found");
        }
    }

    public String toString() {
        if (items.isEmpty()) {
            return "    (none)";
        }
        StringBuilder buf = new StringBuilder();
        String delimiter = "";
        for (Map.Entry<TableLookupIndexReqKey, QueryPlanIndexItem> entry : items.entrySet()) {
            buf.append(delimiter);
            String info = entry.getValue() == null ? "" : " : " + entry.getValue();
            buf.append("    index " + entry.getKey() + info);
            delimiter = "\n";
        }
        return buf.toString();
    }

    /**
     * Print index specifications in readable format.
     *
     * @param indexSpecs - define indexes
     * @return readable format of index info
     */
    @SuppressWarnings({"StringConcatenationInsideStringBufferAppend"})
    public static String print(QueryPlanIndex[] indexSpecs) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("QueryPlanIndex[]\n");

        String delimiter = "";
        for (int i = 0; i < indexSpecs.length; i++) {
            buffer.append(delimiter);
            buffer.append("  index spec stream " + i + " : \n" + (indexSpecs[i] == null ? "    null" : indexSpecs[i]));
            delimiter = "\n";
        }

        return buffer.toString() + "\n";
    }

    public static QueryPlanIndex makeIndexTableAccess(TableLookupIndexReqKey indexName) {
        Map<TableLookupIndexReqKey, QueryPlanIndexItem> indexMap = new HashMap<TableLookupIndexReqKey, QueryPlanIndexItem>();
        indexMap.put(indexName, null);
        return new QueryPlanIndex(indexMap);
    }
}
