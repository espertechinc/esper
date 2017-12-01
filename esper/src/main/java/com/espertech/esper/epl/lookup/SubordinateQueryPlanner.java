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

import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprNodeUtilityCore;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.codegen.ExprNodeCompiler;
import com.espertech.esper.epl.index.quadtree.SubordTableLookupStrategyFactoryQuadTree;
import com.espertech.esper.epl.index.service.EventAdvancedIndexProvisionDesc;
import com.espertech.esper.epl.join.hint.ExcludePlanHint;
import com.espertech.esper.epl.join.hint.IndexHint;
import com.espertech.esper.epl.join.hint.IndexHintInstruction;
import com.espertech.esper.epl.join.plan.*;

import java.lang.annotation.Annotation;
import java.util.*;

public class SubordinateQueryPlanner {
    public static SubordinateWMatchExprQueryPlanResult planOnExpression(
            ExprNode joinExpr,
            EventType filterEventType,
            IndexHint optionalIndexHint,
            boolean isIndexShare,
            int subqueryNumber,
            ExcludePlanHint excludePlanHint,
            boolean isVirtualDataWindow,
            EventTableIndexMetadata indexMetadata,
            EventType eventTypeIndexed,
            Set<String> optionalUniqueKeyProps,
            boolean onlyUseExistingIndexes,
            String statementName,
            int statementId,
            Annotation[] annotations,
            EngineImportService engineImportService) {
        EventType[] allStreamsZeroIndexed = new EventType[]{eventTypeIndexed, filterEventType};
        EventType[] outerStreams = new EventType[]{filterEventType};
        SubordPropPlan joinedPropPlan = QueryPlanIndexBuilder.getJoinProps(joinExpr, 1, allStreamsZeroIndexed, excludePlanHint);

        // No join expression means all
        if (joinExpr == null && !isVirtualDataWindow) {
            return new SubordinateWMatchExprQueryPlanResult(new SubordWMatchExprLookupStrategyFactoryAllUnfiltered(), null);
        }

        SubordinateQueryPlanDesc queryPlanDesc = planSubquery(outerStreams, joinedPropPlan, true, false, optionalIndexHint, isIndexShare, subqueryNumber,
                isVirtualDataWindow, indexMetadata, optionalUniqueKeyProps, onlyUseExistingIndexes, statementName, statementId, annotations);
        ExprEvaluator joinEvaluator = joinExpr == null ? null : ExprNodeCompiler.allocateEvaluator(joinExpr.getForge(), engineImportService, SubordinateQueryPlanner.class, false, statementName);

        if (queryPlanDesc == null) {
            return new SubordinateWMatchExprQueryPlanResult(new SubordWMatchExprLookupStrategyFactoryAllFiltered(joinEvaluator), null);
        }

        if (joinExpr == null) {   // it can be null when using virtual data window
            return new SubordinateWMatchExprQueryPlanResult(
                    new SubordWMatchExprLookupStrategyFactoryIndexedUnfiltered(queryPlanDesc.getLookupStrategyFactory()), queryPlanDesc.getIndexDescs());
        } else {
            return new SubordinateWMatchExprQueryPlanResult(
                    new SubordWMatchExprLookupStrategyFactoryIndexedFiltered(joinEvaluator, queryPlanDesc.getLookupStrategyFactory()), queryPlanDesc.getIndexDescs());
        }
    }

