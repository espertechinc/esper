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
package com.espertech.esper.epl.join.table;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.join.base.HistoricalIndexLookupStrategy;
import com.espertech.esper.epl.join.base.JoinSetComposerPrototypeFactory;
import com.espertech.esper.epl.join.plan.QueryGraph;
import com.espertech.esper.epl.join.plan.QueryGraphValue;
import com.espertech.esper.epl.join.plan.QueryGraphValueEntryHashKeyed;
import com.espertech.esper.epl.join.plan.QueryGraphValuePairHashKeyIndex;
import com.espertech.esper.epl.join.pollindex.PollResultIndexingStrategy;
import com.espertech.esper.util.JavaClassHelper;

import java.io.StringWriter;
import java.util.*;

/**
 * Manages index-building and sharing for historical streams by collecting required indexes during the
 * query planning phase, and by providing the right lookup strategy and indexing strategy during
 * query execution node creation.
 */
public class HistoricalStreamIndexList {
    private final int historicalStreamNum;
    private final EventType[] typesPerStream;
    private final QueryGraph queryGraph;
    private final TreeSet<Integer> pollingStreams;

    private Map<HistoricalStreamIndexDesc, List<Integer>> indexesUsedByStreams;
    private PollResultIndexingStrategy masterIndexingStrategy;

    /**
     * Ctor.
     *
     * @param historicalStreamNum number of the historical stream
     * @param typesPerStream      event types for each stream
     * @param queryGraph          relationship between key and index properties
     */
    public HistoricalStreamIndexList(int historicalStreamNum, EventType[] typesPerStream, QueryGraph queryGraph) {
        this.historicalStreamNum = historicalStreamNum;
        this.typesPerStream = typesPerStream;
        this.queryGraph = queryGraph;
        this.pollingStreams = new TreeSet<Integer>();
    }

    /**
     * Used during query plan phase to indicate that an index must be provided for use in lookup of historical events by using a
     * stream's events.
     *
     * @param streamViewStreamNum the stream providing lookup events
     */
    public void addIndex(int streamViewStreamNum) {
        pollingStreams.add(streamViewStreamNum);
    }

