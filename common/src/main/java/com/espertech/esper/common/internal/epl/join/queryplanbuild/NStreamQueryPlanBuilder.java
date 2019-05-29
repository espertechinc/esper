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
package com.espertech.esper.common.internal.epl.join.queryplanbuild;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.NumberSetPermutationEnumeration;
import com.espertech.esper.common.internal.collection.NumberSetShiftGroupEnumeration;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlan;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlanner;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.context.aifactory.select.StreamJoinAnalysisResultCompileTime;
import com.espertech.esper.common.internal.epl.expression.core.ExprIdentNode;
import com.espertech.esper.common.internal.epl.historical.common.HistoricalStreamIndexListForge;
import com.espertech.esper.common.internal.epl.historical.common.HistoricalViewableDesc;
import com.espertech.esper.common.internal.epl.join.base.JoinSetComposerPrototypeHistoricalDesc;
import com.espertech.esper.common.internal.epl.join.indexlookupplan.*;
import com.espertech.esper.common.internal.epl.join.lookup.IndexMultiKey;
import com.espertech.esper.common.internal.epl.join.lookup.IndexedPropDesc;
import com.espertech.esper.common.internal.epl.join.querygraph.*;
import com.espertech.esper.common.internal.epl.join.queryplan.*;
import com.espertech.esper.common.internal.epl.lookupplan.SubordPropHashKeyForge;
import com.espertech.esper.common.internal.epl.lookupplan.SubordPropRangeKeyForge;
import com.espertech.esper.common.internal.epl.lookupplansubord.EventTableIndexEntryBase;
import com.espertech.esper.common.internal.epl.lookupplansubord.EventTableIndexUtil;
import com.espertech.esper.common.internal.epl.lookupplansubord.IndexKeyInfo;
import com.espertech.esper.common.internal.epl.lookupplansubord.SubordinateQueryPlannerUtil;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.serde.compiletime.resolve.SerdeCompileTimeResolver;
import com.espertech.esper.common.internal.util.DependencyGraph;
import com.espertech.esper.common.internal.util.JavaClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 2 Stream query strategy/execution tree
 * (stream 0)         Lookup in stream 1
 * (stream 1)         Lookup in stream 0
 * <p>
 * ------ Example 1   a 3 table join
 * <p>
 * " where streamA.id = streamB.id " +
 * "   and streamB.id = streamC.id";
 * <p>
 * => Index propery names for each stream
 * for stream 0 to 4 = "id"
 * <p>
 * => join order, ie.
 * for stream 0 = {1, 2}
 * for stream 1 = {factor [0,2]}
 * for stream 2 = {1, 0}
 * <p>
 * => IndexKeyGen optionalIndexKeyGen, created by nested query plan nodes
 * <p>
 * <p>
 * 3 Stream query strategy
 * (stream 0)          Nested iteration
 * Lookup in stream 1        Lookup in stream 2
 * <p>
 * (stream 1)         Factor
 * Lookup in stream 0        Lookup in stream 2
 * <p>
 * (stream 2)         Nested iteration
 * Lookup in stream 1        Lookup in stream 0
 * <p>
 * <p>
 * ------ Example 2  a 4 table join
 * <p>
 * " where streamA.id = streamB.id " +
 * "   and streamB.id = streamC.id";
 * "   and streamC.id = streamD.id";
 * <p>
 * => join order, ie.
 * for stream 0 = {1, 2, 3}
 * for stream 1 = {factor [0,2], use 2 for 3}
 * for stream 2 = {factor [1,3], use 1 for 0}
 * for stream 3 = {2, 1, 0}
 * <p>
 * <p>
 * concepts... nested iteration, inner loop
 * <p>
 * select * from s1, s2, s3, s4 where s1.id=s2.id and s2.id=s3.id and s3.id=s4.id
 * <p>
 * <p>
 * (stream 0)              Nested iteration
 * Lookup in stream 1        Lookup in stream 2        Lookup in stream 3
 * <p>
 * (stream 1)              Factor
 * lookup in stream 0                 Nested iteration
 * Lookup in stream 2        Lookup in stream 3
 * <p>
 * (stream 2)              Factor
 * lookup in stream 3                 Nested iteration
 * Lookup in stream 1        Lookup in stream 0
 * <p>
 * (stream 3)              Nested iteration
 * Lookup in stream 2        Lookup in stream 1        Lookup in stream 0
 * <p>
 * ------ Example 4  a 4 table join, orphan table
 * <p>
 * " where streamA.id = streamB.id " +
 * "   and streamB.id = streamC.id"; (no table D join criteria)
 * <p>
 * ------ Example 5  a 3 table join with 2 indexes for stream B
 * <p>
 * " where streamA.A1 = streamB.B1 " +
 * "   and streamB.B2 = streamC.C1"; (no table D join criteria)
 */

