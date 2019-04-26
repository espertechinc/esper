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
import com.espertech.esper.common.internal.compile.stage1.spec.OuterJoinDesc;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.context.aifactory.select.StreamJoinAnalysisResultCompileTime;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.historical.common.HistoricalStreamIndexListForge;
import com.espertech.esper.common.internal.epl.historical.common.HistoricalViewableDesc;
import com.espertech.esper.common.internal.epl.join.assemble.AssemblyStrategyTreeBuilder;
import com.espertech.esper.common.internal.epl.join.assemble.BaseAssemblyNodeFactory;
import com.espertech.esper.common.internal.epl.join.base.JoinSetComposerPrototypeHistoricalDesc;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphForge;
import com.espertech.esper.common.internal.epl.join.queryplan.*;
import com.espertech.esper.common.internal.epl.join.queryplanouter.InnerJoinGraph;
import com.espertech.esper.common.internal.epl.join.queryplanouter.LookupInstructionPlanForge;
import com.espertech.esper.common.internal.epl.join.queryplanouter.OuterInnerDirectionalGraph;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.serde.compiletime.resolve.SerdeCompileTimeResolverNonHA;
import com.espertech.esper.common.internal.type.OuterJoinType;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.util.DependencyGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

/**
 * Builds a query plan for 3 or more streams in a outer join.
 */
public class NStreamOuterQueryPlanBuilder {
    protected static QueryPlanForgeDesc build(QueryGraphForge queryGraph,
                                          OuterJoinDesc[] outerJoinDescList,
                                          String[] streamNames,
                                          EventType[] typesPerStream,
                                          HistoricalViewableDesc historicalViewableDesc,
                                          DependencyGraph dependencyGraph,
                                          HistoricalStreamIndexListForge[] historicalStreamIndexLists,
                                          String[][][] indexedStreamsUniqueProps,
                                          TableMetaData[] tablesPerStream,
                                          StreamJoinAnalysisResultCompileTime streamJoinAnalysisResult,
                                          StatementRawInfo statementRawInfo,
                                          StatementCompileTimeServices services)
        throws ExprValidationException {
        if (log.isDebugEnabled()) {
            log.debug(".build filterQueryGraph=" + queryGraph);
        }

        int numStreams = queryGraph.getNumStreams();
        QueryPlanNodeForge[] planNodeSpecs = new QueryPlanNodeForge[numStreams];
        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>();

        // Build index specifications
        QueryPlanIndexForge[] indexSpecs = QueryPlanIndexBuilder.buildIndexSpec(queryGraph, typesPerStream, indexedStreamsUniqueProps);

        // any historical streams don't get indexes, the lookup strategy accounts for cached indexes
        if (historicalViewableDesc.isHasHistorical()) {
            for (int i = 0; i < historicalViewableDesc.getHistorical().length; i++) {
                if (historicalViewableDesc.getHistorical()[i]) {
                    indexSpecs[i] = null;
                }
            }
        }

        // Build graph of the outer join to inner table relationships.
        // Build a map of inner joins.
        OuterInnerDirectionalGraph outerInnerGraph;
        InnerJoinGraph innerJoinGraph;
        if (outerJoinDescList.length > 0) {
            outerInnerGraph = graphOuterJoins(numStreams, outerJoinDescList);
            innerJoinGraph = InnerJoinGraph.graphInnerJoins(numStreams, outerJoinDescList);
        } else {
            // all inner joins - thereby no (or empty) directional graph
            outerInnerGraph = new OuterInnerDirectionalGraph(numStreams);
            innerJoinGraph = new InnerJoinGraph(numStreams, true);
        }
        if (log.isDebugEnabled()) {
            log.debug(".build directional graph=" + outerInnerGraph.print());
        }

        // For each stream determine the query plan
        for (int streamNo = 0; streamNo < numStreams; streamNo++) {
            // no plan for historical streams that are dependent upon other streams
            if ((historicalViewableDesc.getHistorical()[streamNo]) && (dependencyGraph.hasDependency(streamNo))) {
                planNodeSpecs[streamNo] = new QueryPlanNodeNoOpForge();
                continue;
            }

            QueryPlanNodeForgeDesc desc = buildPlanNode(numStreams, streamNo, streamNames, queryGraph, outerInnerGraph, outerJoinDescList, innerJoinGraph, indexSpecs, typesPerStream, historicalViewableDesc.getHistorical(), dependencyGraph, historicalStreamIndexLists, tablesPerStream, streamJoinAnalysisResult, statementRawInfo, services);
            QueryPlanNodeForge queryPlanNode = desc.getForge();
            additionalForgeables.addAll(desc.getAdditionalForgeables());

            if (log.isDebugEnabled()) {
                log.debug(".build spec for stream '" + streamNames[streamNo] +
                    "' number " + streamNo + " is " + queryPlanNode);
            }

            planNodeSpecs[streamNo] = queryPlanNode;
        }

        QueryPlanForge queryPlan = new QueryPlanForge(indexSpecs, planNodeSpecs);
        if (log.isDebugEnabled()) {
            log.debug(".build query plan=" + queryPlan.toString());
        }

        return new QueryPlanForgeDesc(queryPlan, additionalForgeables);
    }

