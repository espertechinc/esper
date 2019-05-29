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
package com.espertech.esper.common.internal.epl.historical.common;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.epl.historical.indexingstrategy.PollResultIndexingStrategyForge;
import com.espertech.esper.common.internal.epl.historical.indexingstrategy.PollResultIndexingStrategyMultiForge;
import com.espertech.esper.common.internal.epl.historical.lookupstrategy.HistoricalIndexLookupStrategyForge;
import com.espertech.esper.common.internal.epl.historical.lookupstrategy.HistoricalIndexLookupStrategyMultiForge;
import com.espertech.esper.common.internal.epl.join.base.JoinSetComposerPrototypeForgeFactory;
import com.espertech.esper.common.internal.epl.join.base.JoinSetComposerPrototypeHistoricalDesc;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphForge;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryHashKeyedForge;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueForge;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValuePairHashKeyIndexForge;
import com.espertech.esper.common.internal.serde.compiletime.resolve.SerdeCompileTimeResolver;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.*;

/**
 * Manages index-building and sharing for historical streams by collecting required indexes during the
 * query planning phase, and by providing the right lookup strategy and indexing strategy during
 * query execution node creation.
 */
public class HistoricalStreamIndexListForge {
    private final int historicalStreamNum;
    private final EventType[] typesPerStream;
    private final QueryGraphForge queryGraph;
    private final TreeSet<Integer> pollingStreams;

    private Map<HistoricalStreamIndexDesc, List<Integer>> indexesUsedByStreams;
    private PollResultIndexingStrategyForge masterIndexingStrategy;

    /**
     * Ctor.
     *
     * @param historicalStreamNum number of the historical stream
     * @param typesPerStream      event types for each stream
     * @param queryGraph          relationship between key and index properties
     */
    public HistoricalStreamIndexListForge(int historicalStreamNum, EventType[] typesPerStream, QueryGraphForge queryGraph) {
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
     * @param raw raw info
     * @param serdeResolver resolver
     * @return looking and indexing strategy
     */
    public JoinSetComposerPrototypeHistoricalDesc getStrategy(int streamViewStreamNum, StatementRawInfo raw, SerdeCompileTimeResolver serdeResolver) {
        // If there is only a single polling stream, then build a single index
        if (pollingStreams.size() == 1) {
            return JoinSetComposerPrototypeForgeFactory.determineIndexing(queryGraph, typesPerStream[historicalStreamNum], typesPerStream[streamViewStreamNum], historicalStreamNum, streamViewStreamNum, raw, serdeResolver);
        }

        // If there are multiple polling streams, determine if a single index is appropriate.
        // An index can be reused if:
        //  (a) indexed property names are the same
        //  (b) indexed property types are the same
        //  (c) key property types are the same (because of coercion)
        // A index lookup strategy is always specific to the providing stream.
        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>(2);
        if (indexesUsedByStreams == null) {
            indexesUsedByStreams = new LinkedHashMap<>();
            for (int pollingStream : pollingStreams) {
                QueryGraphValueForge queryGraphValue = queryGraph.getGraphValue(pollingStream, historicalStreamNum);
                QueryGraphValuePairHashKeyIndexForge hashKeyProps = queryGraphValue.getHashKeyProps();
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
                final PollResultIndexingStrategyForge[] indexingStrategies = new PollResultIndexingStrategyForge[numIndexes];

                // create an indexing strategy for each index
                int count = 0;
                for (Map.Entry<HistoricalStreamIndexDesc, List<Integer>> desc : indexesUsedByStreams.entrySet()) {
                    int sampleStreamViewStreamNum = desc.getValue().get(0);
                    JoinSetComposerPrototypeHistoricalDesc indexing = JoinSetComposerPrototypeForgeFactory.determineIndexing(queryGraph, typesPerStream[historicalStreamNum], typesPerStream[sampleStreamViewStreamNum], historicalStreamNum, sampleStreamViewStreamNum, raw, serdeResolver);
                    indexingStrategies[count] = indexing.getIndexingForge();
                    additionalForgeables.addAll(indexing.getAdditionalForgeables());
                    count++;
                }

                // create a master indexing strategy that utilizes each indexing strategy to create a set of indexes
                masterIndexingStrategy = new PollResultIndexingStrategyMultiForge(streamViewStreamNum, indexingStrategies);
            }
        }

        // there is one type of index
        if (indexesUsedByStreams.size() == 1) {
            return JoinSetComposerPrototypeForgeFactory.determineIndexing(queryGraph, typesPerStream[historicalStreamNum], typesPerStream[streamViewStreamNum], historicalStreamNum, streamViewStreamNum, raw, serdeResolver);
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
        JoinSetComposerPrototypeHistoricalDesc indexing = JoinSetComposerPrototypeForgeFactory.determineIndexing(queryGraph, typesPerStream[historicalStreamNum], typesPerStream[streamViewStreamNum], historicalStreamNum, streamViewStreamNum, raw, serdeResolver);
        HistoricalIndexLookupStrategyForge innerLookupStrategy = indexing.getLookupForge();
        HistoricalIndexLookupStrategyForge lookupStrategy = new HistoricalIndexLookupStrategyMultiForge(indexUsed, innerLookupStrategy);
        additionalForgeables.addAll(indexing.getAdditionalForgeables());
        return new JoinSetComposerPrototypeHistoricalDesc(lookupStrategy, masterIndexingStrategy, additionalForgeables);
    }

    private Class[] getPropertyTypes(EventType eventType, String[] properties) {
        Class[] types = new Class[properties.length];
        for (int i = 0; i < properties.length; i++) {
            types[i] = JavaClassHelper.getBoxedType(eventType.getPropertyType(properties[i]));
        }
        return types;
    }

    private Class[] getPropertyTypes(List<QueryGraphValueEntryHashKeyedForge> hashKeys) {
        Class[] types = new Class[hashKeys.size()];
        for (int i = 0; i < hashKeys.size(); i++) {
            types[i] = JavaClassHelper.getBoxedType(hashKeys.get(i).getKeyExpr().getForge().getEvaluationType());
        }
        return types;
    }
}