/**
 * Builds a query plan for 3 or more streams in a join.
 */
public class NStreamQueryPlanBuilder {
    protected static QueryPlanForgeDesc build(QueryGraphForge queryGraph,
                                              EventType[] typesPerStream,
                                              HistoricalViewableDesc historicalViewableDesc,
                                              DependencyGraph dependencyGraph,
                                              HistoricalStreamIndexListForge[] historicalStreamIndexLists,
                                              boolean hasForceNestedIter,
                                              String[][][] indexedStreamsUniqueProps,
                                              TableMetaData[] tablesPerStream,
                                              StreamJoinAnalysisResultCompileTime streamJoinAnalysisResult,
                                              StatementRawInfo raw,
                                              SerdeCompileTimeResolver serdeResolver) {
        if (log.isDebugEnabled()) {
            log.debug(".build filterQueryGraph=" + queryGraph);
        }

        int numStreams = queryGraph.getNumStreams();
        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>(2);

        QueryPlanIndexForge[] indexSpecs = QueryPlanIndexBuilder.buildIndexSpec(queryGraph, typesPerStream, indexedStreamsUniqueProps);
        if (log.isDebugEnabled()) {
            log.debug(".build Index build completed, indexes=" + QueryPlanIndexForge.print(indexSpecs));
        }

        // any historical streams don't get indexes, the lookup strategy accounts for cached indexes
        if (historicalViewableDesc.isHasHistorical()) {
            for (int i = 0; i < historicalViewableDesc.getHistorical().length; i++) {
                if (historicalViewableDesc.getHistorical()[i]) {
                    indexSpecs[i] = null;
                }
            }
        }

        QueryPlanNodeForge[] planNodeSpecs = new QueryPlanNodeForge[numStreams];
        int worstDepth = Integer.MAX_VALUE;
        for (int streamNo = 0; streamNo < numStreams; streamNo++) {
            // no plan for historical streams that are dependent upon other streams
            if ((historicalViewableDesc.getHistorical()[streamNo]) && (dependencyGraph.hasDependency(streamNo))) {
                planNodeSpecs[streamNo] = new QueryPlanNodeNoOpForge();
                continue;
            }

            BestChainResult bestChainResult = computeBestPath(streamNo, queryGraph, dependencyGraph);
            int[] bestChain = bestChainResult.getChain();
            if (log.isDebugEnabled()) {
                log.debug(".build For stream " + streamNo + " bestChain=" + Arrays.toString(bestChain));
            }

            if (bestChainResult.depth < worstDepth) {
                worstDepth = bestChainResult.depth;
            }

            QueryPlanNodeForgeDesc planDesc = createStreamPlan(streamNo, bestChain, queryGraph, indexSpecs, typesPerStream, historicalViewableDesc.getHistorical(), historicalStreamIndexLists, tablesPerStream, streamJoinAnalysisResult, raw, serdeResolver);
            planNodeSpecs[streamNo] = planDesc.getForge();
            additionalForgeables.addAll(planDesc.getAdditionalForgeables());
            if (log.isDebugEnabled()) {
                log.debug(".build spec=" + planNodeSpecs[streamNo]);
            }
        }

        // We use the merge/nested (outer) join algorithm instead.
        if ((worstDepth < numStreams - 1) && (!hasForceNestedIter)) {
            return null;
        }

        // build historical index and lookup strategies
        for (int i = 0; i < numStreams; i++) {
            QueryPlanNodeForge plan = planNodeSpecs[i];
            QueryPlanNodeForgeVisitor visitor = new QueryPlanNodeForgeVisitor() {
                public void visit(QueryPlanNodeForge node) {
                    if (node instanceof HistoricalDataPlanNodeForge) {
                        HistoricalDataPlanNodeForge historical = (HistoricalDataPlanNodeForge) node;
                        JoinSetComposerPrototypeHistoricalDesc desc = historicalStreamIndexLists[historical.getStreamNum()].getStrategy(historical.getLookupStreamNum(), raw, serdeResolver);
                        historical.setPollResultIndexingStrategy(desc.getIndexingForge());
                        historical.setHistoricalIndexLookupStrategy(desc.getLookupForge());
                        additionalForgeables.addAll(desc.getAdditionalForgeables());
                    }
                }
            };
            plan.accept(visitor);
        }

        QueryPlanForge forge = new QueryPlanForge(indexSpecs, planNodeSpecs);
        return new QueryPlanForgeDesc(forge, additionalForgeables);
    }