    private static QueryPlanNodeForgeDesc buildPlanNode(int numStreams,
                                                    int streamNo,
                                                    String[] streamNames,
                                                    QueryGraphForge queryGraph,
                                                    OuterInnerDirectionalGraph outerInnerGraph,
                                                    OuterJoinDesc[] outerJoinDescList,
                                                    InnerJoinGraph innerJoinGraph,
                                                    QueryPlanIndexForge[] indexSpecs,
                                                    EventType[] typesPerStream,
                                                    boolean[] isHistorical,
                                                    DependencyGraph dependencyGraph,
                                                    HistoricalStreamIndexListForge[] historicalStreamIndexLists,
                                                    TableMetaData[] tablesPerStream,
                                                    StreamJoinAnalysisResultCompileTime streamJoinAnalysisResult,
                                                    StatementRawInfo statementRawInfo,
                                                    StatementCompileTimeServices services)
        throws ExprValidationException {
        // For each stream build an array of substreams, considering required streams (inner joins) first
        // The order is relevant therefore preserving order via a LinkedHashMap.
        LinkedHashMap<Integer, int[]> substreamsPerStream = new LinkedHashMap<Integer, int[]>();
        boolean[] requiredPerStream = new boolean[numStreams];
        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>(2);

        // Recursive populating the required (outer) and optional (inner) relationships
        // of this stream and the substream
        Set<Integer> completedStreams = new HashSet<Integer>();
        // keep track of tree path as only those stream events are always available to historical streams
        Stack<Integer> streamCallStack = new Stack<Integer>();
        streamCallStack.push(streamNo);

        // For all inner-joins, the algorithm is slightly different
        if (innerJoinGraph.isAllInnerJoin()) {
            Arrays.fill(requiredPerStream, true);
            recursiveBuildInnerJoin(streamNo, streamCallStack, queryGraph, completedStreams, substreamsPerStream, dependencyGraph);

            // compute a best chain to see if all streams are handled and add the remaining
            NStreamQueryPlanBuilder.BestChainResult bestChain = NStreamQueryPlanBuilder.computeBestPath(streamNo, queryGraph, dependencyGraph);
            addNotYetNavigated(streamNo, numStreams, substreamsPerStream, bestChain);
        } else {
            recursiveBuild(streamNo, streamCallStack, queryGraph, outerInnerGraph, innerJoinGraph, completedStreams, substreamsPerStream, requiredPerStream, dependencyGraph);
        }

        // verify the substreamsPerStream, all streams must exists and be linked
        verifyJoinedPerStream(streamNo, substreamsPerStream);

        // build list of instructions for lookup
        LookupInstructionPlanDesc lookupDesc = buildLookupInstructions(streamNo, substreamsPerStream, requiredPerStream,
            streamNames, queryGraph, indexSpecs, typesPerStream, outerJoinDescList, isHistorical, historicalStreamIndexLists, tablesPerStream, streamJoinAnalysisResult, statementRawInfo, services);
        List<LookupInstructionPlanForge> lookupInstructions = lookupDesc.getForges();
        additionalForgeables.addAll(lookupDesc.getAdditionalForgeables());

        // build historical index and lookup strategies
        for (LookupInstructionPlanForge lookups : lookupInstructions) {
            for (HistoricalDataPlanNodeForge historical : lookups.getHistoricalPlans()) {
                if (historical == null) {
                    continue;
                }
                JoinSetComposerPrototypeHistoricalDesc desc = historicalStreamIndexLists[historical.getStreamNum()].getStrategy(historical.getLookupStreamNum(), statementRawInfo, services.getSerdeResolver());
                historical.setHistoricalIndexLookupStrategy(desc.getLookupForge());
                historical.setPollResultIndexingStrategy(desc.getIndexingForge());
                additionalForgeables.addAll(desc.getAdditionalForgeables());
            }
        }

        // build strategy tree for putting the result back together
        BaseAssemblyNodeFactory assemblyTopNodeFactory = AssemblyStrategyTreeBuilder.build(streamNo, substreamsPerStream, requiredPerStream);
        List<BaseAssemblyNodeFactory> assemblyInstructionFactories = BaseAssemblyNodeFactory.getDescendentNodesBottomUp(assemblyTopNodeFactory);

        LookupInstructionQueryPlanNodeForge forge = new LookupInstructionQueryPlanNodeForge(streamNo, streamNames[streamNo], numStreams, requiredPerStream,
            lookupInstructions, assemblyInstructionFactories);
        return new QueryPlanNodeForgeDesc(forge, additionalForgeables);
    }

