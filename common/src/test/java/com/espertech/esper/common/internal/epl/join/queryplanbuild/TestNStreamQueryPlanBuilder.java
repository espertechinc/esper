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
import com.espertech.esper.common.internal.context.aifactory.select.StreamJoinAnalysisResultCompileTime;
import com.espertech.esper.common.internal.epl.expression.core.ExprIdentNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprIdentNodeImpl;
import com.espertech.esper.common.internal.epl.historical.common.HistoricalViewableDesc;
import com.espertech.esper.common.internal.epl.join.indexlookupplan.FullTableScanLookupPlanForge;
import com.espertech.esper.common.internal.epl.join.indexlookupplan.IndexedTableLookupPlanHashedOnlyForge;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphForge;
import com.espertech.esper.common.internal.epl.join.queryplan.*;
import com.espertech.esper.common.internal.epl.table.compiletime.TableMetaData;
import com.espertech.esper.common.internal.serde.compiletime.resolve.SerdeCompileTimeResolverNonHA;
import com.espertech.esper.common.internal.support.SupportBean_S0;
import com.espertech.esper.common.internal.support.SupportBean_S1;
import com.espertech.esper.common.internal.support.SupportBean_S2;
import com.espertech.esper.common.internal.supportunit.bean.SupportBean_S3;
import com.espertech.esper.common.internal.supportunit.bean.SupportBean_S4;
import com.espertech.esper.common.internal.supportunit.event.SupportEventTypeFactory;
import com.espertech.esper.common.internal.util.DependencyGraph;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static com.espertech.esper.common.internal.supportunit.util.SupportExprNodeFactory.makeIdentNode;

public class TestNStreamQueryPlanBuilder extends TestCase {
    private EventType[] typesPerStream;
    private QueryGraphForge queryGraph;
    private DependencyGraph dependencyGraph;

    public void setUp() {
        typesPerStream = new EventType[]{
            SupportEventTypeFactory.createBeanType(SupportBean_S0.class),
            SupportEventTypeFactory.createBeanType(SupportBean_S1.class),
            SupportEventTypeFactory.createBeanType(SupportBean_S2.class),
            SupportEventTypeFactory.createBeanType(SupportBean_S3.class),
            SupportEventTypeFactory.createBeanType(SupportBean_S4.class)
        };

        queryGraph = new QueryGraphForge(5, null, false);
        queryGraph.addStrictEquals(0, "p00", make(0, "p00"), 1, "p10", make(1, "p10"));
        queryGraph.addStrictEquals(0, "p01", make(0, "p01"), 2, "p20", make(2, "p20"));
        queryGraph.addStrictEquals(4, "p40", make(4, "p40"), 3, "p30", make(3, "p30"));
        queryGraph.addStrictEquals(4, "p41", make(4, "p41"), 3, "p31", make(3, "p31"));
        queryGraph.addStrictEquals(4, "p42", make(4, "p42"), 2, "p21", make(2, "p21"));

        dependencyGraph = new DependencyGraph(5, false);
    }

    public void testBuild() {
        QueryPlanForgeDesc plan = NStreamQueryPlanBuilder.build(queryGraph, typesPerStream, new HistoricalViewableDesc(6), dependencyGraph, null, false, new String[queryGraph.getNumStreams()][][], new TableMetaData[queryGraph.getNumStreams()], new StreamJoinAnalysisResultCompileTime(5), null, SerdeCompileTimeResolverNonHA.INSTANCE);
        log.debug(".testBuild plan=" + plan);
    }

    public void testCreateStreamPlan() {
        QueryPlanIndexForge[] indexes = QueryPlanIndexBuilder.buildIndexSpec(queryGraph, typesPerStream, new String[queryGraph.getNumStreams()][][]);
        for (int i = 0; i < indexes.length; i++) {
            log.debug(".testCreateStreamPlan index " + i + " = " + indexes[i]);
        }

        QueryPlanNodeForgeDesc plan = NStreamQueryPlanBuilder.createStreamPlan(0, new int[]{2, 4, 3, 1}, queryGraph, indexes, typesPerStream, new boolean[5], null, new TableMetaData[queryGraph.getNumStreams()], new StreamJoinAnalysisResultCompileTime(5), null, SerdeCompileTimeResolverNonHA.INSTANCE);

        log.debug(".testCreateStreamPlan plan=" + plan);

        assertTrue(plan.getForge() instanceof NestedIterationNodeForge);
        NestedIterationNodeForge nested = (NestedIterationNodeForge) plan.getForge();
        TableLookupNodeForge tableLookupSpec = (TableLookupNodeForge) nested.getChildNodes().get(0);

        // Check lookup strategy for first lookup
        IndexedTableLookupPlanHashedOnlyForge lookupStrategySpec = (IndexedTableLookupPlanHashedOnlyForge) tableLookupSpec.getLookupStrategySpec();
        assertEquals("p01", ((ExprIdentNode) (lookupStrategySpec.getHashKeys()[0]).getKeyExpr()).getResolvedPropertyName());
        assertEquals(0, lookupStrategySpec.getLookupStream());
        assertEquals(2, lookupStrategySpec.getIndexedStream());
        assertNotNull(lookupStrategySpec.getIndexNum());

        // Check lookup strategy for last lookup
        tableLookupSpec = (TableLookupNodeForge) nested.getChildNodes().get(3);
        FullTableScanLookupPlanForge unkeyedSpecScan = (FullTableScanLookupPlanForge) tableLookupSpec.getLookupStrategySpec();
        assertEquals(1, unkeyedSpecScan.getIndexedStream());
        assertNotNull(unkeyedSpecScan.getIndexNum());
    }