    /**
     * Walks the chain of lookups and constructs lookup strategy and plan specification based
     * on the index specifications.
     *
     * @param lookupStream               - the stream to construct the query plan for
     * @param bestChain                  - the chain that the lookup follows to make best use of indexes
     * @param queryGraph                 - the repository for key properties to indexes
     * @param indexSpecsPerStream        - specifications of indexes
     * @param typesPerStream             - event types for each stream
     * @param isHistorical               - indicator for each stream if it is a historical streams or not
     * @param historicalStreamIndexLists - index management, populated for the query plan
     * @param tablesPerStream            tables
     * @param streamJoinAnalysisResult   stream join analysis
     * @param raw raw info
     * @param serdeResolver serde resolver
     * @return NestedIterationNode with lookups attached underneath
     */
    protected static QueryPlanNodeForgeDesc createStreamPlan(int lookupStream, int[] bestChain, QueryGraphForge queryGraph,
                                                         QueryPlanIndexForge[] indexSpecsPerStream, EventType[] typesPerStream,
                                                         boolean[] isHistorical, HistoricalStreamIndexListForge[] historicalStreamIndexLists,
                                                         TableMetaData[] tablesPerStream,
                                                         StreamJoinAnalysisResultCompileTime streamJoinAnalysisResult,
                                                             StatementRawInfo raw,
                                                             SerdeCompileTimeResolver serdeResolver) {
        NestedIterationNodeForge nestedIterNode = new NestedIterationNodeForge(bestChain);
        int currentLookupStream = lookupStream;
        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>(2);

        // Walk through each successive lookup
        for (int i = 0; i < bestChain.length; i++) {
            int indexedStream = bestChain[i];

            QueryPlanNodeForge node;
            if (isHistorical[indexedStream]) {
                if (historicalStreamIndexLists[indexedStream] == null) {
                    historicalStreamIndexLists[indexedStream] = new HistoricalStreamIndexListForge(indexedStream, typesPerStream, queryGraph);
                }
                historicalStreamIndexLists[indexedStream].addIndex(currentLookupStream);
                node = new HistoricalDataPlanNodeForge(indexedStream, lookupStream, currentLookupStream, typesPerStream.length, null);
            } else {
                TableLookupPlanDesc tableLookupPlan = createLookupPlan(queryGraph, currentLookupStream, indexedStream, streamJoinAnalysisResult.isVirtualDW(indexedStream), indexSpecsPerStream[indexedStream], typesPerStream, tablesPerStream[indexedStream], raw, serdeResolver);
                node = new TableLookupNodeForge(tableLookupPlan.getForge());
                additionalForgeables.addAll(tableLookupPlan.getAdditionalForgeables());
            }
            nestedIterNode.addChildNode(node);

            currentLookupStream = bestChain[i];
        }

        return new QueryPlanNodeForgeDesc(nestedIterNode, additionalForgeables);
    }