    private static void addNotYetNavigated(int streamNo, int numStreams, LinkedHashMap<Integer, int[]> substreamsPerStream, NStreamQueryPlanBuilder.BestChainResult bestChain) {
        // sum up all substreams (the query plan for each stream: nested iteration or cardinal)
        Set<Integer> streams = new HashSet<Integer>();
        streams.add(streamNo);
        recursiveAdd(streamNo, streamNo, substreamsPerStream, streams, false);

        // we are done, all have navigated
        if (streams.size() == numStreams) {
            return;
        }

        int previous = streamNo;
        for (int stream : bestChain.getChain()) {

            if (streams.contains(stream)) {
                previous = stream;
                continue;
            }

            // add node as a nested join to the previous stream
            int[] substreams = substreamsPerStream.get(previous);
            if (substreams == null) {
                substreams = new int[0];
            }
            int[] added = CollectionUtil.addValue(substreams, stream);
            substreamsPerStream.put(previous, added);

            if (!substreamsPerStream.containsKey(stream)) {
                substreamsPerStream.put(stream, new int[0]);
            }

            previous = stream;
        }
    }

    private static LookupInstructionPlanDesc buildLookupInstructions(
        int rootStreamNum,
        LinkedHashMap<Integer, int[]> substreamsPerStream,
        boolean[] requiredPerStream,
        String[] streamNames,
        QueryGraphForge queryGraph,
        QueryPlanIndexForge[] indexSpecs,
        EventType[] typesPerStream,
        OuterJoinDesc[] outerJoinDescList,
        boolean[] isHistorical,
        HistoricalStreamIndexListForge[] historicalStreamIndexLists,
        TableMetaData[] tablesPerStream,
        StreamJoinAnalysisResultCompileTime streamJoinAnalysisResult,
        StatementRawInfo statementRawInfo,
        StatementCompileTimeServices services) {

        List<LookupInstructionPlanForge> result = new LinkedList<LookupInstructionPlanForge>();
        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>(2);

        for (int fromStream : substreamsPerStream.keySet()) {
            int[] substreams = substreamsPerStream.get(fromStream);

            // for streams with no substreams we don't need to look up
            if (substreams.length == 0) {
                continue;
            }

            TableLookupPlanForge[] plans = new TableLookupPlanForge[substreams.length];
            HistoricalDataPlanNodeForge[] historicalPlans = new HistoricalDataPlanNodeForge[substreams.length];

            for (int i = 0; i < substreams.length; i++) {
                int toStream = substreams[i];

                if (isHistorical[toStream]) {
                    // There may not be an outer-join descriptor, use if provided to build the associated expression
                    ExprNode outerJoinExpr = null;
                    if (outerJoinDescList.length > 0) {
                        OuterJoinDesc outerJoinDesc;
                        if (toStream == 0) {
                            outerJoinDesc = outerJoinDescList[0];
                        } else {
                            outerJoinDesc = outerJoinDescList[toStream - 1];
                        }
                        outerJoinExpr = outerJoinDesc.makeExprNode(statementRawInfo, services);
                    }

                    if (historicalStreamIndexLists[toStream] == null) {
                        historicalStreamIndexLists[toStream] = new HistoricalStreamIndexListForge(toStream, typesPerStream, queryGraph);
                    }
                    historicalStreamIndexLists[toStream].addIndex(fromStream);
                    historicalPlans[i] = new HistoricalDataPlanNodeForge(toStream, rootStreamNum, fromStream, typesPerStream.length, outerJoinExpr == null ? null : outerJoinExpr.getForge());
                } else {
                    TableLookupPlanDesc planDesc = NStreamQueryPlanBuilder.createLookupPlan(queryGraph, fromStream, toStream, streamJoinAnalysisResult.isVirtualDW(toStream), indexSpecs[toStream], typesPerStream, tablesPerStream[toStream], statementRawInfo, SerdeCompileTimeResolverNonHA.INSTANCE);
                    plans[i] = planDesc.getForge();
                    additionalForgeables.addAll(planDesc.getAdditionalForgeables());
                }
            }

            String fromStreamName = streamNames[fromStream];
            LookupInstructionPlanForge instruction = new LookupInstructionPlanForge(fromStream, fromStreamName, substreams, plans, historicalPlans, requiredPerStream);
            result.add(instruction);
        }

        return new LookupInstructionPlanDesc(result, additionalForgeables);
    }