    public void testComputeBestPath() {
        NStreamQueryPlanBuilder.BestChainResult bestChain = NStreamQueryPlanBuilder.computeBestPath(0, queryGraph, dependencyGraph);
        assertEquals(3, bestChain.getDepth());
        assertTrue(Arrays.equals(bestChain.getChain(), new int[]{2, 4, 3, 1}));

        bestChain = NStreamQueryPlanBuilder.computeBestPath(3, queryGraph, dependencyGraph);
        assertEquals(4, bestChain.getDepth());
        assertTrue(Arrays.equals(bestChain.getChain(), new int[]{4, 2, 0, 1}));

        // try a stream that is not connected in any way
        queryGraph = new QueryGraphForge(6, null, false);
        bestChain = NStreamQueryPlanBuilder.computeBestPath(5, queryGraph, dependencyGraph);
        log.debug(".testComputeBestPath bestChain=" + bestChain);
        assertEquals(0, bestChain.getDepth());
        assertTrue(Arrays.equals(bestChain.getChain(), new int[]{0, 1, 2, 3, 4}));
    }

    public void testComputeNavigableDepth() {
        ExprIdentNode fake = makeIdentNode("theString", "s0");
        queryGraph.addStrictEquals(3, "p30", fake, 2, "p20", fake);
        queryGraph.addStrictEquals(2, "p30", fake, 1, "p20", fake);

        int depth = NStreamQueryPlanBuilder.computeNavigableDepth(0, new int[]{1, 2, 3, 4}, queryGraph);
        assertEquals(4, depth);

        depth = NStreamQueryPlanBuilder.computeNavigableDepth(0, new int[]{4, 2, 3, 1}, queryGraph);
        assertEquals(0, depth);

        depth = NStreamQueryPlanBuilder.computeNavigableDepth(4, new int[]{3, 2, 1, 0}, queryGraph);
        assertEquals(4, depth);

        depth = NStreamQueryPlanBuilder.computeNavigableDepth(1, new int[]{0, 3, 4, 2}, queryGraph);
        assertEquals(1, depth);
    }

    public void testBuildDefaultNestingOrder() {
        int[] result = NStreamQueryPlanBuilder.buildDefaultNestingOrder(4, 0);
        assertTrue(Arrays.equals(result, new int[]{1, 2, 3}));

        result = NStreamQueryPlanBuilder.buildDefaultNestingOrder(4, 1);
        assertTrue(Arrays.equals(result, new int[]{0, 2, 3}));

        result = NStreamQueryPlanBuilder.buildDefaultNestingOrder(4, 2);
        assertTrue(Arrays.equals(result, new int[]{0, 1, 3}));

        result = NStreamQueryPlanBuilder.buildDefaultNestingOrder(4, 3);
        assertTrue(Arrays.equals(result, new int[]{0, 1, 2}));
    }

    public void testIsDependencySatisfied() {
        DependencyGraph graph = new DependencyGraph(3, false);
        graph.addDependency(1, 0);
        graph.addDependency(2, 0);

        assertTrue(NStreamQueryPlanBuilder.isDependencySatisfied(0, new int[]{1, 2}, graph));
        assertFalse(NStreamQueryPlanBuilder.isDependencySatisfied(1, new int[]{0, 2}, graph));
        assertFalse(NStreamQueryPlanBuilder.isDependencySatisfied(2, new int[]{0, 1}, graph));

        graph = new DependencyGraph(5, false);
        graph.addDependency(4, 1);
        graph.addDependency(4, 2);
        graph.addDependency(2, 0);

        assertTrue(NStreamQueryPlanBuilder.isDependencySatisfied(0, new int[]{1, 2, 3, 4}, graph));
        assertTrue(NStreamQueryPlanBuilder.isDependencySatisfied(1, new int[]{0, 2, 3, 4}, graph));
        assertFalse(NStreamQueryPlanBuilder.isDependencySatisfied(1, new int[]{2, 0, 3, 4}, graph));
        assertFalse(NStreamQueryPlanBuilder.isDependencySatisfied(1, new int[]{4, 0, 3, 2}, graph));
        assertFalse(NStreamQueryPlanBuilder.isDependencySatisfied(3, new int[]{4, 0, 1, 2}, graph));
        assertFalse(NStreamQueryPlanBuilder.isDependencySatisfied(2, new int[]{3, 1, 4, 0}, graph));
        assertTrue(NStreamQueryPlanBuilder.isDependencySatisfied(3, new int[]{1, 0, 2, 4}, graph));
    }

    private ExprIdentNode make(int stream, String p) {
        return new ExprIdentNodeImpl(typesPerStream[stream], p, stream);
    }

    private final static Logger log = LoggerFactory.getLogger(TestNStreamQueryPlanBuilder.class);
}