    /**
     * Create the table lookup plan for a from-stream to look up in an indexed stream
     * using the columns supplied in the query graph and looking at the actual indexes available
     * and their index number.
     *
     * @param queryGraph             - contains properties joining the 2 streams
     * @param currentLookupStream    - stream to use key values from
     * @param indexedStream          - stream to look up in
     * @param indexSpecs             - index specification defining indexes to be created for stream
     * @param typesPerStream         - event types for each stream
     * @param indexedStreamTableMeta table info
     * @param indexedStreamIsVDW     vdw indicators
     * @param raw raw info
     * @param serdeResolver serde resolver
     * @return plan for performing a lookup in a given table using one of the indexes supplied
     */
    public static TableLookupPlanDesc createLookupPlan(QueryGraphForge queryGraph, int currentLookupStream, int indexedStream,
                                                        boolean indexedStreamIsVDW,
                                                        QueryPlanIndexForge indexSpecs,
                                                        EventType[] typesPerStream,
                                                        TableMetaData indexedStreamTableMeta,
                                                       StatementRawInfo raw,
                                                       SerdeCompileTimeResolver serdeResolver) {
        QueryGraphValueForge queryGraphValue = queryGraph.getGraphValue(currentLookupStream, indexedStream);
        QueryGraphValuePairHashKeyIndexForge hashKeyProps = queryGraphValue.getHashKeyProps();
        List<QueryGraphValueEntryHashKeyedForge> hashPropsKeys = hashKeyProps.getKeys();
        String[] hashIndexProps = hashKeyProps.getIndexed();

        QueryGraphValuePairRangeIndexForge rangeProps = queryGraphValue.getRangeProps();
        List<QueryGraphValueEntryRangeForge> rangePropsKeys = rangeProps.getKeys();
        String[] rangeIndexProps = rangeProps.getIndexed();

        Pair<TableLookupIndexReqKey, int[]> pairIndexHashRewrite = indexSpecs.getIndexNum(hashIndexProps, rangeIndexProps);
        TableLookupIndexReqKey indexNum = pairIndexHashRewrite == null ? null : pairIndexHashRewrite.getFirst();

        // handle index redirection towards unique index
        if (pairIndexHashRewrite != null && pairIndexHashRewrite.getSecond() != null) {
            int[] indexes = pairIndexHashRewrite.getSecond();
            String[] newHashIndexProps = new String[indexes.length];
            List<QueryGraphValueEntryHashKeyedForge> newHashKeys = new ArrayList<QueryGraphValueEntryHashKeyedForge>();
            for (int i = 0; i < indexes.length; i++) {
                newHashIndexProps[i] = hashIndexProps[indexes[i]];
                newHashKeys.add(hashPropsKeys.get(indexes[i]));
            }
            hashIndexProps = newHashIndexProps;
            hashPropsKeys = newHashKeys;
            rangeIndexProps = new String[0];
            rangePropsKeys = Collections.emptyList();
        }

        // no direct hash or range lookups
        if (hashIndexProps.length == 0 && rangeIndexProps.length == 0) {

            // handle single-direction 'in' keyword
            QueryGraphValuePairInKWSingleIdxForge singles = queryGraphValue.getInKeywordSingles();
            if (!singles.getKey().isEmpty()) {

                QueryGraphValueEntryInKeywordSingleIdxForge single = null;
                indexNum = null;
                if (indexedStreamTableMeta != null) {
                    String[] indexes = singles.getIndexed();
                    int count = 0;
                    for (String index : indexes) {
                        Pair<IndexMultiKey, EventTableIndexEntryBase> indexPairFound = EventTableIndexUtil.findIndexBestAvailable(indexedStreamTableMeta.getIndexMetadata().getIndexes(), Collections.singleton(index), Collections.<String>emptySet(), null);
                        if (indexPairFound != null) {
                            indexNum = new TableLookupIndexReqKey(indexPairFound.getSecond().getOptionalIndexName(), indexPairFound.getSecond().getOptionalIndexModuleName(), indexedStreamTableMeta.getTableName());
                            single = singles.getKey().get(count);
                        }
                        count++;
                    }
                } else {
                    single = singles.getKey().get(0);
                    Pair<TableLookupIndexReqKey, int[]> pairIndex = indexSpecs.getIndexNum(new String[]{singles.getIndexed()[0]}, new String[0]);
                    indexNum = pairIndex.getFirst();
                }

                if (indexNum != null) {
                    InKeywordTableLookupPlanSingleIdxForge forge = new InKeywordTableLookupPlanSingleIdxForge(currentLookupStream, indexedStream, indexedStreamIsVDW, typesPerStream, indexNum, single.getKeyExprs());
                    return new TableLookupPlanDesc(forge, Collections.emptyList());
                }
            }

            // handle multi-direction 'in' keyword
            List<QueryGraphValuePairInKWMultiIdx> multis = queryGraphValue.getInKeywordMulti();
            if (!multis.isEmpty()) {
                if (indexedStreamTableMeta != null) {
                    return getFullTableScanTable(currentLookupStream, indexedStream, indexedStreamIsVDW, typesPerStream, indexedStreamTableMeta);
                }
                QueryGraphValuePairInKWMultiIdx multi = multis.get(0);
                TableLookupIndexReqKey[] indexNameArray = new TableLookupIndexReqKey[multi.getIndexed().length];
                boolean foundAll = true;
                for (int i = 0; i < multi.getIndexed().length; i++) {
                    ExprIdentNode identNode = (ExprIdentNode) multi.getIndexed()[i];
                    Pair<TableLookupIndexReqKey, int[]> pairIndex = indexSpecs.getIndexNum(new String[]{identNode.getResolvedPropertyName()}, new String[0]);
                    if (pairIndex == null) {
                        foundAll = false;
                    } else {
                        indexNameArray[i] = pairIndex.getFirst();
                    }
                }
                if (foundAll) {
                    InKeywordTableLookupPlanMultiIdxForge forge = new InKeywordTableLookupPlanMultiIdxForge(currentLookupStream, indexedStream, indexedStreamIsVDW, typesPerStream, indexNameArray, multi.getKey().getKeyExpr());
                    return new TableLookupPlanDesc(forge, Collections.emptyList());
                }
            }

            // We don't use a keyed index but use the full stream set as the stream does not have any indexes

            // If no such full set index exists yet, add to specs
            if (indexedStreamTableMeta != null) {
                return getFullTableScanTable(currentLookupStream, indexedStream, indexedStreamIsVDW, typesPerStream, indexedStreamTableMeta);
            }
            if (indexNum == null) {
                indexNum = new TableLookupIndexReqKey(indexSpecs.addIndex(new String[0], new Class[0], typesPerStream[indexedStream]), null);
            }
            FullTableScanLookupPlanForge forge = new FullTableScanLookupPlanForge(currentLookupStream, indexedStream, indexedStreamIsVDW, typesPerStream, indexNum);
            return new TableLookupPlanDesc(forge, Collections.emptyList());
        }

        if (indexNum == null) {
            throw new IllegalStateException("Failed to query plan as index for " + Arrays.toString(hashIndexProps) + " and " + Arrays.toString(rangeIndexProps) + " in the index specification");
        }

        if (indexedStreamTableMeta != null) {
            Pair<IndexMultiKey, EventTableIndexEntryBase> indexPairFound = EventTableIndexUtil.findIndexBestAvailable(indexedStreamTableMeta.getIndexMetadata().getIndexes(), toSet(hashIndexProps), toSet(rangeIndexProps), null);
            if (indexPairFound != null) {
                IndexKeyInfo indexKeyInfo = SubordinateQueryPlannerUtil.compileIndexKeyInfo(indexPairFound.getFirst(), hashIndexProps, getHashKeyFuncsAsSubProp(hashPropsKeys), rangeIndexProps, getRangeFuncsAsSubProp(rangePropsKeys));
                if (indexKeyInfo.getOrderedKeyCoercionTypes().isCoerce() || indexKeyInfo.getOrderedRangeCoercionTypes().isCoerce()) {
                    return getFullTableScanTable(currentLookupStream, indexedStream, indexedStreamIsVDW, typesPerStream, indexedStreamTableMeta);
                }
                hashPropsKeys = toHashKeyFuncs(indexKeyInfo.getOrderedHashDesc());
                hashIndexProps = IndexedPropDesc.getIndexProperties(indexPairFound.getFirst().getHashIndexedProps());
                rangePropsKeys = toRangeKeyFuncs(indexKeyInfo.getOrderedRangeDesc());
                rangeIndexProps = IndexedPropDesc.getIndexProperties(indexPairFound.getFirst().getRangeIndexedProps());
                indexNum = new TableLookupIndexReqKey(indexPairFound.getSecond().getOptionalIndexName(), indexPairFound.getSecond().getOptionalIndexModuleName(), indexedStreamTableMeta.getTableName());
                // the plan will be created below
                if (hashIndexProps.length == 0 && rangeIndexProps.length == 0) {
                    return getFullTableScanTable(currentLookupStream, indexedStream, indexedStreamIsVDW, typesPerStream, indexedStreamTableMeta);
                }
            } else {
                return getFullTableScanTable(currentLookupStream, indexedStream, indexedStreamIsVDW, typesPerStream, indexedStreamTableMeta);
            }
        }

        // straight keyed-index lookup
        if (hashIndexProps.length > 0 && rangeIndexProps.length == 0) {
            // Determine coercion required
            CoercionDesc coercionTypes = CoercionUtil.getCoercionTypesHash(typesPerStream, currentLookupStream, indexedStream, hashPropsKeys, hashIndexProps);
            if (coercionTypes.isCoerce()) {
                // check if there already are coercion types for this index
                Class[] existCoercionTypes = indexSpecs.getCoercionTypes(hashIndexProps);
                if (existCoercionTypes != null) {
                    for (int i = 0; i < existCoercionTypes.length; i++) {
                        coercionTypes.getCoercionTypes()[i] = JavaClassHelper.getCompareToCoercionType(existCoercionTypes[i], coercionTypes.getCoercionTypes()[i]);
                    }
                }
                if (!indexSpecs.getItems().isEmpty()) {
                    indexSpecs.setCoercionTypes(hashIndexProps, coercionTypes.getCoercionTypes());
                }
            }
            Class[] coercionTypesArray = coercionTypes.getCoercionTypes();
            MultiKeyClassRef tableLookupMultiKey = null;
            List<StmtClassForgeableFactory> additionalForgeables = Collections.emptyList();
            if (indexNum.getTableName() != null) {
                MultiKeyPlan tableMultiKeyPlan = MultiKeyPlanner.planMultiKey(coercionTypesArray, true, raw, serdeResolver);
                tableLookupMultiKey = tableMultiKeyPlan.getClassRef();
                additionalForgeables = tableMultiKeyPlan.getMultiKeyForgeables();
            }
            IndexedTableLookupPlanHashedOnlyForge forge = new IndexedTableLookupPlanHashedOnlyForge(currentLookupStream, indexedStream, indexedStreamIsVDW, typesPerStream, indexNum, hashPropsKeys.toArray(new QueryGraphValueEntryHashKeyedForge[hashPropsKeys.size()]), indexSpecs, coercionTypesArray, tableLookupMultiKey);
            return new TableLookupPlanDesc(forge, additionalForgeables);
        }

        // sorted index lookup
        CoercionDesc coercionTypesRange = CoercionUtil.getCoercionTypesRange(typesPerStream, indexedStream, rangeIndexProps, rangePropsKeys);
        CoercionDesc coercionTypesHash = CoercionUtil.getCoercionTypesHash(typesPerStream, currentLookupStream, indexedStream, hashPropsKeys, hashIndexProps);
        if (hashIndexProps.length == 0 && rangeIndexProps.length == 1) {
            QueryGraphValueEntryRangeForge range = rangePropsKeys.get(0);
            Class coercionType = null;
            if (coercionTypesRange.isCoerce()) {
                coercionType = coercionTypesRange.getCoercionTypes()[0];
            }
            SortedTableLookupPlanForge forge = new SortedTableLookupPlanForge(currentLookupStream, indexedStream, indexedStreamIsVDW, typesPerStream, indexNum, range, coercionType);
            return new TableLookupPlanDesc(forge, Collections.emptyList());
        } else {
            MultiKeyClassRef tableLookupMultiKey = null;
            List<StmtClassForgeableFactory> additionalForgeables = Collections.emptyList();
            if (indexNum.getTableName() != null) {
                MultiKeyPlan tableMultiKeyPlan = MultiKeyPlanner.planMultiKey(coercionTypesHash.getCoercionTypes(), true, raw, serdeResolver);
                tableLookupMultiKey = tableMultiKeyPlan.getClassRef();
                additionalForgeables = tableMultiKeyPlan.getMultiKeyForgeables();
            }

            // composite range and index lookup
            CompositeTableLookupPlanForge forge = new CompositeTableLookupPlanForge(currentLookupStream, indexedStream, indexedStreamIsVDW, typesPerStream, indexNum, hashPropsKeys, coercionTypesHash.getCoercionTypes(), rangePropsKeys, coercionTypesRange.getCoercionTypes(), indexSpecs, tableLookupMultiKey);
            return new TableLookupPlanDesc(forge, additionalForgeables);
        }
    }