    /**
     * Recusivly builds a substream-per-stream ordered tree graph using the
     * join information supplied for outer joins and from the query graph (where clause).
     * <p>
     * Required streams are considered first and their lookup is placed first in the list
     * to gain performance.
     *
     * @param streamNum           is the root stream number that supplies the incoming event to build the tree for
     * @param queryGraph          contains where-clause stream relationship info
     * @param outerInnerGraph     contains the outer join stream relationship info
     * @param completedStreams    is a temporary holder for streams already considered
     * @param substreamsPerStream is the ordered, tree-like structure to be filled
     * @param requiredPerStream   indicates which streams are required and which are optional
     * @param streamCallStack     the query plan call stack of streams available via cursor
     * @param dependencyGraph     - dependencies between historical streams
     * @param innerJoinGraph      inner join graph
     * @throws ExprValidationException if the query planning failed
     */
    protected static void recursiveBuild(int streamNum,
                                         Stack<Integer> streamCallStack,
                                         QueryGraphForge queryGraph,
                                         OuterInnerDirectionalGraph outerInnerGraph,
                                         InnerJoinGraph innerJoinGraph,
                                         Set<Integer> completedStreams,
                                         LinkedHashMap<Integer, int[]> substreamsPerStream,
                                         boolean[] requiredPerStream,
                                         DependencyGraph dependencyGraph
    )
        throws ExprValidationException {
        // add this stream to the set of completed streams
        completedStreams.add(streamNum);

        // check if the dependencies have been satisfied
        if (dependencyGraph.hasDependency(streamNum)) {
            Set<Integer> dependencies = dependencyGraph.getDependenciesForStream(streamNum);
            for (Integer dependentStream : dependencies) {
                if (!streamCallStack.contains(dependentStream)) {
                    throw new ExprValidationException("Historical stream " + streamNum + " parameter dependency originating in stream " + dependentStream + " cannot or may not be satisfied by the join");
                }
            }
        }

        // Determine the streams we can navigate to from this stream
        Set<Integer> navigableStreams = queryGraph.getNavigableStreams(streamNum);

        // add unqualified navigable streams (since on-expressions in outer joins are optional)
        Set<Integer> unqualifiedNavigable = outerInnerGraph.getUnqualifiedNavigableStreams().get(streamNum);
        if (unqualifiedNavigable != null) {
            navigableStreams.addAll(unqualifiedNavigable);
        }

        // remove those already done
        navigableStreams.removeAll(completedStreams);

        // Which streams are inner streams to this stream (optional), which ones are outer to the stream (required)
        Set<Integer> requiredStreams = getOuterStreams(streamNum, navigableStreams, outerInnerGraph);

        // Add inner joins, if any, unless already completed for this stream
        innerJoinGraph.addRequiredStreams(streamNum, requiredStreams, completedStreams);

        Set<Integer> optionalStreams = getInnerStreams(streamNum, navigableStreams, outerInnerGraph, innerJoinGraph, completedStreams);

        // Remove from the required streams the optional streams which places 'full' joined streams
        // into the optional stream category
        requiredStreams.removeAll(optionalStreams);

        // if we are a leaf node, we are done
        if (navigableStreams.isEmpty()) {
            substreamsPerStream.put(streamNum, new int[0]);
            return;
        }

        // First the outer (required) streams to this stream, then the inner (optional) streams
        int[] substreams = new int[requiredStreams.size() + optionalStreams.size()];
        substreamsPerStream.put(streamNum, substreams);
        int count = 0;
        for (int stream : requiredStreams) {
            substreams[count++] = stream;
            requiredPerStream[stream] = true;
        }
        for (int stream : optionalStreams) {
            substreams[count++] = stream;
        }

        // next we look at all the required streams and add their dependent streams
        for (int stream : requiredStreams) {
            completedStreams.add(stream);
        }

        for (int stream : requiredStreams) {
            streamCallStack.push(stream);
            recursiveBuild(stream, streamCallStack, queryGraph, outerInnerGraph, innerJoinGraph,
                completedStreams, substreamsPerStream, requiredPerStream, dependencyGraph);
            streamCallStack.pop();
        }
        // look at all the optional streams and add their dependent streams
        for (int stream : optionalStreams) {
            streamCallStack.push(stream);
            recursiveBuild(stream, streamCallStack, queryGraph, outerInnerGraph, innerJoinGraph,
                completedStreams, substreamsPerStream, requiredPerStream, dependencyGraph);
            streamCallStack.pop();
        }
    }

