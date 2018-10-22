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
package com.espertech.esper.common.internal.epl.join.queryplan;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenMakeable;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenMakeableUtil;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.util.UuidGenerator;

import java.util.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;

/**
 * Specifies an index to build as part of an overall query plan.
 */
public class QueryPlanIndexForge implements CodegenMakeable<SAIFFInitializeSymbol> {
    private Map<TableLookupIndexReqKey, QueryPlanIndexItemForge> items;

    public QueryPlanIndexForge(Map<TableLookupIndexReqKey, QueryPlanIndexItemForge> items) {
        if (items == null) {
            throw new IllegalArgumentException("Null value not allowed for items");
        }
        this.items = items;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        return newInstance(QueryPlanIndex.class, CodegenMakeableUtil.makeMap("items", TableLookupIndexReqKey.class, QueryPlanIndexItem.class, items, this.getClass(), parent, symbols, classScope));
    }

    public Map<TableLookupIndexReqKey, QueryPlanIndexItemForge> getItems() {
        return items;
    }

    public static QueryPlanIndexForge makeIndex(List<QueryPlanIndexItemForge> indexesSet) {
        Map<TableLookupIndexReqKey, QueryPlanIndexItemForge> items = new LinkedHashMap<TableLookupIndexReqKey, QueryPlanIndexItemForge>();
        for (QueryPlanIndexItemForge item : indexesSet) {
            items.put(new TableLookupIndexReqKey(UuidGenerator.generate(), null), item);
        }
        return new QueryPlanIndexForge(items);
    }

    /**
     * Find a matching index for the property names supplied.
     *
     * @param indexProps - property names to search for
     * @param rangeProps - range props
     * @return -1 if not found, or offset within indexes if found
     */
    public Pair<TableLookupIndexReqKey, int[]> getIndexNum(String[] indexProps, String[] rangeProps) {
        // find an exact match first
        QueryPlanIndexItemForge proposed = new QueryPlanIndexItemForge(indexProps, new Class[indexProps.length], rangeProps, new Class[rangeProps.length], false, null, null);
        for (Map.Entry<TableLookupIndexReqKey, QueryPlanIndexItemForge> entry : items.entrySet()) {
            if (entry.getValue().equalsCompareSortedProps(proposed)) {
                return new Pair<TableLookupIndexReqKey, int[]>(entry.getKey(), null);
            }
        }

        // find partial match second, i.e. for unique indexes where the where-clause is overspecific
        for (Map.Entry<TableLookupIndexReqKey, QueryPlanIndexItemForge> entry : items.entrySet()) {
            if (entry.getValue().getRangeProps() == null || entry.getValue().getRangeProps().length == 0) {
                int[] indexes = QueryPlanIndexUniqueHelper.checkSufficientGetAssignment(entry.getValue().getHashProps(), indexProps);
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

    public String addIndex(String[] indexProperties, Class[] coercionTypes, EventType eventType) {
        String uuid = UuidGenerator.generate();
        items.put(new TableLookupIndexReqKey(uuid, null), new QueryPlanIndexItemForge(indexProperties, coercionTypes, new String[0], new Class[0], false, null, eventType));
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
        for (Map.Entry<TableLookupIndexReqKey, QueryPlanIndexItemForge> entry : items.entrySet()) {
            arr[count] = entry.getValue().getHashProps();
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
        for (Map.Entry<TableLookupIndexReqKey, QueryPlanIndexItemForge> entry : items.entrySet()) {
            if (Arrays.deepEquals(entry.getValue().getHashProps(), indexProperties)) {
                return entry.getValue().getHashTypes();
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
        for (Map.Entry<TableLookupIndexReqKey, QueryPlanIndexItemForge> entry : items.entrySet()) {
            if (Arrays.deepEquals(entry.getValue().getHashProps(), indexProperties)) {
                entry.getValue().setHashTypes(coercionTypes);
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
        for (Map.Entry<TableLookupIndexReqKey, QueryPlanIndexItemForge> entry : items.entrySet()) {
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
    public static String print(QueryPlanIndexForge[] indexSpecs) {
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

    public static QueryPlanIndexForge makeIndexTableAccess(TableLookupIndexReqKey indexName) {
        Map<TableLookupIndexReqKey, QueryPlanIndexItemForge> indexMap = new HashMap<TableLookupIndexReqKey, QueryPlanIndexItemForge>();
        indexMap.put(indexName, null);
        return new QueryPlanIndexForge(indexMap);
    }
}