    /**
     * Compute a best chain or path for lookups to take for the lookup stream passed in and the query
     * property relationships.
     * The method runs through all possible permutations of lookup path {@link NumberSetPermutationEnumeration}
     * until a path is found in which all streams can be accessed via an index.
     * If not such path is found, the method returns the path with the greatest depth, ie. where
     * the first one or more streams are index accesses.
     * If no depth other then zero is found, returns the default nesting order.
     *
     * @param lookupStream    - stream to start look up
     * @param queryGraph      - navigability between streams
     * @param dependencyGraph - dependencies between historical streams
     * @return chain and chain depth
     */
    protected static BestChainResult computeBestPath(int lookupStream, QueryGraphForge queryGraph, DependencyGraph dependencyGraph) {
        int[] defNestingorder = buildDefaultNestingOrder(queryGraph.getNumStreams(), lookupStream);
        Enumeration<int[]> streamEnum;
        if (defNestingorder.length < 6) {
            streamEnum = new NumberSetPermutationEnumeration(defNestingorder);
        } else {
            streamEnum = new NumberSetShiftGroupEnumeration(defNestingorder);
        }
        int[] bestPermutation = null;
        int bestDepth = -1;

        while (streamEnum.hasMoreElements()) {
            int[] permutation = streamEnum.nextElement();

            // Only if the permutation satisfies all dependencies is the permutation considered
            if (dependencyGraph != null) {
                boolean pass = isDependencySatisfied(lookupStream, permutation, dependencyGraph);
                if (!pass) {
                    continue;
                }
            }

            int permutationDepth = computeNavigableDepth(lookupStream, permutation, queryGraph);

            if (permutationDepth > bestDepth) {
                bestPermutation = permutation;
                bestDepth = permutationDepth;
            }

            // Stop when the permutation yielding the full depth (lenght of stream chain) was hit
            if (permutationDepth == queryGraph.getNumStreams() - 1) {
                break;
            }
        }

        return new BestChainResult(bestDepth, bestPermutation);
    }