    public static SubordinateQueryPlanDesc planSubquery(EventType[] outerStreams,
                                                        SubordPropPlan joinDesc,
                                                        boolean isNWOnTrigger,
                                                        boolean forceTableScan,
                                                        IndexHint optionalIndexHint,
                                                        boolean indexShare,
                                                        int subqueryNumber,
                                                        boolean isVirtualDataWindow,
                                                        EventTableIndexMetadata indexMetadata,
                                                        Set<String> optionalUniqueKeyProps,
                                                        boolean onlyUseExistingIndexes,
                                                        String statementName,
                                                        int statementId,
                                                        Annotation[] annotations) {
        if (isVirtualDataWindow) {
            SubordinateQueryPlannerIndexPropDesc indexProps = getIndexPropDesc(joinDesc.getHashProps(), joinDesc.getRangeProps());
            SubordTableLookupStrategyFactoryVDW lookupStrategyFactory = new SubordTableLookupStrategyFactoryVDW(statementName, statementId, annotations,
                    outerStreams,
                    Arrays.asList(indexProps.getHashJoinedProps()),
                    new CoercionDesc(false, indexProps.getHashIndexCoercionType()),
                    Arrays.asList(indexProps.getRangeJoinedProps()),
                    new CoercionDesc(false, indexProps.getRangeIndexCoercionType()),
                    isNWOnTrigger,
                    joinDesc, forceTableScan, indexProps.getListPair());
            return new SubordinateQueryPlanDesc(lookupStrategyFactory, null);
        }

        if (joinDesc.getCustomIndexOps() != null && !joinDesc.getCustomIndexOps().isEmpty()) {
            for (Map.Entry<QueryGraphValueEntryCustomKey, QueryGraphValueEntryCustomOperation> op : joinDesc.getCustomIndexOps().entrySet()) {
                for (Map.Entry<IndexMultiKey, EventTableIndexMetadataEntry> index : indexMetadata.getIndexes().entrySet()) {
                    if (isCustomIndexMatch(index, op)) {
                        EventAdvancedIndexProvisionDesc provisionDesc = index.getValue().getQueryPlanIndexItem().getAdvancedIndexProvisionDesc();
                        SubordTableLookupStrategyFactoryQuadTree lookupStrategyFactory = provisionDesc.getFactory().getSubordinateLookupStrategy(op.getKey().getOperationName(), op.getValue().getPositionalExpressions(), isNWOnTrigger, outerStreams.length);
                        SubordinateQueryIndexDesc indexDesc = new SubordinateQueryIndexDesc(null, index.getValue().getOptionalIndexName(), index.getKey(), null);
                        return new SubordinateQueryPlanDesc(lookupStrategyFactory, new SubordinateQueryIndexDesc[] {indexDesc});
                    }
                }
            }
        }

        List<SubordPropHashKey> hashKeys = Collections.emptyList();
        CoercionDesc hashKeyCoercionTypes = null;
        List<SubordPropRangeKey> rangeKeys = Collections.emptyList();
        CoercionDesc rangeKeyCoercionTypes = null;
        ExprNode[] inKeywordSingleIdxKeys = null;
        ExprNode inKeywordMultiIdxKey = null;

        SubordinateQueryIndexDesc[] indexDescs;
        if (joinDesc.getInKeywordSingleIndex() != null) {
            SubordPropInKeywordSingleIndex single = joinDesc.getInKeywordSingleIndex();
            SubordPropHashKey keyInfo = new SubordPropHashKey(new QueryGraphValueEntryHashKeyedExpr(single.getExpressions()[0], false), null, single.getCoercionType());
            SubordinateQueryIndexDesc indexDesc = findOrSuggestIndex(
                    Collections.singletonMap(single.getIndexedProp(), keyInfo),
                    Collections.<String, SubordPropRangeKey>emptyMap(), optionalIndexHint, indexShare, subqueryNumber,
                    indexMetadata, optionalUniqueKeyProps, onlyUseExistingIndexes);
            if (indexDesc == null) {
                return null;
            }
            SubordinateQueryIndexDesc desc = new SubordinateQueryIndexDesc(indexDesc.getOptionalIndexKeyInfo(), indexDesc.getIndexName(), indexDesc.getIndexMultiKey(), indexDesc.getQueryPlanIndexItem());
            indexDescs = new SubordinateQueryIndexDesc[]{desc};
            inKeywordSingleIdxKeys = single.getExpressions();
        } else if (joinDesc.getInKeywordMultiIndex() != null) {
            SubordPropInKeywordMultiIndex multi = joinDesc.getInKeywordMultiIndex();

            indexDescs = new SubordinateQueryIndexDesc[multi.getIndexedProp().length];
            for (int i = 0; i < multi.getIndexedProp().length; i++) {
                SubordPropHashKey keyInfo = new SubordPropHashKey(new QueryGraphValueEntryHashKeyedExpr(multi.getExpression(), false), null, multi.getCoercionType());
                SubordinateQueryIndexDesc indexDesc = findOrSuggestIndex(
                        Collections.singletonMap(multi.getIndexedProp()[i], keyInfo),
                        Collections.<String, SubordPropRangeKey>emptyMap(), optionalIndexHint, indexShare, subqueryNumber,
                        indexMetadata, optionalUniqueKeyProps, onlyUseExistingIndexes);
                if (indexDesc == null) {
                    return null;
                }
                indexDescs[i] = indexDesc;
            }
            inKeywordMultiIdxKey = multi.getExpression();
        } else {
            SubordinateQueryIndexDesc indexDesc = findOrSuggestIndex(joinDesc.getHashProps(),
                    joinDesc.getRangeProps(), optionalIndexHint, false, subqueryNumber,
                    indexMetadata, optionalUniqueKeyProps, onlyUseExistingIndexes);
            if (indexDesc == null) {
                return null;
            }
            IndexKeyInfo indexKeyInfo = indexDesc.getOptionalIndexKeyInfo();
            hashKeys = indexKeyInfo.getOrderedHashDesc();
            hashKeyCoercionTypes = indexKeyInfo.getOrderedKeyCoercionTypes();
            rangeKeys = indexKeyInfo.getOrderedRangeDesc();
            rangeKeyCoercionTypes = indexKeyInfo.getOrderedRangeCoercionTypes();
            SubordinateQueryIndexDesc desc = new SubordinateQueryIndexDesc(indexDesc.getOptionalIndexKeyInfo(), indexDesc.getIndexName(), indexDesc.getIndexMultiKey(), indexDesc.getQueryPlanIndexItem());
            indexDescs = new SubordinateQueryIndexDesc[]{desc};
        }

        if (forceTableScan) {
            return null;
        }

        SubordTableLookupStrategyFactory lookupStrategyFactory = SubordinateTableLookupStrategyUtil.getLookupStrategy(outerStreams,
                hashKeys, hashKeyCoercionTypes, rangeKeys, rangeKeyCoercionTypes, inKeywordSingleIdxKeys, inKeywordMultiIdxKey, isNWOnTrigger);
        return new SubordinateQueryPlanDesc(lookupStrategyFactory, indexDescs);
    }

