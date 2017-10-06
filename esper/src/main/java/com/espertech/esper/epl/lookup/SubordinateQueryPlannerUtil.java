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
package com.espertech.esper.epl.lookup;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprForge;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.join.plan.CoercionDesc;
import com.espertech.esper.epl.join.table.EventTable;
import com.espertech.esper.epl.join.table.EventTableAndNamePair;
import com.espertech.esper.epl.join.table.EventTableUtil;
import com.espertech.esper.epl.join.util.*;
import com.espertech.esper.util.CollectionUtil;
import com.espertech.esper.util.JavaClassHelper;
import org.slf4j.Logger;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SubordinateQueryPlannerUtil {
    private EventBean[] events;

    public static void queryPlanLogOnExpr(boolean queryPlanLogging, Logger queryPlanLog, SubordinateWMatchExprQueryPlanResult strategy, Annotation[] annotations, EngineImportService engineImportService) {
        QueryPlanIndexHook hook = QueryPlanIndexHookUtil.getHook(annotations, engineImportService);
        if (queryPlanLogging && (queryPlanLog.isInfoEnabled() || hook != null)) {
            String prefix = "On-Expr ";
            queryPlanLog.info(prefix + "strategy " + strategy.getFactory().toQueryPlan());
            if (strategy.getIndexDescs() == null) {
                queryPlanLog.info(prefix + "full table scan");
            } else {
                for (int i = 0; i < strategy.getIndexDescs().length; i++) {
                    String indexName = strategy.getIndexDescs()[i].getIndexName();
                    String indexText = indexName != null ? "index " + indexName + " " : "(implicit) (" + i + ")";
                    queryPlanLog.info(prefix + indexText);
                }
            }
            if (hook != null) {
                IndexNameAndDescPair[] pairs = getPairs(strategy.getIndexDescs());
                SubordTableLookupStrategyFactory inner = strategy.getFactory().getOptionalInnerStrategy();
                hook.infraOnExpr(new QueryPlanIndexDescOnExpr(pairs,
                        strategy.getFactory().getClass().getSimpleName(),
                        inner == null ? null : inner.getClass().getSimpleName()));
            }
        }
    }

    public static void queryPlanLogOnSubq(boolean queryPlanLogging, Logger queryPlanLog, SubordinateQueryPlanDesc plan, int subqueryNum, Annotation[] annotations, EngineImportService engineImportService) {
        QueryPlanIndexHook hook = QueryPlanIndexHookUtil.getHook(annotations, engineImportService);
        if (queryPlanLogging && (queryPlanLog.isInfoEnabled() || hook != null)) {
            String prefix = "Subquery " + subqueryNum + " ";
            String strategy = (plan == null || plan.getLookupStrategyFactory() == null) ? "table scan" : plan.getLookupStrategyFactory().toQueryPlan();
            queryPlanLog.info(prefix + "strategy " + strategy);
            if (plan != null) {
                if (plan.getIndexDescs() != null) {
                    for (int i = 0; i < plan.getIndexDescs().length; i++) {
                        String indexName = plan.getIndexDescs()[i].getIndexName();
                        String indexText = indexName != null ? "index " + indexName + " " : "(implicit) ";
                        queryPlanLog.info(prefix + "shared index");
                        queryPlanLog.info(prefix + indexText);
                    }
                }
            }
            if (hook != null) {
                IndexNameAndDescPair[] pairs = plan == null ? new IndexNameAndDescPair[0] : getPairs(plan.getIndexDescs());
                String factory = plan == null ? null : plan.getLookupStrategyFactory().getClass().getSimpleName();
                hook.subquery(new QueryPlanIndexDescSubquery(pairs, subqueryNum, factory));
            }
        }
    }

    private static IndexNameAndDescPair[] getPairs(SubordinateQueryIndexDesc[] indexDescs) {
        if (indexDescs == null) {
            return null;
        }
        IndexNameAndDescPair[] pairs = new IndexNameAndDescPair[indexDescs.length];
        for (int i = 0; i < indexDescs.length; i++) {
            SubordinateQueryIndexDesc index = indexDescs[i];
            pairs[i] = new IndexNameAndDescPair(index.getIndexName(), index.getIndexMultiKey().toQueryPlan());
        }
        return pairs;
    }

    public static SubordinateQueryPlannerIndexPropListPair toListOfHashedAndBtreeProps(String[] hashIndexPropsProvided, Class[] hashIndexCoercionType, String[] rangeIndexPropsProvided, Class[] rangeIndexCoercionType) {
        List<IndexedPropDesc> hashedProps = new ArrayList<IndexedPropDesc>();
        List<IndexedPropDesc> btreeProps = new ArrayList<IndexedPropDesc>();
        for (int i = 0; i < hashIndexPropsProvided.length; i++) {
            hashedProps.add(new IndexedPropDesc(hashIndexPropsProvided[i], hashIndexCoercionType[i]));
        }
        for (int i = 0; i < rangeIndexPropsProvided.length; i++) {
            btreeProps.add(new IndexedPropDesc(rangeIndexPropsProvided[i], rangeIndexCoercionType[i]));
        }
        return new SubordinateQueryPlannerIndexPropListPair(hashedProps, btreeProps);
    }

    /**
     * Given an index with a defined set of hash(equals) and range(btree) props and uniqueness flag,
     * and given a list of indexable properties and accessAccessors for both hash and range,
     * return the ordered keys and coercion information.
     *
     * @param indexMultiKey           index definition
     * @param hashIndexPropsProvided  hash indexable properties
     * @param hashJoinedProps         keys for hash indexable properties
     * @param rangeIndexPropsProvided btree indexable properties
     * @param rangeJoinedProps        keys for btree indexable properties
     * @return ordered set of key information
     */
    public static IndexKeyInfo compileIndexKeyInfo(IndexMultiKey indexMultiKey,
                                                   String[] hashIndexPropsProvided,
                                                   SubordPropHashKey[] hashJoinedProps,
                                                   String[] rangeIndexPropsProvided,
                                                   SubordPropRangeKey[] rangeJoinedProps) {

        // map the order of indexed columns (key) to the key information available
        IndexedPropDesc[] indexedKeyProps = indexMultiKey.getHashIndexedProps();
        boolean isCoerceHash = false;
        SubordPropHashKey[] hashesDesc = new SubordPropHashKey[indexedKeyProps.length];
        Class[] hashPropCoercionTypes = new Class[indexedKeyProps.length];

        for (int i = 0; i < indexedKeyProps.length; i++) {
            String indexField = indexedKeyProps[i].getIndexPropName();
            int index = CollectionUtil.findItem(hashIndexPropsProvided, indexField);
            if (index == -1) {
                throw new IllegalStateException("Could not find index property for lookup '" + indexedKeyProps[i]);
            }
            hashesDesc[i] = hashJoinedProps[index];
            hashPropCoercionTypes[i] = indexedKeyProps[i].getCoercionType();
            ExprForge keyForge = hashesDesc[i].getHashKey().getKeyExpr().getForge();
            ExprEvaluator evaluatorHashkey = keyForge.getExprEvaluator();
            if (evaluatorHashkey != null && JavaClassHelper.getBoxedType(indexedKeyProps[i].getCoercionType()) != JavaClassHelper.getBoxedType(keyForge.getEvaluationType())) {   // we allow null evaluator
                isCoerceHash = true;
            }
        }

        // map the order of range columns (range) to the range information available
        indexedKeyProps = indexMultiKey.getRangeIndexedProps();
        SubordPropRangeKey[] rangesDesc = new SubordPropRangeKey[indexedKeyProps.length];
        Class[] rangePropCoercionTypes = new Class[indexedKeyProps.length];
        boolean isCoerceRange = false;
        for (int i = 0; i < indexedKeyProps.length; i++) {
            String indexField = indexedKeyProps[i].getIndexPropName();
            int index = CollectionUtil.findItem(rangeIndexPropsProvided, indexField);
            if (index == -1) {
                throw new IllegalStateException("Could not find range property for lookup '" + indexedKeyProps[i]);
            }
            rangesDesc[i] = rangeJoinedProps[index];
            rangePropCoercionTypes[i] = rangeJoinedProps[index].getCoercionType();
            if (JavaClassHelper.getBoxedType(indexedKeyProps[i].getCoercionType()) != JavaClassHelper.getBoxedType(rangePropCoercionTypes[i])) {
                isCoerceRange = true;
            }
        }

        return new IndexKeyInfo(Arrays.asList(hashesDesc),
                new CoercionDesc(isCoerceHash, hashPropCoercionTypes), Arrays.asList(rangesDesc), new CoercionDesc(isCoerceRange, rangePropCoercionTypes));
    }

    private static IndexNameAndDescPair[] getTableClassNamePairs(EventTableAndNamePair[] pairs) {
        if (pairs == null) {
            return new IndexNameAndDescPair[0];
        }
        IndexNameAndDescPair[] names = new IndexNameAndDescPair[pairs.length];
        for (int i = 0; i < pairs.length; i++) {
            names[i] = new IndexNameAndDescPair(pairs[i].getIndexName(), pairs[i].getEventTable().getProviderClass().getSimpleName());
        }
        return names;
    }

    public static EventTable[] realizeTables(SubordinateQueryIndexDesc[] indexDescriptors,
                                             EventType eventType,
                                             EventTableIndexRepository indexRepository,
                                             Iterable<EventBean> contents,
                                             AgentInstanceContext agentInstanceContext,
                                             boolean isRecoveringResilient) {
        EventTable[] tables = new EventTable[indexDescriptors.length];
        for (int i = 0; i < tables.length; i++) {
            SubordinateQueryIndexDesc desc = indexDescriptors[i];
            EventTable table = indexRepository.getIndexByDesc(desc.getIndexMultiKey());
            if (table == null) {
                table = EventTableUtil.buildIndex(agentInstanceContext, 0, desc.getQueryPlanIndexItem(), eventType, true, desc.getIndexMultiKey().isUnique(), desc.getIndexName(), null, false);

                // fill table since its new
                if (!isRecoveringResilient) {
                    EventBean[] events = new EventBean[1];
                    for (EventBean prefilledEvent : contents) {
                        events[0] = prefilledEvent;
                        table.add(events, agentInstanceContext);
                    }
                }

                indexRepository.addIndex(desc.getIndexMultiKey(), new EventTableIndexRepositoryEntry(null, table));
            }
            tables[i] = table;
        }
        return tables;
    }

    public static void addIndexMetaAndRef(SubordinateQueryIndexDesc[] indexDescs, EventTableIndexMetadata repo, String statementName) throws ExprValidationException {
        for (SubordinateQueryIndexDesc desc : indexDescs) {
            if (desc.getIndexName() != null) {
                repo.addIndexReference(desc.getIndexName(), statementName);
            } else {
                repo.addIndexNonExplicit(desc.getIndexMultiKey(), statementName, desc.getQueryPlanIndexItem());
                repo.addIndexReference(desc.getIndexMultiKey(), statementName);
            }
        }
    }
}