    /**
     * Determine if the proposed permutation of lookups passes dependencies
     *
     * @param lookupStream    stream to initiate
     * @param permutation     permutation of lookups
     * @param dependencyGraph dependencies
     * @return pass or fail indication
     */
    protected static boolean isDependencySatisfied(int lookupStream, int[] permutation, DependencyGraph dependencyGraph) {
        for (Map.Entry<Integer, SortedSet<Integer>> entry : dependencyGraph.getDependencies().entrySet()) {
            int target = entry.getKey();
            int positionTarget = positionOf(target, lookupStream, permutation);
            if (positionTarget == -1) {
                throw new IllegalArgumentException("Target dependency not found in permutation for target " + target + " and permutation " + Arrays.toString(permutation) + " and lookup stream " + lookupStream);
            }

            // check the position of each dependency, it must be higher
            for (int dependency : entry.getValue()) {
                int positonDep = positionOf(dependency, lookupStream, permutation);
                if (positonDep == -1) {
                    throw new IllegalArgumentException("Dependency not found in permutation for dependency " + dependency + " and permutation " + Arrays.toString(permutation) + " and lookup stream " + lookupStream);
                }

                if (positonDep > positionTarget) {
                    return false;
                }
            }
        }
        return true;
    }

    private static int positionOf(int stream, int lookupStream, int[] permutation) {
        if (stream == lookupStream) {
            return 0;
        }
        for (int i = 0; i < permutation.length; i++) {
            if (permutation[i] == stream) {
                return i + 1;
            }
        }
        return -1;
    }

