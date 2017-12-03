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
package com.espertech.esper.epl.fafquery;

import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.CombinationEnumeration;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.index.quadtree.EventTableQuadTree;
import com.espertech.esper.epl.index.service.EventAdvancedIndexProvisionDesc;
import com.espertech.esper.epl.join.exec.base.RangeIndexLookupValue;
import com.espertech.esper.epl.join.exec.base.RangeIndexLookupValueRange;
import com.espertech.esper.epl.join.exec.composite.CompositeIndexLookup;
import com.espertech.esper.epl.join.exec.composite.CompositeIndexLookupFactory;
import com.espertech.esper.epl.join.hint.IndexHint;
import com.espertech.esper.epl.join.hint.IndexHintInstruction;
import com.espertech.esper.epl.join.plan.*;
import com.espertech.esper.epl.join.table.*;
import com.espertech.esper.epl.join.util.IndexNameAndDescPair;
import com.espertech.esper.epl.join.util.QueryPlanIndexDescFAF;
import com.espertech.esper.epl.join.util.QueryPlanIndexHook;
import com.espertech.esper.epl.join.util.QueryPlanIndexHookUtil;
import com.espertech.esper.epl.lookup.*;
import com.espertech.esper.epl.virtualdw.VirtualDWView;
import com.espertech.esper.filterspec.DoubleRange;
import com.espertech.esper.filterspec.Range;
import com.espertech.esper.filterspec.StringRange;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.util.JavaClassHelper;
import com.espertech.esper.util.NullableObject;
import org.slf4j.Logger;

import java.lang.annotation.Annotation;
import java.util.*;

public class FireAndForgetQueryExec {
    public static Collection<EventBean> snapshot(QueryGraph queryGraph,
                                                 Annotation[] annotations,
                                                 VirtualDWView virtualDataWindow,
                                                 EventTableIndexRepository indexRepository,
                                                 boolean queryPlanLogging,
                                                 Logger queryPlanLogDestination,
                                                 String objectName,
                                                 AgentInstanceContext agentInstanceContext) {

        QueryGraphValue queryGraphValue = queryGraph == null ? null : queryGraph.getGraphValue(QueryGraph.SELF_STREAM, 0);
        if (queryGraphValue == null || queryGraphValue.getItems().isEmpty()) {
            if (virtualDataWindow != null) {
                Pair<IndexMultiKey, EventTable> pair = virtualDataWindow.getFireAndForgetDesc(Collections.<String>emptySet(), Collections.<String>emptySet());
                return virtualDataWindow.getFireAndForgetData(pair.getSecond(), CollectionUtil.OBJECTARRAY_EMPTY, new RangeIndexLookupValue[0], annotations);
            }
            return null;
        }

        // determine custom index
        NullableObject<Collection<EventBean>> customResult = snapshotCustomIndex(queryGraphValue, indexRepository, annotations, agentInstanceContext, queryPlanLogging, queryPlanLogDestination, objectName);
        if (customResult != null) {
            return customResult.getObject();
        }

        // determine lookup based on hash-keys and ranges
        QueryGraphValuePairHashKeyIndex keysAvailable = queryGraphValue.getHashKeyProps();
        Set<String> keyNamesAvailable = keysAvailable.getIndexed().length == 0 ? Collections.<String>emptySet() : new HashSet<>(Arrays.asList(keysAvailable.getIndexed()));
        QueryGraphValuePairRangeIndex rangesAvailable = queryGraphValue.getRangeProps();
        Set<String> rangeNamesAvailable = rangesAvailable.getIndexed().length == 0 ? Collections.<String>emptySet() : new HashSet<>(Arrays.asList(rangesAvailable.getIndexed()));
        Pair<IndexMultiKey, EventTableAndNamePair> tablePair;

        // find index that matches the needs
        tablePair = findIndex(keyNamesAvailable, rangeNamesAvailable, indexRepository, virtualDataWindow, annotations);

        // regular index lookup
        if (tablePair != null) {
            return snapshotIndex(keysAvailable, rangesAvailable, tablePair, virtualDataWindow, annotations, agentInstanceContext, queryPlanLogging, queryPlanLogDestination, objectName);
        }

        // in-keyword lookup
        NullableObject<Collection<EventBean>> inkwResult = snapshotInKeyword(queryGraphValue, indexRepository, virtualDataWindow, annotations, agentInstanceContext, queryPlanLogging, queryPlanLogDestination, objectName);
        if (inkwResult != null) {
            return inkwResult.getObject();
        }

        queryPlanReportTableScan(annotations, agentInstanceContext, queryPlanLogging, queryPlanLogDestination, objectName);
        return null;
    }