    /**
     * Recusivly builds a substream-per-stream ordered tree graph using the
     * join information supplied for outer joins and from the query graph (where clause).
     * <p>
     * Required streams are considered first and their lookup is placed first in the list
     * to gain performance.
     *
     * @param streamNum           is the root stream number that supplies the incoming event to build the tree for
     * @param queryGraph          contains where-clause stream relationship info
     * @param completedStreams    is a temporary holder for streams already considered
     * @param substreamsPerStream is the ordered, tree-like structure to be filled
     * @param streamCallStack     the query plan call stack of streams available via cursor
     * @param dependencyGraph     - dependencies between historical streams
     * @throws ExprValidationException if the query planning failed
     */
    protected static void recursiveBuildInnerJoin(int streamNum,
                                                  Stack<Integer> streamCallStack,
                                                  QueryGraphForge queryGraph,
                                                  Set<Integer> completedStreams,
                                                  LinkedHashMap<Integer, int[]> substreamsPerStream,
                                                  DependencyGraph dependencyGraph)
        throws ExprValidationException {
        // add this stream to the set of completed streams
        completedStreams.add(streamNum);

        // check if the dependencies have been satisfied
        if (dependencyGraph.hasDependency(streamNum)) {
            Set<Integer> dependencies = dependencyGraph.getDependenciesForStream(streamNum);
            for (Integer dependentStream : dependencies) {
                if (!streamCallStack.contains(dependentStream)) {
                    throw new ExprValidationException("Historical stream " + streamNum + " parameter dependency originating in stream " + dependentStream + " cannot or may not be satisfied by the join");
                }
            }
        }

        // Determine the streams we can navigate to from this stream
        Set<Integer> navigableStreams = queryGraph.getNavigableStreams(streamNum);

        // remove streams with a dependency on other streams not yet processed
        Integer[] navigableStreamArr = navigableStreams.toArray(new Integer[navigableStreams.size()]);
        for (int navigableStream : navigableStreamArr) {
            if (dependencyGraph.hasUnsatisfiedDependency(navigableStream, completedStreams)) {
                navigableStreams.remove(navigableStream);
            }
        }

        // remove those already done
        navigableStreams.removeAll(completedStreams);

        // if we are a leaf node, we are done
        if (navigableStreams.isEmpty()) {
            substreamsPerStream.put(streamNum, new int[0]);
            return;
        }

        // First the outer (required) streams to this stream, then the inner (optional) streams
        int[] substreams = new int[navigableStreams.size()];
        substreamsPerStream.put(streamNum, substreams);
        int count = 0;
        for (int stream : navigableStreams) {
            substreams[count++] = stream;
            completedStreams.add(stream);
        }

        for (int stream : navigableStreams) {
            streamCallStack.push(stream);
            recursiveBuildInnerJoin(stream, streamCallStack, queryGraph, completedStreams, substreamsPerStream, dependencyGraph);
            streamCallStack.pop();
        }
    }