    /**
     * Given a chain of streams to look up and indexing information, compute the index within the
     * chain of the first non-index lookup.
     *
     * @param lookupStream - stream to start lookup for
     * @param nextStreams  - list of stream numbers next in lookup
     * @param queryGraph   - indexing information
     * @return value between 0 and (nextStreams.lenght - 1)
     */
    protected static int computeNavigableDepth(int lookupStream, int[] nextStreams, QueryGraphForge queryGraph) {
        int currentStream = lookupStream;
        int currentDepth = 0;

        for (int i = 0; i < nextStreams.length; i++) {
            int nextStream = nextStreams[i];
            boolean navigable = queryGraph.isNavigableAtAll(currentStream, nextStream);
            if (!navigable) {
                break;
            }
            currentStream = nextStream;
            currentDepth++;
        }

        return currentDepth;
    }

    /**
     * Returns default nesting order for a given number of streams for a certain stream.
     * Example: numStreams = 5, forStream = 2, result = {0, 1, 3, 4}
     * The resulting array has all streams except the forStream, in ascdending order.
     *
     * @param numStreams - number of streams
     * @param forStream  - stream to generate a nesting order for
     * @return int array with all stream numbers starting at 0 to (numStreams - 1) leaving the
     * forStream out
     */
    protected static int[] buildDefaultNestingOrder(int numStreams, int forStream) {
        int[] nestingOrder = new int[numStreams - 1];

        int count = 0;
        for (int i = 0; i < numStreams; i++) {
            if (i == forStream) {
                continue;
            }
            nestingOrder[count++] = i;
        }

        return nestingOrder;
    }