    private static Pair<IndexMultiKey, EventTableAndNamePair> findIndex(Set<String> keyNamesAvailable, Set<String> rangeNamesAvailable, EventTableIndexRepository indexRepository, VirtualDWView virtualDataWindow, Annotation[] annotations) {
        if (virtualDataWindow != null) {
            Pair<IndexMultiKey, EventTable> tablePairNoName = virtualDataWindow.getFireAndForgetDesc(keyNamesAvailable, rangeNamesAvailable);
            return new Pair<>(tablePairNoName.getFirst(), new EventTableAndNamePair(tablePairNoName.getSecond(), null));
        }
        IndexHint indexHint = IndexHint.getIndexHint(annotations);
        List<IndexHintInstruction> optionalIndexHintInstructions = indexHint != null ? indexHint.getInstructionsFireAndForget() : null;
        return indexRepository.findTable(keyNamesAvailable, rangeNamesAvailable, optionalIndexHintInstructions);
    }

    private static NullableObject<Collection<EventBean>> snapshotInKeyword(QueryGraphValue queryGraphValue, EventTableIndexRepository indexRepository, VirtualDWView virtualDataWindow, Annotation[] annotations, AgentInstanceContext agentInstanceContext, boolean queryPlanLogging, Logger queryPlanLogDestination, String objectName) {
        QueryGraphValuePairInKWSingleIdx inkwSingles = queryGraphValue.getInKeywordSingles();
        if (inkwSingles.getIndexed().length == 0) {
            return null;
        }

        Pair<IndexMultiKey, EventTableAndNamePair> tablePair = findIndex(new HashSet<>(Arrays.asList(inkwSingles.getIndexed())), Collections.<String>emptySet(), indexRepository, virtualDataWindow, annotations);
        if (tablePair == null) {
            return null;
        }

        queryPlanReport(tablePair.getSecond().getIndexName(), tablePair.getSecond().getEventTable(), annotations, agentInstanceContext, queryPlanLogging, queryPlanLogDestination, objectName);

        // table lookup with in-clause: determine combinations
        IndexedPropDesc[] tableHashProps = tablePair.getFirst().getHashIndexedProps();
        Object[][] combinations = new Object[tableHashProps.length][];
        for (int tableHashPropNum = 0; tableHashPropNum < tableHashProps.length; tableHashPropNum++) {
            for (int i = 0; i < inkwSingles.getIndexed().length; i++) {
                if (inkwSingles.getIndexed()[i].equals(tableHashProps[tableHashPropNum].getIndexPropName())) {
                    QueryGraphValueEntryInKeywordSingleIdx keysExpressions = inkwSingles.getKey().get(i);
                    Object[] values = new Object[keysExpressions.getKeyExprs().length];
                    combinations[tableHashPropNum] = values;
                    for (int j = 0; j < keysExpressions.getKeyExprs().length; j++) {
                        values[j] = keysExpressions.getKeyExprs()[j].getForge().getExprEvaluator().evaluate(null, true, agentInstanceContext);
                    }
                }
            }
        }

        // enumerate combinations
        CombinationEnumeration enumeration = new CombinationEnumeration(combinations);
        HashSet<EventBean> events = new HashSet<EventBean>();
        for (; enumeration.hasMoreElements(); ) {
            Object[] keys = enumeration.nextElement();
            Collection<EventBean> result = fafTableLookup(virtualDataWindow, tablePair.getFirst(), tablePair.getSecond().getEventTable(), keys, null, annotations);
            events.addAll(result);
        }
        return new NullableObject<Collection<EventBean>>(events);
    }