    private static Set<Integer> getInnerStreams(int fromStream, Set<Integer> toStreams, OuterInnerDirectionalGraph outerInnerGraph,
                                                InnerJoinGraph innerJoinGraph,
                                                Set<Integer> completedStreams) {
        Set<Integer> innerStreams = new HashSet<Integer>();
        for (int toStream : toStreams) {
            if (outerInnerGraph.isInner(fromStream, toStream)) {
                // if the to-stream, recursively, has an inner join itself, it becomes a required stream and not optional
                boolean hasInnerJoin = false;
                if (!innerJoinGraph.isEmpty()) {
                    HashSet<Integer> doNotUseStreams = new HashSet<Integer>(completedStreams);
                    completedStreams.add(fromStream);
                    hasInnerJoin = recursiveHasInnerJoin(toStream, outerInnerGraph, innerJoinGraph, doNotUseStreams);
                }

                if (!hasInnerJoin) {
                    innerStreams.add(toStream);
                }
            }
        }
        return innerStreams;
    }

    private static boolean recursiveHasInnerJoin(int toStream, OuterInnerDirectionalGraph outerInnerGraph, InnerJoinGraph innerJoinGraph, Set<Integer> completedStreams) {
        // Check if the to-stream is in any of the inner joins
        boolean hasInnerJoin = innerJoinGraph.hasInnerJoin(toStream);

        if (hasInnerJoin) {
            return true;
        }

        Set<Integer> innerToToStream = outerInnerGraph.getInner(toStream);
        if (innerToToStream != null) {
            for (int nextStream : innerToToStream) {
                if (completedStreams.contains(nextStream)) {
                    continue;
                }

                HashSet<Integer> notConsider = new HashSet<Integer>(completedStreams);
                notConsider.add(toStream);
                boolean result = recursiveHasInnerJoin(nextStream, outerInnerGraph, innerJoinGraph, notConsider);

                if (result) {
                    return true;
                }
            }
        }

        Set<Integer> outerToToStream = outerInnerGraph.getOuter(toStream);
        if (outerToToStream != null) {
            for (int nextStream : outerToToStream) {
                if (completedStreams.contains(nextStream)) {
                    continue;
                }

                HashSet<Integer> notConsider = new HashSet<Integer>(completedStreams);
                notConsider.add(toStream);
                boolean result = recursiveHasInnerJoin(nextStream, outerInnerGraph, innerJoinGraph, notConsider);

                if (result) {
                    return true;
                }
            }
        }

        return false;
    }

    // which streams are to this table an outer stream
    private static Set<Integer> getOuterStreams(int fromStream, Set<Integer> toStreams, OuterInnerDirectionalGraph outerInnerGraph) {
        Set<Integer> outerStreams = new HashSet<Integer>();
        for (int toStream : toStreams) {
            if (outerInnerGraph.isOuter(toStream, fromStream)) {
                outerStreams.add(toStream);
            }
        }
        return outerStreams;
    }