    private static List<QueryGraphValueEntryRangeForge> toRangeKeyFuncs(List<SubordPropRangeKeyForge> orderedRangeDesc) {
        List<QueryGraphValueEntryRangeForge> result = new ArrayList<>();
        for (SubordPropRangeKeyForge key : orderedRangeDesc) {
            result.add(key.getRangeInfo());
        }
        return result;
    }

    private static List<QueryGraphValueEntryHashKeyedForge> toHashKeyFuncs(List<SubordPropHashKeyForge> orderedHashProperties) {
        List<QueryGraphValueEntryHashKeyedForge> result = new ArrayList<>();
        for (SubordPropHashKeyForge key : orderedHashProperties) {
            result.add(key.getHashKey());
        }
        return result;
    }

    /**
     * Encapsulates the chain information.
     */
    public static class BestChainResult {
        private int depth;
        private int[] chain;

        /**
         * Ctor.
         *
         * @param depth - depth this chain resolves into a indexed lookup
         * @param chain - chain for nested lookup
         */
        public BestChainResult(int depth, int[] chain) {
            this.depth = depth;
            this.chain = chain;
        }

        /**
         * Returns depth of lookups via index in chain.
         *
         * @return depth
         */
        public int getDepth() {
            return depth;
        }

        /**
         * Returns chain of stream numbers.
         *
         * @return array of stream numbers
         */
        public int[] getChain() {
            return chain;
        }

        public String toString() {
            return "depth=" + depth + " chain=" + Arrays.toString(chain);
        }
    }

    private static TableLookupPlanDesc getFullTableScanTable(int lookupStream, int indexedStream, boolean indexedStreamIsVDW, EventType[] typesPerStream, TableMetaData indexedStreamTableMeta) {
        TableLookupIndexReqKey indexName = new TableLookupIndexReqKey(indexedStreamTableMeta.getTableName(), indexedStreamTableMeta.getTableModuleName(), indexedStreamTableMeta.getTableName());
        FullTableScanUniquePerKeyLookupPlanForge forge = new FullTableScanUniquePerKeyLookupPlanForge(lookupStream, indexedStream, indexedStreamIsVDW, typesPerStream, indexName);
        return new TableLookupPlanDesc(forge, Collections.emptyList());
    }

    private static SubordPropRangeKeyForge[] getRangeFuncsAsSubProp(List<QueryGraphValueEntryRangeForge> funcs) {
        SubordPropRangeKeyForge[] keys = new SubordPropRangeKeyForge[funcs.size()];
        for (int i = 0; i < funcs.size(); i++) {
            QueryGraphValueEntryRangeForge func = funcs.get(i);
            keys[i] = new SubordPropRangeKeyForge(func, func.getExpressions()[0].getForge().getEvaluationType());
        }
        return keys;
    }

    private static SubordPropHashKeyForge[] getHashKeyFuncsAsSubProp(List<QueryGraphValueEntryHashKeyedForge> funcs) {
        SubordPropHashKeyForge[] keys = new SubordPropHashKeyForge[funcs.size()];
        for (int i = 0; i < funcs.size(); i++) {
            keys[i] = new SubordPropHashKeyForge(funcs.get(i), null, null);
        }
        return keys;
    }

    private static Set<String> toSet(String[] strings) {
        return new LinkedHashSet<>(Arrays.asList(strings));
    }

    private final static Logger log = LoggerFactory.getLogger(NStreamQueryPlanBuilder.class);
}