    private static Collection<EventBean> snapshotIndex(QueryGraphValuePairHashKeyIndex keysAvailable, QueryGraphValuePairRangeIndex rangesAvailable, Pair<IndexMultiKey, EventTableAndNamePair> tablePair, VirtualDWView virtualDataWindow, Annotation[] annotations, AgentInstanceContext agentInstanceContext, boolean queryPlanLogging, Logger queryPlanLogDestination, String objectName) {

        // report plan
        queryPlanReport(tablePair.getSecond().getIndexName(), tablePair.getSecond().getEventTable(), annotations, agentInstanceContext, queryPlanLogging, queryPlanLogDestination, objectName);

        // compile hash lookup values
        IndexedPropDesc[] tableHashProps = tablePair.getFirst().getHashIndexedProps();
        Object[] keyValues = new Object[tableHashProps.length];
        for (int tableHashPropNum = 0; tableHashPropNum < tableHashProps.length; tableHashPropNum++) {
            IndexedPropDesc tableHashProp = tableHashProps[tableHashPropNum];
            for (int i = 0; i < keysAvailable.getIndexed().length; i++) {
                if (keysAvailable.getIndexed()[i].equals(tableHashProp.getIndexPropName())) {
                    QueryGraphValueEntryHashKeyed key = keysAvailable.getKeys().get(i);
                    Object value = key.getKeyExpr().getForge().getExprEvaluator().evaluate(null, true, agentInstanceContext);
                    if (value != null) {
                        value = mayCoerceNonNull(value, tableHashProp.getCoercionType());
                        keyValues[tableHashPropNum] = value;
                    }
                }
            }
        }

        // compile range lookup values
        IndexedPropDesc[] tableRangeProps = tablePair.getFirst().getRangeIndexedProps();
        RangeIndexLookupValue[] rangeValues = new RangeIndexLookupValue[tableRangeProps.length];
        for (int tableRangePropNum = 0; tableRangePropNum < tableRangeProps.length; tableRangePropNum++) {
            IndexedPropDesc tableRangeProp = tableRangeProps[tableRangePropNum];
            for (int i = 0; i < rangesAvailable.getIndexed().length; i++) {
                if (rangesAvailable.getIndexed()[i].equals(tableRangeProp.getIndexPropName())) {
                    QueryGraphValueEntryRange range = rangesAvailable.getKeys().get(i);
                    if (range instanceof QueryGraphValueEntryRangeIn) {
                        QueryGraphValueEntryRangeIn between = (QueryGraphValueEntryRangeIn) range;
                        Object start = between.getExprStart().getForge().getExprEvaluator().evaluate(null, true, agentInstanceContext);
                        Object end = between.getExprEnd().getForge().getExprEvaluator().evaluate(null, true, agentInstanceContext);
                        Range rangeValue;
                        if (JavaClassHelper.isNumeric(tableRangeProp.getCoercionType())) {
                            Double startDouble = null;
                            if (start != null) {
                                startDouble = ((Number) start).doubleValue();
                            }
                            Double endDouble = null;
                            if (end != null) {
                                endDouble = ((Number) end).doubleValue();
                            }
                            rangeValue = new DoubleRange(startDouble, endDouble);
                        } else {
                            rangeValue = new StringRange(start == null ? null : start.toString(), end == null ? null : end.toString());
                        }
                        rangeValues[tableRangePropNum] = new RangeIndexLookupValueRange(rangeValue, between.getType(), between.isAllowRangeReversal());
                    } else {
                        QueryGraphValueEntryRangeRelOp relOp = (QueryGraphValueEntryRangeRelOp) range;
                        Object value = relOp.getExpression().getForge().getExprEvaluator().evaluate(null, true, agentInstanceContext);
                        if (value != null) {
                            value = mayCoerceNonNull(value, tableRangeProp.getCoercionType());
                        }
                        rangeValues[tableRangePropNum] = new RangeIndexLookupValueRange(value, relOp.getType(), true);
                    }
                }
            }
        }

        // perform lookup
        return fafTableLookup(virtualDataWindow, tablePair.getFirst(), tablePair.getSecond().getEventTable(), keyValues, rangeValues, annotations);
    }

