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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.CombinationEnumeration;
import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.join.exec.base.RangeIndexLookupValue;
import com.espertech.esper.epl.join.exec.base.RangeIndexLookupValueEquals;
import com.espertech.esper.epl.join.exec.base.RangeIndexLookupValueRange;
import com.espertech.esper.epl.join.exec.composite.CompositeIndexLookup;
import com.espertech.esper.epl.join.exec.composite.CompositeIndexLookupFactory;
import com.espertech.esper.epl.join.hint.IndexHint;
import com.espertech.esper.epl.join.hint.IndexHintInstruction;
import com.espertech.esper.epl.join.plan.QueryGraphRangeConsolidateDesc;
import com.espertech.esper.epl.join.plan.QueryGraphRangeEnum;
import com.espertech.esper.epl.join.plan.QueryGraphRangeUtil;
import com.espertech.esper.epl.join.table.*;
import com.espertech.esper.epl.join.util.IndexNameAndDescPair;
import com.espertech.esper.epl.join.util.QueryPlanIndexDescFAF;
import com.espertech.esper.epl.join.util.QueryPlanIndexHook;
import com.espertech.esper.epl.join.util.QueryPlanIndexHookUtil;
import com.espertech.esper.epl.lookup.EventTableIndexRepository;
import com.espertech.esper.epl.lookup.IndexMultiKey;
import com.espertech.esper.epl.lookup.IndexedPropDesc;
import com.espertech.esper.epl.virtualdw.VirtualDWView;
import com.espertech.esper.filter.*;
import org.slf4j.Logger;

import java.lang.annotation.Annotation;
import java.util.*;