    private static boolean isCustomIndexMatch(Map.Entry<IndexMultiKey, EventTableIndexMetadataEntry> index, Map.Entry<QueryGraphValueEntryCustomKey, QueryGraphValueEntryCustomOperation> op) {
        if (index.getValue().getExplicitIndexNameIfExplicit() == null || index.getValue().getQueryPlanIndexItem() == null) {
            return false;
        }
        EventAdvancedIndexProvisionDesc provision = index.getValue().getQueryPlanIndexItem().getAdvancedIndexProvisionDesc();
        if (provision == null) {
            return false;
        }
        if (!provision.getFactory().providesIndexForOperation(op.getKey().getOperationName(), op.getValue().getPositionalExpressions())) {
            return false;
        }
        return ExprNodeUtilityCore.deepEquals(index.getKey().getAdvancedIndexDesc().getIndexedExpressions(), op.getKey().getExprNodes(), true);
    }

    private static SubordinateQueryIndexDesc findOrSuggestIndex(
            Map<String, SubordPropHashKey> hashProps,
            Map<String, SubordPropRangeKey> rangeProps,
            IndexHint optionalIndexHint,
            boolean isIndexShare,
            int subqueryNumber,
            EventTableIndexMetadata indexMetadata,
            Set<String> optionalUniqueKeyProps,
            boolean onlyUseExistingIndexes) {

        SubordinateQueryPlannerIndexPropDesc indexProps = getIndexPropDesc(hashProps, rangeProps);
        SubordinateQueryPlannerIndexPropListPair hashedAndBtreeProps = indexProps.getListPair();

        // Get or create the table for this index (exact match or property names, type of index and coercion type is expected)
        IndexKeyInfo indexKeyInfo;   // how needs all of IndexKeyInfo+QueryPlanIndexItem+IndexMultiKey
        IndexMultiKey indexMultiKey;
        String indexName = null;
        QueryPlanIndexItem planIndexItem = null;

        if (hashedAndBtreeProps.getHashedProps().isEmpty() && hashedAndBtreeProps.getBtreeProps().isEmpty()) {
            return null;
        }

        Pair<IndexMultiKey, String> existing = null;
        Pair<QueryPlanIndexItem, IndexMultiKey> planned = null;

        // consider index hints
        List<IndexHintInstruction> optionalIndexHintInstructions = null;
        if (optionalIndexHint != null) {
            optionalIndexHintInstructions = optionalIndexHint.getInstructionsSubquery(subqueryNumber);
        }

        IndexMultiKey indexFoundPair = EventTableIndexUtil.findIndexConsiderTyping(indexMetadata.getIndexes(), hashedAndBtreeProps.getHashedProps(), hashedAndBtreeProps.getBtreeProps(), optionalIndexHintInstructions);
        if (indexFoundPair != null) {
            EventTableIndexMetadataEntry hintIndex = indexMetadata.getIndexes().get(indexFoundPair);
            existing = new Pair<IndexMultiKey, String>(indexFoundPair, hintIndex.getOptionalIndexName());
        }

        // nothing found: plan one
        if (existing == null && !onlyUseExistingIndexes) {
            // not found, see if the item is declared unique
            List<IndexedPropDesc> proposedHashedProps = hashedAndBtreeProps.getHashedProps();
            List<IndexedPropDesc> proposedBtreeProps = hashedAndBtreeProps.getBtreeProps();

            // match against unique-key properties when suggesting an index
            boolean unique = false;
            boolean coerce = !isIndexShare;
            if (optionalUniqueKeyProps != null && !optionalUniqueKeyProps.isEmpty()) {
                List<IndexedPropDesc> newHashProps = new ArrayList<IndexedPropDesc>();
                for (String uniqueKey : optionalUniqueKeyProps) {
                    boolean found = false;
                    for (IndexedPropDesc hashProp : hashedAndBtreeProps.getHashedProps()) {
                        if (hashProp.getIndexPropName().equals(uniqueKey)) {
                            newHashProps.add(hashProp);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        newHashProps = null;
                        break;
                    }
                }
                if (newHashProps != null) {
                    proposedHashedProps = newHashProps;
                    proposedBtreeProps = Collections.emptyList();
                    unique = true;
                    coerce = false;
                }
            }

            planned = planIndex(unique, proposedHashedProps, proposedBtreeProps, coerce);
        }

        // compile index information
        if (existing == null && planned == null) {
            return null;
        }
        // handle existing
        if (existing != null) {
            indexKeyInfo = SubordinateQueryPlannerUtil.compileIndexKeyInfo(existing.getFirst(),
                    indexProps.getHashIndexPropsProvided(), indexProps.getHashJoinedProps(),
                    indexProps.getRangeIndexPropsProvided(), indexProps.getRangeJoinedProps());
            indexName = existing.getSecond();
            indexMultiKey = existing.getFirst();
        } else {
            // handle planned
            indexKeyInfo = SubordinateQueryPlannerUtil.compileIndexKeyInfo(planned.getSecond(),
                    indexProps.getHashIndexPropsProvided(), indexProps.getHashJoinedProps(),
                    indexProps.getRangeIndexPropsProvided(), indexProps.getRangeJoinedProps());
            indexMultiKey = planned.getSecond();
            planIndexItem = planned.getFirst();
        }

        return new SubordinateQueryIndexDesc(indexKeyInfo, indexName, indexMultiKey, planIndexItem);
    }

    private static SubordinateQueryPlannerIndexPropDesc getIndexPropDesc(Map<String, SubordPropHashKey> hashProps, Map<String, SubordPropRangeKey> rangeProps) {

        // hash property names and types
        String[] hashIndexPropsProvided = new String[hashProps.size()];
        Class[] hashIndexCoercionType = new Class[hashProps.size()];
        SubordPropHashKey[] hashJoinedProps = new SubordPropHashKey[hashProps.size()];
        int count = 0;
        for (Map.Entry<String, SubordPropHashKey> entry : hashProps.entrySet()) {
            hashIndexPropsProvided[count] = entry.getKey();
            hashIndexCoercionType[count] = entry.getValue().getCoercionType();
            hashJoinedProps[count++] = entry.getValue();
        }

        // range property names and types
        String[] rangeIndexPropsProvided = new String[rangeProps.size()];
        Class[] rangeIndexCoercionType = new Class[rangeProps.size()];
        SubordPropRangeKey[] rangeJoinedProps = new SubordPropRangeKey[rangeProps.size()];
        count = 0;
        for (Map.Entry<String, SubordPropRangeKey> entry : rangeProps.entrySet()) {
            rangeIndexPropsProvided[count] = entry.getKey();
            rangeIndexCoercionType[count] = entry.getValue().getCoercionType();
            rangeJoinedProps[count++] = entry.getValue();
        }

        // Add all joined fields to an array for sorting
        SubordinateQueryPlannerIndexPropListPair listPair = SubordinateQueryPlannerUtil.toListOfHashedAndBtreeProps(hashIndexPropsProvided,
                hashIndexCoercionType, rangeIndexPropsProvided, rangeIndexCoercionType);
        return new SubordinateQueryPlannerIndexPropDesc(hashIndexPropsProvided, hashIndexCoercionType,
                rangeIndexPropsProvided, rangeIndexCoercionType, listPair,
                hashJoinedProps, rangeJoinedProps);
    }

    private static Pair<QueryPlanIndexItem, IndexMultiKey> planIndex(boolean unique,
                                                                     List<IndexedPropDesc> hashProps,
                                                                     List<IndexedPropDesc> btreeProps,
                                                                     boolean mustCoerce) {

        // not resolved as full match and not resolved as unique index match, allocate
        IndexMultiKey indexPropKey = new IndexMultiKey(unique, hashProps, btreeProps, null);

        IndexedPropDesc[] indexedPropDescs = hashProps.toArray(new IndexedPropDesc[hashProps.size()]);
        String[] indexProps = IndexedPropDesc.getIndexProperties(indexedPropDescs);
        Class[] indexCoercionTypes = IndexedPropDesc.getCoercionTypes(indexedPropDescs);
        if (!mustCoerce) {
            indexCoercionTypes = null;
        }

        IndexedPropDesc[] rangePropDescs = btreeProps.toArray(new IndexedPropDesc[btreeProps.size()]);
        String[] rangeProps = IndexedPropDesc.getIndexProperties(rangePropDescs);
        Class[] rangeCoercionTypes = IndexedPropDesc.getCoercionTypes(rangePropDescs);

        QueryPlanIndexItem indexItem = new QueryPlanIndexItem(indexProps, indexCoercionTypes, rangeProps, rangeCoercionTypes, unique, null);
        return new Pair<QueryPlanIndexItem, IndexMultiKey>(indexItem, indexPropKey);
    }
}