    private static Object mayCoerceNonNull(Object value, Class coercionType) {
        if (value.getClass() == coercionType) {
            return value;
        }
        if (value instanceof Number) {
            return JavaClassHelper.coerceBoxed((Number) value, coercionType);
        }
        return value;
    }

    private static NullableObject<Collection<EventBean>> snapshotCustomIndex(QueryGraphValue queryGraphValue, EventTableIndexRepository indexRepository, Annotation[] annotations, AgentInstanceContext agentInstanceContext, boolean queryPlanLogging, Logger queryPlanLogDestination, String objectName) {

        EventTable table = null;
        String indexName = null;
        QueryGraphValueEntryCustomOperation values = null;

        // find matching index
        boolean found = false;
        for (QueryGraphValueDesc valueDesc : queryGraphValue.getItems()) {
            if (valueDesc.getEntry() instanceof QueryGraphValueEntryCustom) {
                QueryGraphValueEntryCustom customIndex = (QueryGraphValueEntryCustom) valueDesc.getEntry();

                for (Map.Entry<IndexMultiKey, EventTableIndexRepositoryEntry> entry : indexRepository.getTableIndexesRefCount().entrySet()) {
                    if (entry.getKey().getAdvancedIndexDesc() == null) {
                        continue;
                    }
                    EventTableIndexMetadataEntry metadata = indexRepository.getEventTableIndexMetadata().getIndexes().get(entry.getKey());
                    if (metadata == null || metadata.getExplicitIndexNameIfExplicit() == null) {
                        continue;
                    }
                    EventAdvancedIndexProvisionDesc provision = metadata.getQueryPlanIndexItem().getAdvancedIndexProvisionDesc();
                    if (provision == null) {
                        continue;
                    }
                    for (Map.Entry<QueryGraphValueEntryCustomKey, QueryGraphValueEntryCustomOperation> op : customIndex.getOperations().entrySet()) {
                        if (!provision.getFactory().providesIndexForOperation(op.getKey().getOperationName(), op.getValue().getPositionalExpressions())) {
                            continue;
                        }
                        if (ExprNodeUtilityCore.deepEquals(entry.getKey().getAdvancedIndexDesc().getIndexedExpressions(), op.getKey().getExprNodes(), true)) {
                            values = op.getValue();
                            table = entry.getValue().getTable();
                            indexName = metadata.getExplicitIndexNameIfExplicit();
                            found = true;
                            break;
                        }
                    }

                    if (found) {
                        break;
                    }
                }
            }
            if (found) {
                break;
            }
        }

        if (table == null) {
            return null;
        }

        // report
        queryPlanReport(indexName, table, annotations, agentInstanceContext, queryPlanLogging, queryPlanLogDestination, objectName);

        // execute
        EventTableQuadTree index = (EventTableQuadTree) table;
        double x = eval(values.getPositionalExpressions().get(0).getForge().getExprEvaluator(), agentInstanceContext, "x");
        double y = eval(values.getPositionalExpressions().get(1).getForge().getExprEvaluator(), agentInstanceContext, "y");
        double width = eval(values.getPositionalExpressions().get(2).getForge().getExprEvaluator(), agentInstanceContext, "width");
        double height = eval(values.getPositionalExpressions().get(3).getForge().getExprEvaluator(), agentInstanceContext, "height");
        return new NullableObject<>(index.queryRange(x, y, width, height));
    }

