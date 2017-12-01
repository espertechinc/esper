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
package com.espertech.esper.supportregression.epl;

import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.join.plan.*;
import org.junit.Assert;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

public class SupportQueryPlanIndexHelper {

    public static String getIndexedExpressions(Map<TableLookupIndexReqKey, QueryPlanIndexItem> entries) {
        StringWriter buf = new StringWriter();
        for (Map.Entry<TableLookupIndexReqKey, QueryPlanIndexItem> entry : entries.entrySet()) {
            buf.append(Arrays.toString(entry.getValue().getIndexProps()));
        }
        return buf.toString();
    }

    public static void compareQueryPlans(QueryPlan expectedPlan, QueryPlan actualPlan) {
        Map<TableLookupIndexReqKey, TableLookupIndexReqKey> indexNameMapping = new HashMap<TableLookupIndexReqKey, TableLookupIndexReqKey>();
        compareIndexes(expectedPlan.getIndexSpecs(), actualPlan.getIndexSpecs(), indexNameMapping);
        compareExecNodeSpecs(expectedPlan.getExecNodeSpecs(), actualPlan.getExecNodeSpecs(), indexNameMapping);
    }

    private static void compareIndexes(QueryPlanIndex[] expected, QueryPlanIndex[] actual, Map<TableLookupIndexReqKey, TableLookupIndexReqKey> indexNameMapping) {
        Assert.assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            compareIndex(i, expected[i], actual[i], indexNameMapping);
        }
    }

    private static void compareIndex(int streamNum, QueryPlanIndex expected, QueryPlanIndex actual, Map<TableLookupIndexReqKey, TableLookupIndexReqKey> indexNameMapping) {
        Map<TableLookupIndexReqKey, QueryPlanIndexItem> actualItems = actual.getItems();
        Map<TableLookupIndexReqKey, QueryPlanIndexItem> expectedItems = expected.getItems();
        Assert.assertEquals("Number of indexes mismatch for stream " + streamNum, expectedItems.size(), actualItems.size());

        Iterator<Map.Entry<TableLookupIndexReqKey, QueryPlanIndexItem>> itActual = actualItems.entrySet().iterator();
        Iterator<Map.Entry<TableLookupIndexReqKey, QueryPlanIndexItem>> itExpected = expectedItems.entrySet().iterator();

        int count = 0;
        for (; itActual.hasNext(); ) {
            Map.Entry<TableLookupIndexReqKey, QueryPlanIndexItem> actualItem = itActual.next();
            Map.Entry<TableLookupIndexReqKey, QueryPlanIndexItem> expectedItem = itExpected.next();
            SupportQueryPlanIndexHelper.compareIndexItem(streamNum, count, expectedItem.getValue(), actualItem.getValue());
            count++;
            indexNameMapping.put(actualItem.getKey(), expectedItem.getKey());
        }
    }

    private static void compareExecNodeSpecs(QueryPlanNode[] expected, QueryPlanNode[] actual, Map<TableLookupIndexReqKey, TableLookupIndexReqKey> indexNameMapping) {
        Assert.assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            compareExecNodeSpec(i, expected[i], actual[i], indexNameMapping);
        }
    }

    private static void compareExecNodeSpec(int streamNum, QueryPlanNode expected, QueryPlanNode actual, Map<TableLookupIndexReqKey, TableLookupIndexReqKey> indexNameMapping) {
        if (actual instanceof QueryPlanNodeNoOp && expected == null) {
        } else if (actual instanceof TableLookupNode && expected instanceof TableLookupNode) {
            compareTableLookup(streamNum, (TableLookupNode) expected, (TableLookupNode) actual, indexNameMapping);
        } else if (actual instanceof TableOuterLookupNode && expected instanceof TableOuterLookupNode) {
            compareTableLookupOuter(streamNum, (TableOuterLookupNode) expected, (TableOuterLookupNode) actual, indexNameMapping);
        } else if (actual instanceof LookupInstructionQueryPlanNode && expected instanceof LookupInstructionQueryPlanNode) {
            compareInstruction(streamNum, (LookupInstructionQueryPlanNode) expected, (LookupInstructionQueryPlanNode) actual, indexNameMapping);
        } else {
            Assert.fail("Failed to compare plan node for stream " + streamNum + ", unhandled plan " + actual.getClass().getName());
        }
    }

    private static void compareInstruction(int streamNum, LookupInstructionQueryPlanNode expected, LookupInstructionQueryPlanNode actual, Map<TableLookupIndexReqKey, TableLookupIndexReqKey> indexNameMapping) {
        assertEquals(expected.getRootStream(), actual.getRootStream());
        assertEquals(expected.getRootStreamName(), actual.getRootStreamName());
        assertEquals(expected.getLookupInstructions().size(), actual.getLookupInstructions().size());
        for (int i = 0; i < expected.getLookupInstructions().size(); i++) {
            compareInstructionDetail(streamNum, i, expected.getLookupInstructions().get(i), actual.getLookupInstructions().get(i), indexNameMapping);
        }
    }

    private static void compareInstructionDetail(int streamNum, int numInstruction, LookupInstructionPlan expected, LookupInstructionPlan actual, Map<TableLookupIndexReqKey, TableLookupIndexReqKey> indexNameMapping) {
        assertEquals(expected.getLookupPlans().length, actual.getLookupPlans().length);
        for (int i = 0; i < expected.getLookupPlans().length; i++) {
            compareTableLookupPlan(streamNum, numInstruction, expected.getLookupPlans()[i], actual.getLookupPlans()[i], indexNameMapping);
        }
    }

    private static void compareTableLookupOuter(int streamNum, TableOuterLookupNode expected, TableOuterLookupNode actual, Map<TableLookupIndexReqKey, TableLookupIndexReqKey> indexNameMapping) {
        compareTableLookupPlan(streamNum, 0, expected.getLookupStrategySpec(), actual.getLookupStrategySpec(), indexNameMapping);
    }

    private static void compareTableLookup(int streamNum, TableLookupNode expected, TableLookupNode actual, Map<TableLookupIndexReqKey, TableLookupIndexReqKey> indexNameMapping) {
        compareTableLookupPlan(streamNum, 0, expected.getTableLookupPlan(), actual.getTableLookupPlan(), indexNameMapping);
    }

    private static void compareTableLookupPlan(int streamNum, int numInstruction, TableLookupPlan expectedPlan, TableLookupPlan actualPlan, Map<TableLookupIndexReqKey, TableLookupIndexReqKey> indexNameMapping) {
        String message = "Failed at stream " + streamNum + " and instruction " + numInstruction;
        Assert.assertEquals(message, expectedPlan.getIndexedStream(), actualPlan.getIndexedStream());
        Assert.assertEquals(message, expectedPlan.getLookupStream(), actualPlan.getLookupStream());
        Assert.assertEquals(message, expectedPlan.getClass().getSimpleName(), actualPlan.getClass().getSimpleName());

        // assert index mapping
        Assert.assertEquals(message, expectedPlan.getIndexNum().length, actualPlan.getIndexNum().length);
        for (int i = 0; i < expectedPlan.getIndexNum().length; i++) {
            TableLookupIndexReqKey expectedIndexKey = expectedPlan.getIndexNum()[i];
            TableLookupIndexReqKey actualIndexKey = actualPlan.getIndexNum()[i];
            Assert.assertEquals(message, expectedIndexKey, indexNameMapping.get(actualIndexKey));
        }

        if (expectedPlan instanceof FullTableScanLookupPlan && actualPlan instanceof FullTableScanLookupPlan) {
        } else if (expectedPlan instanceof IndexedTableLookupPlanSingle && actualPlan instanceof IndexedTableLookupPlanSingle) {
            IndexedTableLookupPlanSingle singleActual = (IndexedTableLookupPlanSingle) actualPlan;
            IndexedTableLookupPlanSingle singleExpected = (IndexedTableLookupPlanSingle) expectedPlan;
            compareIndexDesc(singleExpected.getKeyDescriptor(), singleActual.getKeyDescriptor());
        } else if (expectedPlan instanceof InKeywordTableLookupPlanMultiIdx && actualPlan instanceof InKeywordTableLookupPlanMultiIdx) {
            InKeywordTableLookupPlanMultiIdx inExpected = (InKeywordTableLookupPlanMultiIdx) expectedPlan;
            InKeywordTableLookupPlanMultiIdx inActual = (InKeywordTableLookupPlanMultiIdx) actualPlan;
            assertTrue(ExprNodeUtilityCore.deepEquals(inExpected.getKeyExpr(), inActual.getKeyExpr(), false));
        } else if (expectedPlan instanceof InKeywordTableLookupPlanSingleIdx && actualPlan instanceof InKeywordTableLookupPlanSingleIdx) {
            InKeywordTableLookupPlanSingleIdx inExpected = (InKeywordTableLookupPlanSingleIdx) expectedPlan;
            InKeywordTableLookupPlanSingleIdx inActual = (InKeywordTableLookupPlanSingleIdx) actualPlan;
            assertTrue(ExprNodeUtilityCore.deepEquals(inExpected.getExpressions(), inActual.getExpressions(), false));
        } else if (expectedPlan instanceof SortedTableLookupPlan && actualPlan instanceof SortedTableLookupPlan) {
            SortedTableLookupPlan inExpected = (SortedTableLookupPlan) expectedPlan;
            SortedTableLookupPlan inActual = (SortedTableLookupPlan) actualPlan;
            assertEquals(inExpected.getLookupStream(), inActual.getLookupStream());
            assertTrue(ExprNodeUtilityCore.deepEquals(inExpected.getRangeKeyPair().getExpressions(), inActual.getRangeKeyPair().getExpressions(), false));
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

    private static void compareIndexDescRange(QueryGraphValueEntryRange expected, QueryGraphValueEntryRange actual) {
        assertEquals(expected.toQueryPlan(), actual.toQueryPlan());
    }

    private static void compareIndexDescHash(QueryGraphValueEntryHashKeyed expected, QueryGraphValueEntryHashKeyed actual) {
        assertEquals(expected.toQueryPlan(), actual.toQueryPlan());
    }

    private static void compareIndexItem(int stream, int num, QueryPlanIndexItem expectedIndex, QueryPlanIndexItem actualIndex) {
        if (!expectedIndex.equalsCompareSortedProps(actualIndex)) {
            Assert.fail("At stream " + stream + " index " + num + "\nExpected:\n" + expectedIndex + "\n" +
                    "Received:\n" + actualIndex + "\n");
        }
    }
}
