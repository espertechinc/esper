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
package com.espertech.esper.regressionlib.support.util;

import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCompare;
import com.espertech.esper.common.internal.epl.join.indexlookupplan.*;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryHashKeyedForge;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryRangeForge;
import com.espertech.esper.common.internal.epl.join.queryplan.*;
import com.espertech.esper.common.internal.epl.join.queryplanouter.LookupInstructionPlanForge;
import org.junit.Assert;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SupportQueryPlanIndexHelper {

    public static String getIndexedExpressions(Map<TableLookupIndexReqKey, QueryPlanIndexItemForge> entries) {
        StringWriter buf = new StringWriter();
        for (Map.Entry<TableLookupIndexReqKey, QueryPlanIndexItemForge> entry : entries.entrySet()) {
            buf.append(Arrays.toString(entry.getValue().getHashProps()));
        }
        return buf.toString();
    }

    public static void compareQueryPlans(QueryPlanForge expectedPlan, QueryPlanForge actualPlan) {
        Map<TableLookupIndexReqKey, TableLookupIndexReqKey> indexNameMapping = new HashMap<TableLookupIndexReqKey, TableLookupIndexReqKey>();
        compareIndexes(expectedPlan.getIndexSpecs(), actualPlan.getIndexSpecs(), indexNameMapping);
        compareExecNodeSpecs(expectedPlan.getExecNodeSpecs(), actualPlan.getExecNodeSpecs(), indexNameMapping);
    }

    private static void compareIndexes(QueryPlanIndexForge[] expected, QueryPlanIndexForge[] actual, Map<TableLookupIndexReqKey, TableLookupIndexReqKey> indexNameMapping) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            compareIndex(i, expected[i], actual[i], indexNameMapping);
        }
    }

    private static void compareIndex(int streamNum, QueryPlanIndexForge expected, QueryPlanIndexForge actual, Map<TableLookupIndexReqKey, TableLookupIndexReqKey> indexNameMapping) {
        Map<TableLookupIndexReqKey, QueryPlanIndexItemForge> actualItems = actual.getItems();
        Map<TableLookupIndexReqKey, QueryPlanIndexItemForge> expectedItems = expected.getItems();
        assertEquals("Number of indexes mismatch for stream " + streamNum, expectedItems.size(), actualItems.size());

        Iterator<Map.Entry<TableLookupIndexReqKey, QueryPlanIndexItemForge>> itActual = actualItems.entrySet().iterator();
        Iterator<Map.Entry<TableLookupIndexReqKey, QueryPlanIndexItemForge>> itExpected = expectedItems.entrySet().iterator();

        int count = 0;
        for (; itActual.hasNext(); ) {
            Map.Entry<TableLookupIndexReqKey, QueryPlanIndexItemForge> actualItem = itActual.next();
            Map.Entry<TableLookupIndexReqKey, QueryPlanIndexItemForge> expectedItem = itExpected.next();
            SupportQueryPlanIndexHelper.compareIndexItem(streamNum, count, expectedItem.getValue(), actualItem.getValue());
            count++;
            indexNameMapping.put(actualItem.getKey(), expectedItem.getKey());
        }
    }

    private static void compareExecNodeSpecs(QueryPlanNodeForge[] expected, QueryPlanNodeForge[] actual, Map<TableLookupIndexReqKey, TableLookupIndexReqKey> indexNameMapping) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            compareExecNodeSpec(i, expected[i], actual[i], indexNameMapping);
        }
    }

    private static void compareExecNodeSpec(int streamNum, QueryPlanNodeForge expected, QueryPlanNodeForge actual, Map<TableLookupIndexReqKey, TableLookupIndexReqKey> indexNameMapping) {
        if (actual instanceof QueryPlanNodeNoOpForge && expected == null) {
        } else if (actual instanceof TableLookupNodeForge && expected instanceof TableLookupNodeForge) {
            compareTableLookup(streamNum, (TableLookupNodeForge) expected, (TableLookupNodeForge) actual, indexNameMapping);
        } else if (actual instanceof TableOuterLookupNodeForge && expected instanceof TableOuterLookupNodeForge) {
            compareTableLookupOuter(streamNum, (TableOuterLookupNodeForge) expected, (TableOuterLookupNodeForge) actual, indexNameMapping);
        } else if (actual instanceof LookupInstructionQueryPlanNodeForge && expected instanceof LookupInstructionQueryPlanNodeForge) {
            compareInstruction(streamNum, (LookupInstructionQueryPlanNodeForge) expected, (LookupInstructionQueryPlanNodeForge) actual, indexNameMapping);
        } else {
            Assert.fail("Failed to compare plan node for stream " + streamNum + ", unhandled plan " + actual.getClass().getName());
        }
    }

    private static void compareInstruction(int streamNum, LookupInstructionQueryPlanNodeForge expected, LookupInstructionQueryPlanNodeForge actual, Map<TableLookupIndexReqKey, TableLookupIndexReqKey> indexNameMapping) {
        assertEquals(expected.getRootStream(), actual.getRootStream());
        assertEquals(expected.getRootStreamName(), actual.getRootStreamName());
        assertEquals(expected.getLookupInstructions().size(), actual.getLookupInstructions().size());
        for (int i = 0; i < expected.getLookupInstructions().size(); i++) {
            compareInstructionDetail(streamNum, i, expected.getLookupInstructions().get(i), actual.getLookupInstructions().get(i), indexNameMapping);
        }
    }

    private static void compareInstructionDetail(int streamNum, int numInstruction, LookupInstructionPlanForge expected, LookupInstructionPlanForge actual, Map<TableLookupIndexReqKey, TableLookupIndexReqKey> indexNameMapping) {
        assertEquals(expected.getLookupPlans().length, actual.getLookupPlans().length);
        for (int i = 0; i < expected.getLookupPlans().length; i++) {
            compareTableLookupPlan(streamNum, numInstruction, expected.getLookupPlans()[i], actual.getLookupPlans()[i], indexNameMapping);
        }
    }

    private static void compareTableLookupOuter(int streamNum, TableOuterLookupNodeForge expected, TableOuterLookupNodeForge actual, Map<TableLookupIndexReqKey, TableLookupIndexReqKey> indexNameMapping) {
        compareTableLookupPlan(streamNum, 0, expected.getLookupStrategySpec(), actual.getLookupStrategySpec(), indexNameMapping);
    }

    private static void compareTableLookup(int streamNum, TableLookupNodeForge expected, TableLookupNodeForge actual, Map<TableLookupIndexReqKey, TableLookupIndexReqKey> indexNameMapping) {
        compareTableLookupPlan(streamNum, 0, expected.getTableLookupPlan(), actual.getTableLookupPlan(), indexNameMapping);
    }

    private static void compareTableLookupPlan(int streamNum, int numInstruction, TableLookupPlanForge expectedPlan, TableLookupPlanForge actualPlan, Map<TableLookupIndexReqKey, TableLookupIndexReqKey> indexNameMapping) {
        String message = "Failed at stream " + streamNum + " and instruction " + numInstruction;
        assertEquals(message, expectedPlan.getIndexedStream(), actualPlan.getIndexedStream());
        assertEquals(message, expectedPlan.getLookupStream(), actualPlan.getLookupStream());
        assertEquals(message, expectedPlan.getClass().getSimpleName(), actualPlan.getClass().getSimpleName());

        // assert index mapping
        assertEquals(message, expectedPlan.getIndexNum().length, actualPlan.getIndexNum().length);
        for (int i = 0; i < expectedPlan.getIndexNum().length; i++) {
            TableLookupIndexReqKey expectedIndexKey = expectedPlan.getIndexNum()[i];
            TableLookupIndexReqKey actualIndexKey = actualPlan.getIndexNum()[i];
            assertEquals(message, expectedIndexKey, indexNameMapping.get(actualIndexKey));
        }

        if (expectedPlan instanceof FullTableScanLookupPlanForge && actualPlan instanceof FullTableScanLookupPlanForge) {
        } else if (expectedPlan instanceof IndexedTableLookupPlanHashedOnlyForge && actualPlan instanceof IndexedTableLookupPlanHashedOnlyForge) {
            IndexedTableLookupPlanHashedOnlyForge singleActual = (IndexedTableLookupPlanHashedOnlyForge) actualPlan;
            IndexedTableLookupPlanHashedOnlyForge singleExpected = (IndexedTableLookupPlanHashedOnlyForge) expectedPlan;
            compareIndexDesc(singleExpected.getKeyDescriptor(), singleActual.getKeyDescriptor());
        } else if (expectedPlan instanceof InKeywordTableLookupPlanMultiIdxForge && actualPlan instanceof InKeywordTableLookupPlanMultiIdxForge) {
            InKeywordTableLookupPlanMultiIdxForge inExpected = (InKeywordTableLookupPlanMultiIdxForge) expectedPlan;
            InKeywordTableLookupPlanMultiIdxForge inActual = (InKeywordTableLookupPlanMultiIdxForge) actualPlan;
            assertTrue(ExprNodeUtilityCompare.deepEquals(inExpected.getKeyExpr(), inActual.getKeyExpr(), false));
        } else if (expectedPlan instanceof InKeywordTableLookupPlanSingleIdxForge && actualPlan instanceof InKeywordTableLookupPlanSingleIdxForge) {
            InKeywordTableLookupPlanSingleIdxForge inExpected = (InKeywordTableLookupPlanSingleIdxForge) expectedPlan;
            InKeywordTableLookupPlanSingleIdxForge inActual = (InKeywordTableLookupPlanSingleIdxForge) actualPlan;
            assertTrue(ExprNodeUtilityCompare.deepEquals(inExpected.getExpressions(), inActual.getExpressions(), false));
        } else if (expectedPlan instanceof SortedTableLookupPlanForge && actualPlan instanceof SortedTableLookupPlanForge) {
            SortedTableLookupPlanForge inExpected = (SortedTableLookupPlanForge) expectedPlan;
            SortedTableLookupPlanForge inActual = (SortedTableLookupPlanForge) actualPlan;
            assertEquals(inExpected.getLookupStream(), inActual.getLookupStream());
            assertTrue(ExprNodeUtilityCompare.deepEquals(inExpected.getRangeKeyPair().getExpressions(), inActual.getRangeKeyPair().getExpressions(), false));
        } else {
            Assert.fail("Failed to compare plan for stream " + streamNum + ", found type " + actualPlan.getClass());
        }
    }

    private static void compareIndexDesc(TableLookupKeyDesc expected, TableLookupKeyDesc actual) {
        assertEquals(expected.getHashes().size(), actual.getHashes().size());
        for (int i = 0; i < expected.getHashes().size(); i++) {
            compareIndexDescHash(expected.getHashes().get(i), actual.getHashes().get(i));
        }
        assertEquals(expected.getRanges().size(), actual.getRanges().size());
        for (int i = 0; i < expected.getRanges().size(); i++) {
            compareIndexDescRange(expected.getRanges().get(i), actual.getRanges().get(i));
        }
    }

    private static void compareIndexDescRange(QueryGraphValueEntryRangeForge expected, QueryGraphValueEntryRangeForge actual) {
        assertEquals(expected.toQueryPlan(), actual.toQueryPlan());
    }

    private static void compareIndexDescHash(QueryGraphValueEntryHashKeyedForge expected, QueryGraphValueEntryHashKeyedForge actual) {
        assertEquals(expected.toQueryPlan(), actual.toQueryPlan());
    }

    private static void compareIndexItem(int stream, int num, QueryPlanIndexItemForge expectedIndex, QueryPlanIndexItemForge actualIndex) {
        if (!expectedIndex.equalsCompareSortedProps(actualIndex)) {
            Assert.fail("At stream " + stream + " index " + num + "\nExpected:\n" + expectedIndex + "\n" +
                "Received:\n" + actualIndex + "\n");
        }
    }
}