    public String toQueryPlan() {
        return this.getClass().getSimpleName();
    }

    private static Collection<EventBean> fafTableLookup(VirtualDWView virtualDataWindow, IndexMultiKey indexMultiKey, EventTable eventTable, Object[] keyValues, RangeIndexLookupValue[] rangeValues, Annotation[] annotations) {
        if (virtualDataWindow != null) {
            return virtualDataWindow.getFireAndForgetData(eventTable, keyValues, rangeValues, annotations);
        }

        Set<EventBean> result;
        if (indexMultiKey.getHashIndexedProps().length > 0 && indexMultiKey.getRangeIndexedProps().length == 0) {
            if (indexMultiKey.getHashIndexedProps().length == 1) {
                PropertyIndexedEventTableSingle table = (PropertyIndexedEventTableSingle) eventTable;
                result = table.lookup(keyValues[0]);
            } else {
                PropertyIndexedEventTable table = (PropertyIndexedEventTable) eventTable;
                result = table.lookup(keyValues);
            }
        } else if (indexMultiKey.getHashIndexedProps().length == 0 && indexMultiKey.getRangeIndexedProps().length == 1) {
            PropertySortedEventTable table = (PropertySortedEventTable) eventTable;
            result = table.lookupConstants(rangeValues[0]);
        } else {
            PropertyCompositeEventTable table = (PropertyCompositeEventTable) eventTable;
            Class[] rangeCoercion = table.getOptRangeCoercedTypes();
            CompositeIndexLookup lookup = CompositeIndexLookupFactory.make(keyValues, rangeValues, rangeCoercion);
            result = new HashSet<EventBean>();
            lookup.lookup(table.getIndex(), result, table.getPostProcessor());
        }
        if (result != null) {
            return result;
        }
        return Collections.EMPTY_LIST;
    }

    private static double eval(ExprEvaluator eval, ExprEvaluatorContext context, String name) {
        Number number = (Number) eval.evaluate(null, true, context);
        if (number == null) {
            throw new EPException("Invalid null value for '" + name + "'");
        }
        return number.doubleValue();
    }

    private static void queryPlanReportTableScan(Annotation[] annotations, AgentInstanceContext agentInstanceContext, boolean queryPlanLogging, Logger queryPlanLogDestination, String objectName) {
        queryPlanReport(null, null, annotations, agentInstanceContext, queryPlanLogging, queryPlanLogDestination, objectName);
    }

    private static void queryPlanReport(String indexNameOrNull, EventTable eventTableOrNull, Annotation[] annotations, AgentInstanceContext agentInstanceContext, boolean queryPlanLogging, Logger queryPlanLogDestination, String objectName) {
        QueryPlanIndexHook hook = QueryPlanIndexHookUtil.getHook(annotations, agentInstanceContext.getEngineImportService());
        if (queryPlanLogging && (queryPlanLogDestination.isInfoEnabled() || hook != null)) {
            String prefix = "Fire-and-forget from " + objectName + " ";
            String indexText = indexNameOrNull != null ? "index " + indexNameOrNull + " " : "full table scan ";
            indexText += "(snapshot only, for join see separate query plan) ";
            if (eventTableOrNull == null) {
                queryPlanLogDestination.info(prefix + indexText);
            } else {
                queryPlanLogDestination.info(prefix + indexText + eventTableOrNull.toQueryPlan());
            }

            if (hook != null) {
                hook.fireAndForget(new QueryPlanIndexDescFAF(new IndexNameAndDescPair[]{new IndexNameAndDescPair(indexNameOrNull, eventTableOrNull != null ? eventTableOrNull.getProviderClass().getSimpleName() : null)}));
            }
        }
    }
}