    /**
     * Get the strategies to use for polling from a given stream.
     *
     * @param streamViewStreamNum the stream providing the polling events
     * @return looking and indexing strategy
     */
    public Pair<HistoricalIndexLookupStrategy, PollResultIndexingStrategy> getStrategy(int streamViewStreamNum) {
        // If there is only a single polling stream, then build a single index
        if (pollingStreams.size() == 1) {
            return JoinSetComposerPrototypeFactory.determineIndexing(queryGraph, typesPerStream[historicalStreamNum], typesPerStream[streamViewStreamNum], historicalStreamNum, streamViewStreamNum);
        }

        // If there are multiple polling streams, determine if a single index is appropriate.
        // An index can be reused if:
        //  (a) indexed property names are the same
        //  (b) indexed property types are the same
        //  (c) key property types are the same (because of coercion)
        // A index lookup strategy is always specific to the providing stream.
        if (indexesUsedByStreams == null) {
            indexesUsedByStreams = new LinkedHashMap<HistoricalStreamIndexDesc, List<Integer>>();
            for (int pollingStream : pollingStreams) {
                QueryGraphValue queryGraphValue = queryGraph.getGraphValue(pollingStream, historicalStreamNum);
                QueryGraphValuePairHashKeyIndex hashKeyProps = queryGraphValue.getHashKeyProps();
                String[] indexProperties = hashKeyProps.getIndexed();

                Class[] keyTypes = getPropertyTypes(hashKeyProps.getKeys());
                Class[] indexTypes = getPropertyTypes(typesPerStream[historicalStreamNum], indexProperties);

                HistoricalStreamIndexDesc desc = new HistoricalStreamIndexDesc(indexProperties, indexTypes, keyTypes);
                List<Integer> usedByStreams = indexesUsedByStreams.get(desc);
                if (usedByStreams == null) {
                    usedByStreams = new LinkedList<Integer>();
                    indexesUsedByStreams.put(desc, usedByStreams);
                }
                usedByStreams.add(pollingStream);
            }

            // There are multiple indexes required:
            // Build a master indexing strategy that forms multiple indexes and numbers each.
            if (indexesUsedByStreams.size() > 1) {
                final int numIndexes = indexesUsedByStreams.size();
                final PollResultIndexingStrategy[] indexingStrategies = new PollResultIndexingStrategy[numIndexes];

                // create an indexing strategy for each index
                int count = 0;
                for (Map.Entry<HistoricalStreamIndexDesc, List<Integer>> desc : indexesUsedByStreams.entrySet()) {
                    int sampleStreamViewStreamNum = desc.getValue().get(0);
                    indexingStrategies[count] = JoinSetComposerPrototypeFactory.determineIndexing(queryGraph, typesPerStream[historicalStreamNum], typesPerStream[sampleStreamViewStreamNum], historicalStreamNum, sampleStreamViewStreamNum).getSecond();
                    count++;
                }

                // create a master indexing strategy that utilizes each indexing strategy to create a set of indexes
                final int streamNum = streamViewStreamNum;
                masterIndexingStrategy = new PollResultIndexingStrategy() {
                    public EventTable[] index(List<EventBean> pollResult, boolean isActiveCache, StatementContext statementContext) {
                        EventTable[] tables = new EventTable[numIndexes];
                        for (int i = 0; i < numIndexes; i++) {
                            tables[i] = indexingStrategies[i].index(pollResult, isActiveCache, statementContext)[0];
                        }

                        EventTableOrganization organization = new EventTableOrganization(null, false, false, streamNum, null, EventTableOrganizationType.MULTIINDEX);
                        return new EventTable[]{new MultiIndexEventTable(tables, organization)};
                    }

                    public String toQueryPlan() {
                        StringWriter writer = new StringWriter();
                        String delimiter = "";
                        for (PollResultIndexingStrategy strategy : indexingStrategies) {
                            writer.append(delimiter);
                            writer.append(strategy.toQueryPlan());
                            delimiter = ", ";
                        }
                        return this.getClass().getSimpleName() + " " + writer.toString();
                    }
                };
            }
        }

        // there is one type of index
        if (indexesUsedByStreams.size() == 1) {
            return JoinSetComposerPrototypeFactory.determineIndexing(queryGraph, typesPerStream[historicalStreamNum], typesPerStream[streamViewStreamNum], historicalStreamNum, streamViewStreamNum);
        }

        // determine which index number the polling stream must use
        int indexUsed = 0;
        boolean found = false;
        for (List<Integer> desc : indexesUsedByStreams.values()) {
            if (desc.contains(streamViewStreamNum)) {
                found = true;
                break;
            }
            indexUsed++;
        }
        if (!found) {
            throw new IllegalStateException("Index not found for use by stream " + streamViewStreamNum);
        }

        // Use one of the indexes built by the master index and a lookup strategy
        final int indexNumber = indexUsed;
        final HistoricalIndexLookupStrategy innerLookupStrategy = JoinSetComposerPrototypeFactory.determineIndexing(queryGraph, typesPerStream[historicalStreamNum], typesPerStream[streamViewStreamNum], historicalStreamNum, streamViewStreamNum).getFirst();

        HistoricalIndexLookupStrategy lookupStrategy = new HistoricalIndexLookupStrategy() {
            public Iterator<EventBean> lookup(EventBean lookupEvent, EventTable[] index, ExprEvaluatorContext context) {
                MultiIndexEventTable multiIndex = (MultiIndexEventTable) index[0];
                EventTable indexToUse = multiIndex.getTables()[indexNumber];
                return innerLookupStrategy.lookup(lookupEvent, new EventTable[]{indexToUse}, context);
            }

            public String toQueryPlan() {
                return this.getClass().getSimpleName() + " inner: " + innerLookupStrategy.toQueryPlan();
            }
        };

        return new Pair<HistoricalIndexLookupStrategy, PollResultIndexingStrategy>(lookupStrategy, masterIndexingStrategy);
    }

    private Class[] getPropertyTypes(EventType eventType, String[] properties) {
        Class[] types = new Class[properties.length];
        for (int i = 0; i < properties.length; i++) {
            types[i] = JavaClassHelper.getBoxedType(eventType.getPropertyType(properties[i]));
        }
        return types;
    }

    private Class[] getPropertyTypes(List<QueryGraphValueEntryHashKeyed> hashKeys) {
        Class[] types = new Class[hashKeys.size()];
        for (int i = 0; i < hashKeys.size(); i++) {
            types[i] = JavaClassHelper.getBoxedType(hashKeys.get(i).getKeyExpr().getForge().getEvaluationType());
        }
        return types;
    }


}