public class FireAndForgetQueryExec {
    public static Collection<EventBean> snapshot(
            FilterSpecCompiled optionalFilter,
            Annotation[] annotations,
            VirtualDWView virtualDataWindow,
            EventTableIndexRepository indexRepository,
            boolean queryPlanLogging,
            Logger queryPlanLogDestination,
            String objectName,
            AgentInstanceContext agentInstanceContext) {

        if (optionalFilter == null || optionalFilter.getParameters().length == 0) {
            if (virtualDataWindow != null) {
                Pair<IndexMultiKey, EventTable> pair = virtualDataWindow.getFireAndForgetDesc(Collections.<String>emptySet(), Collections.<String>emptySet());
                return virtualDataWindow.getFireAndForgetData(pair.getSecond(), new Object[0], new RangeIndexLookupValue[0], annotations);
            }
            return null;
        }

        // Determine what straight-equals keys and which ranges are available.
        // Widening/Coercion is part of filter spec compile.
        Set<String> keysAvailable = new HashSet<String>();
        Set<String> rangesAvailable = new HashSet<String>();
        if (optionalFilter.getParameters().length == 1) {
            for (FilterSpecParam param : optionalFilter.getParameters()[0]) {
                if (!(param instanceof FilterSpecParamConstant ||
                        param instanceof FilterSpecParamRange ||
                        param instanceof FilterSpecParamIn)) {
                    continue;
                }
                if (param.getFilterOperator() == FilterOperator.EQUAL ||
                        param.getFilterOperator() == FilterOperator.IS ||
                        param.getFilterOperator() == FilterOperator.IN_LIST_OF_VALUES) {
                    keysAvailable.add(param.getLookupable().getExpression());
                } else if (param.getFilterOperator().isRangeOperator() ||
                        param.getFilterOperator().isInvertedRangeOperator() ||
                        param.getFilterOperator().isComparisonOperator()) {
                    rangesAvailable.add(param.getLookupable().getExpression());
                } else if (param.getFilterOperator().isRangeOperator()) {
                    rangesAvailable.add(param.getLookupable().getExpression());
                }
            }
        }

        // Find an index that matches the needs
        Pair<IndexMultiKey, EventTableAndNamePair> tablePair;
        if (virtualDataWindow != null) {
            Pair<IndexMultiKey, EventTable> tablePairNoName = virtualDataWindow.getFireAndForgetDesc(keysAvailable, rangesAvailable);
            tablePair = new Pair<IndexMultiKey, EventTableAndNamePair>(tablePairNoName.getFirst(), new EventTableAndNamePair(tablePairNoName.getSecond(), null));
        } else {
            IndexHint indexHint = IndexHint.getIndexHint(annotations);
            List<IndexHintInstruction> optionalIndexHintInstructions = null;
            if (indexHint != null) {
                optionalIndexHintInstructions = indexHint.getInstructionsFireAndForget();
            }
            tablePair = indexRepository.findTable(keysAvailable, rangesAvailable, optionalIndexHintInstructions);
        }

        QueryPlanIndexHook hook = QueryPlanIndexHookUtil.getHook(annotations, agentInstanceContext.getStatementContext().getEngineImportService());
        if (queryPlanLogging && (queryPlanLogDestination.isInfoEnabled() || hook != null)) {
            String prefix = "Fire-and-forget from " + objectName + " ";
            String indexName = tablePair != null && tablePair.getSecond() != null ? tablePair.getSecond().getIndexName() : null;
            String indexText = indexName != null ? "index " + indexName + " " : "full table scan ";
            indexText += "(snapshot only, for join see separate query plan)";
            if (tablePair == null) {
                queryPlanLogDestination.info(prefix + indexText);
            } else {
                queryPlanLogDestination.info(prefix + indexText + tablePair.getSecond().getEventTable().toQueryPlan());
            }

            if (hook != null) {
                hook.fireAndForget(new QueryPlanIndexDescFAF(
                    new IndexNameAndDescPair[]{
                        new IndexNameAndDescPair(indexName, tablePair != null ?
                                tablePair.getSecond().getEventTable().getProviderClass().getSimpleName() : null)
                    }));
            }
        }

        if (tablePair == null) {
            return null;    // indicates table scan
        }

        // Compile key sets which contain key index lookup values
        String[] keyIndexProps = IndexedPropDesc.getIndexProperties(tablePair.getFirst().getHashIndexedProps());
        boolean hasKeyWithInClause = false;
        Object[] keyValues = new Object[keyIndexProps.length];
        for (int keyIndex = 0; keyIndex < keyIndexProps.length; keyIndex++) {
            for (FilterSpecParam param : optionalFilter.getParameters()[0]) {
                if (param.getLookupable().getExpression().equals(keyIndexProps[keyIndex])) {
                    if (param.getFilterOperator() == FilterOperator.IN_LIST_OF_VALUES) {
                        Object[] keyValuesList = ((MultiKeyUntyped) param.getFilterValue(null, agentInstanceContext)).getKeys();
                        if (keyValuesList.length == 0) {
                            continue;
                        } else if (keyValuesList.length == 1) {
                            keyValues[keyIndex] = keyValuesList[0];
                        } else {
                            keyValues[keyIndex] = keyValuesList;
                            hasKeyWithInClause = true;
                        }
                    } else {
                        keyValues[keyIndex] = param.getFilterValue(null, agentInstanceContext);
                    }
                    break;
                }
            }
        }

        // Analyze ranges - these may include key lookup value (EQUALS semantics)
        String[] rangeIndexProps = IndexedPropDesc.getIndexProperties(tablePair.getFirst().getRangeIndexedProps());
        RangeIndexLookupValue[] rangeValues;
        if (rangeIndexProps.length > 0) {
            rangeValues = compileRangeLookupValues(rangeIndexProps, optionalFilter.getParameters()[0], agentInstanceContext);
        } else {
            rangeValues = new RangeIndexLookupValue[0];
        }

        EventTable eventTable = tablePair.getSecond().getEventTable();
        IndexMultiKey indexMultiKey = tablePair.getFirst();

        // table lookup without in-clause
        if (!hasKeyWithInClause) {
            return fafTableLookup(virtualDataWindow, indexMultiKey, eventTable, keyValues, rangeValues, annotations);
        }

        // table lookup with in-clause: determine combinations
        Object[][] combinations = new Object[keyIndexProps.length][];
        for (int i = 0; i < keyValues.length; i++) {
            if (keyValues[i] instanceof Object[]) {
                combinations[i] = (Object[]) keyValues[i];
            } else {
                combinations[i] = new Object[]{keyValues[i]};
            }
        }

        // enumerate combinations
        CombinationEnumeration enumeration = new CombinationEnumeration(combinations);
        HashSet<EventBean> events = new HashSet<EventBean>();
        for (; enumeration.hasMoreElements(); ) {
            Object[] keys = enumeration.nextElement();
            Collection<EventBean> result = fafTableLookup(virtualDataWindow, indexMultiKey, eventTable, keys, rangeValues, annotations);
            events.addAll(result);
        }
        return events;
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

    private static RangeIndexLookupValue[] compileRangeLookupValues(String[] rangeIndexProps, FilterSpecParam[] parameters, AgentInstanceContext agentInstanceContext) {
        RangeIndexLookupValue[] result = new RangeIndexLookupValue[rangeIndexProps.length];

        for (int rangeIndex = 0; rangeIndex < rangeIndexProps.length; rangeIndex++) {
            for (FilterSpecParam param : parameters) {
                if (!(param.getLookupable().getExpression().equals(rangeIndexProps[rangeIndex]))) {
                    continue;
                }

                if (param.getFilterOperator() == FilterOperator.EQUAL || param.getFilterOperator() == FilterOperator.IS) {
                    result[rangeIndex] = new RangeIndexLookupValueEquals(param.getFilterValue(null, agentInstanceContext));
                } else if (param.getFilterOperator().isRangeOperator() || param.getFilterOperator().isInvertedRangeOperator()) {
                    QueryGraphRangeEnum opAdd = QueryGraphRangeEnum.mapFrom(param.getFilterOperator());
                    result[rangeIndex] = new RangeIndexLookupValueRange(param.getFilterValue(null, agentInstanceContext), opAdd, true);
                } else if (param.getFilterOperator().isComparisonOperator()) {

                    RangeIndexLookupValue existing = result[rangeIndex];
                    QueryGraphRangeEnum opAdd = QueryGraphRangeEnum.mapFrom(param.getFilterOperator());
                    if (existing == null) {
                        result[rangeIndex] = new RangeIndexLookupValueRange(param.getFilterValue(null, agentInstanceContext), opAdd, true);
                    } else {
                        if (!(existing instanceof RangeIndexLookupValueRange)) {
                            continue;
                        }
                        RangeIndexLookupValueRange existingRange = (RangeIndexLookupValueRange) existing;
                        QueryGraphRangeEnum opExist = existingRange.getOperator();
                        QueryGraphRangeConsolidateDesc desc = QueryGraphRangeUtil.getCanConsolidate(opExist, opAdd);
                        if (desc != null) {
                            DoubleRange doubleRange = getDoubleRange(desc.isReverse(), existing.getValue(), param.getFilterValue(null, agentInstanceContext));
                            result[rangeIndex] = new RangeIndexLookupValueRange(doubleRange, desc.getType(), false);
                        }
                    }
                }
            }
        }
        return result;
    }

    private static DoubleRange getDoubleRange(boolean reverse, Object start, Object end) {
        if (start == null || end == null) {
            return null;
        }
        double startDbl = ((Number) start).doubleValue();
        double endDbl = ((Number) end).doubleValue();
        if (reverse) {
            return new DoubleRange(startDbl, endDbl);
        } else {
            return new DoubleRange(endDbl, startDbl);
        }
    }
}