    /**
     * Builds a graph of outer joins given the outer join information from the statement.
     * Eliminates right and left joins and full joins by placing the information in a graph object.
     *
     * @param numStreams        - is the number of streams
     * @param outerJoinDescList - list of outer join stream numbers and property names
     * @return graph object
     */
    protected static OuterInnerDirectionalGraph graphOuterJoins(int numStreams, OuterJoinDesc[] outerJoinDescList) {
        if ((outerJoinDescList.length + 1) != numStreams) {
            throw new IllegalArgumentException("Number of outer join descriptors and number of streams not matching up");
        }

        OuterInnerDirectionalGraph graph = new OuterInnerDirectionalGraph(numStreams);

        for (int i = 0; i < outerJoinDescList.length; i++) {
            OuterJoinDesc desc = outerJoinDescList[i];
            int streamMax = i + 1;       // the outer join must references streams less then streamMax

            // Check outer join on-expression, if provided
            int streamOne;
            int streamTwo;
            int lowerStream;
            int higherStream;
            if (desc.getOptLeftNode() != null) {
                streamOne = desc.getOptLeftNode().getStreamId();
                streamTwo = desc.getOptRightNode().getStreamId();

                if ((streamOne > streamMax) || (streamTwo > streamMax) ||
                    (streamOne == streamTwo)) {
                    throw new IllegalArgumentException("Outer join descriptors reference future streams, or same streams");
                }

                // Determine who is the first stream in the streams listed
                lowerStream = streamOne;
                higherStream = streamTwo;
                if (streamOne > streamTwo) {
                    lowerStream = streamTwo;
                    higherStream = streamOne;
                }
            } else {
                streamOne = i;
                streamTwo = i + 1;
                lowerStream = i;
                higherStream = i + 1;

                graph.addUnqualifiedNavigable(streamOne, streamTwo);
            }

            // Add to graph
            if (desc.getOuterJoinType() == OuterJoinType.FULL) {
                graph.add(streamOne, streamTwo);
                graph.add(streamTwo, streamOne);
            } else if (desc.getOuterJoinType() == OuterJoinType.LEFT) {
                graph.add(lowerStream, higherStream);
            } else if (desc.getOuterJoinType() == OuterJoinType.RIGHT) {
                graph.add(higherStream, lowerStream);
            } else if (desc.getOuterJoinType() == OuterJoinType.INNER) {
                // no navigability for inner joins
            } else {
                throw new IllegalArgumentException("Outer join descriptors join type not handled, type=" + desc.getOuterJoinType());
            }
        }

        return graph;
    }

    /**
     * Verifies that the tree-like structure representing which streams join (lookup) into which sub-streams
     * is correct, ie. all streams are included and none are listed twice.
     *
     * @param rootStream             is the stream supplying the incoming event
     * @param streamsJoinedPerStream is keyed by the from-stream number and contains as values all
     *                               stream numbers of lookup into to-streams.
     */
    public static void verifyJoinedPerStream(int rootStream, Map<Integer, int[]> streamsJoinedPerStream) {
        Set<Integer> streams = new HashSet<Integer>();
        streams.add(rootStream);

        recursiveAdd(rootStream, rootStream, streamsJoinedPerStream, streams, true);

        if (streams.size() != streamsJoinedPerStream.size()) {
            throw new IllegalArgumentException("Not all streams found, streamsJoinedPerStream=" +
                print(streamsJoinedPerStream));
        }
    }

    private static void recursiveAdd(int validatedStream, int currentStream, Map<Integer, int[]> streamsJoinedPerStream, Set<Integer> streams, boolean verify) {
        if (currentStream >= streamsJoinedPerStream.size() && verify) {
            throw new IllegalArgumentException("Error in stream " + currentStream + " streamsJoinedPerStream=" +
                print(streamsJoinedPerStream));
        }
        int[] joinedStreams = streamsJoinedPerStream.get(currentStream);
        for (int i = 0; i < joinedStreams.length; i++) {
            int addStream = joinedStreams[i];
            if (streams.contains(addStream)) {
                throw new IllegalArgumentException("Stream " + addStream + " found twice when validating " + validatedStream);
            }
            streams.add(addStream);
            recursiveAdd(validatedStream, addStream, streamsJoinedPerStream, streams, verify);
        }
    }

    /**
     * Returns textual presentation of stream-substream relationships.
     *
     * @param streamsJoinedPerStream is the tree-like structure of stream-substream
     * @return textual presentation
     */
    public static String print(Map<Integer, int[]> streamsJoinedPerStream) {
        StringWriter buf = new StringWriter();
        PrintWriter printer = new PrintWriter(buf);

        for (int stream : streamsJoinedPerStream.keySet()) {
            int[] substreams = streamsJoinedPerStream.get(stream);
            printer.println("stream " + stream + " : " + Arrays.toString(substreams));
        }

        return buf.toString();
    }

    private final static Logger log = LoggerFactory.getLogger(NStreamOuterQueryPlanBuilder.class);
}
