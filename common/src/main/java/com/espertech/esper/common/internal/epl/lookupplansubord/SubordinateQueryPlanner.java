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
package com.espertech.esper.common.internal.epl.lookupplansubord;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlan;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyPlanner;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.index.advanced.index.quadtree.SubordTableLookupStrategyFactoryQuadTreeForge;
import com.espertech.esper.common.internal.epl.index.advanced.index.service.EventAdvancedIndexProvisionCompileTime;
import com.espertech.esper.common.internal.epl.index.advanced.index.service.EventAdvancedIndexProvisionRuntime;
import com.espertech.esper.common.internal.epl.join.hint.ExcludePlanHint;
import com.espertech.esper.common.internal.epl.join.hint.IndexHint;
import com.espertech.esper.common.internal.epl.join.hint.IndexHintInstruction;
import com.espertech.esper.common.internal.epl.join.lookup.IndexMultiKey;
import com.espertech.esper.common.internal.epl.join.lookup.IndexedPropDesc;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryCustomKeyForge;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryCustomOperationForge;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryHashKeyedForgeExpr;
import com.espertech.esper.common.internal.epl.join.queryplan.CoercionDesc;
import com.espertech.esper.common.internal.epl.join.queryplan.QueryPlanIndexItemForge;
import com.espertech.esper.common.internal.epl.join.queryplanbuild.QueryPlanIndexBuilder;
import com.espertech.esper.common.internal.epl.lookup.AdvancedIndexIndexMultiKeyPart;
import com.espertech.esper.common.internal.epl.lookup.SubordTableLookupStrategyFactoryForge;
import com.espertech.esper.common.internal.epl.lookupplan.*;
import com.espertech.esper.common.internal.epl.lookupsubord.SubordWMatchExprLookupStrategyAllFilteredForge;
import com.espertech.esper.common.internal.epl.lookupsubord.SubordWMatchExprLookupStrategyAllUnfilteredForge;
import com.espertech.esper.common.internal.epl.lookupsubord.SubordWMatchExprLookupStrategyIndexedFilteredForge;
import com.espertech.esper.common.internal.epl.lookupsubord.SubordWMatchExprLookupStrategyIndexedUnfilteredForge;
import com.espertech.esper.common.internal.epl.virtualdw.SubordTableLookupStrategyFactoryForgeVDW;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.type.NameAndModule;

import java.util.*;

import static com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery.getPropertiesPerExpressionExpectSingle;

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
        StatementRawInfo statementRawInfo,
        StatementCompileTimeServices compileTimeServices)
        throws ExprValidationException {

        EventType[] allStreamsZeroIndexed = new EventType[]{eventTypeIndexed, filterEventType};
        EventType[] outerStreams = new EventType[]{filterEventType};
        SubordPropPlan joinedPropPlan = QueryPlanIndexBuilder.getJoinProps(joinExpr, 1, allStreamsZeroIndexed, excludePlanHint);

        // No join expression means all
        if (joinExpr == null && !isVirtualDataWindow) {
            SubordinateWMatchExprQueryPlanForge forge = new SubordinateWMatchExprQueryPlanForge(new SubordWMatchExprLookupStrategyAllUnfilteredForge(), null);
            return new SubordinateWMatchExprQueryPlanResult(forge, Collections.emptyList());
        }

        SubordinateQueryPlan queryPlan = planSubquery(outerStreams, joinedPropPlan, true, false, optionalIndexHint, isIndexShare, subqueryNumber,
            isVirtualDataWindow, indexMetadata, optionalUniqueKeyProps, onlyUseExistingIndexes, eventTypeIndexed, statementRawInfo, compileTimeServices);

        if (queryPlan == null) {
            SubordinateWMatchExprQueryPlanForge forge = new SubordinateWMatchExprQueryPlanForge(new SubordWMatchExprLookupStrategyAllFilteredForge(joinExpr), null);
            return new SubordinateWMatchExprQueryPlanResult(forge, Collections.emptyList());
        }

        SubordinateQueryPlanDescForge queryPlanDesc = queryPlan.getForge();
        SubordinateWMatchExprQueryPlanForge forge;
        if (joinExpr == null) {   // it can be null when using virtual data window
            forge = new SubordinateWMatchExprQueryPlanForge(
                new SubordWMatchExprLookupStrategyIndexedUnfilteredForge(queryPlanDesc.getLookupStrategyFactory()), queryPlanDesc.getIndexDescs());
        } else {
            SubordWMatchExprLookupStrategyIndexedFilteredForge filteredForge = new SubordWMatchExprLookupStrategyIndexedFilteredForge(joinExpr.getForge(), queryPlanDesc.getLookupStrategyFactory());
            forge = new SubordinateWMatchExprQueryPlanForge(filteredForge, queryPlanDesc.getIndexDescs());
        }
        return new SubordinateWMatchExprQueryPlanResult(forge, queryPlan.getAdditionalForgeables());
    }

    public static SubordinateQueryPlan planSubquery(EventType[] outerStreams,
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
                                                    EventType eventTypeIndexed,
                                                    StatementRawInfo statementRawInfo,
                                                    StatementCompileTimeServices services)
        throws ExprValidationException {
        if (isVirtualDataWindow) {
            SubordinateQueryPlannerIndexPropDesc indexProps = getIndexPropDesc(joinDesc.getHashProps(), joinDesc.getRangeProps());
            SubordTableLookupStrategyFactoryForgeVDW lookupStrategyFactory = new SubordTableLookupStrategyFactoryForgeVDW(statementRawInfo.getStatementName(), statementRawInfo.getAnnotations(),
                outerStreams,
                Arrays.asList(indexProps.getHashJoinedProps()),
                new CoercionDesc(false, indexProps.getHashIndexCoercionType()),
                Arrays.asList(indexProps.getRangeJoinedProps()),
                new CoercionDesc(false, indexProps.getRangeIndexCoercionType()),
                isNWOnTrigger,
                joinDesc, forceTableScan, indexProps.getListPair());
            SubordinateQueryPlanDescForge forge = new SubordinateQueryPlanDescForge(lookupStrategyFactory, null);
            return new SubordinateQueryPlan(forge, Collections.emptyList());
        }

        if (joinDesc.getCustomIndexOps() != null && !joinDesc.getCustomIndexOps().isEmpty()) {
            for (Map.Entry<QueryGraphValueEntryCustomKeyForge, QueryGraphValueEntryCustomOperationForge> op : joinDesc.getCustomIndexOps().entrySet()) {
                for (Map.Entry<IndexMultiKey, EventTableIndexMetadataEntry> index : indexMetadata.getIndexes().entrySet()) {
                    if (isCustomIndexMatch(index, op)) {
                        EventAdvancedIndexProvisionRuntime provisionDesc = index.getValue().getOptionalQueryPlanIndexItem().getAdvancedIndexProvisionDesc();
                        SubordTableLookupStrategyFactoryQuadTreeForge lookupStrategyFactory = provisionDesc.getFactory().getForge().getSubordinateLookupStrategy(op.getKey().getOperationName(), op.getValue().getPositionalExpressions(), isNWOnTrigger, outerStreams.length);
                        EventAdvancedIndexProvisionCompileTime provisionCompileTime = provisionDesc.toCompileTime(eventTypeIndexed, statementRawInfo, services);
                        QueryPlanIndexItemForge indexItemForge = new QueryPlanIndexItemForge(new String[0], new Class[0], new String[0], new Class[0], false, provisionCompileTime, eventTypeIndexed);
                        SubordinateQueryIndexDescForge indexDesc = new SubordinateQueryIndexDescForge(null, index.getValue().getOptionalIndexName(), index.getValue().getOptionalIndexModuleName(), index.getKey(), indexItemForge);
                        SubordinateQueryPlanDescForge forge = new SubordinateQueryPlanDescForge(lookupStrategyFactory, new SubordinateQueryIndexDescForge[]{indexDesc});
                        return new SubordinateQueryPlan(forge, Collections.emptyList());
                    }
                }
            }
        }

        List<SubordPropHashKeyForge> hashKeys = Collections.emptyList();
        CoercionDesc hashKeyCoercionTypes = null;
        List<SubordPropRangeKeyForge> rangeKeys = Collections.emptyList();
        CoercionDesc rangeKeyCoercionTypes = null;
        ExprNode[] inKeywordSingleIdxKeys = null;
        ExprNode inKeywordMultiIdxKey = null;

        SubordinateQueryIndexDescForge[] indexDescs;
        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>(2);
        MultiKeyClassRef multiKeyClasses = null;
        if (joinDesc.getInKeywordSingleIndex() != null) {
            SubordPropInKeywordSingleIndex single = joinDesc.getInKeywordSingleIndex();
            SubordPropHashKeyForge keyInfo = new SubordPropHashKeyForge(new QueryGraphValueEntryHashKeyedForgeExpr(single.getExpressions()[0], false), null, single.getCoercionType());
            SubordinateQueryIndexSuggest index = findOrSuggestIndex(
                Collections.singletonMap(single.getIndexedProp(), keyInfo),
                Collections.emptyMap(), optionalIndexHint, indexShare, subqueryNumber,
                indexMetadata, optionalUniqueKeyProps, onlyUseExistingIndexes, eventTypeIndexed, statementRawInfo, services);

            if (index == null) {
                return null;
            }
            SubordinateQueryIndexDescForge indexDesc = index.getForge();
            SubordinateQueryIndexDescForge desc = new SubordinateQueryIndexDescForge(indexDesc.getOptionalIndexKeyInfo(), indexDesc.getIndexName(), indexDesc.getIndexModuleName(), indexDesc.getIndexMultiKey(), indexDesc.getOptionalQueryPlanIndexItem());
            indexDescs = new SubordinateQueryIndexDescForge[]{desc};
            inKeywordSingleIdxKeys = single.getExpressions();
            additionalForgeables.addAll(index.getMultiKeyForgeables());
        } else if (joinDesc.getInKeywordMultiIndex() != null) {
            SubordPropInKeywordMultiIndex multi = joinDesc.getInKeywordMultiIndex();

            indexDescs = new SubordinateQueryIndexDescForge[multi.getIndexedProp().length];
            for (int i = 0; i < multi.getIndexedProp().length; i++) {
                SubordPropHashKeyForge keyInfo = new SubordPropHashKeyForge(new QueryGraphValueEntryHashKeyedForgeExpr(multi.getExpression(), false), null, multi.getCoercionType());
                SubordinateQueryIndexSuggest index = findOrSuggestIndex(
                    Collections.singletonMap(multi.getIndexedProp()[i], keyInfo),
                    Collections.emptyMap(), optionalIndexHint, indexShare, subqueryNumber,
                    indexMetadata, optionalUniqueKeyProps, onlyUseExistingIndexes, eventTypeIndexed, statementRawInfo, services);
                SubordinateQueryIndexDescForge indexDesc = index.getForge();
                additionalForgeables.addAll(index.getMultiKeyForgeables());
                if (indexDesc == null) {
                    return null;
                }
                indexDescs[i] = indexDesc;
            }
            inKeywordMultiIdxKey = multi.getExpression();
        } else {
            SubordinateQueryIndexSuggest index = findOrSuggestIndex(joinDesc.getHashProps(),
                joinDesc.getRangeProps(), optionalIndexHint, false, subqueryNumber,
                indexMetadata, optionalUniqueKeyProps, onlyUseExistingIndexes, eventTypeIndexed, statementRawInfo, services);
            if (index == null) {
                return null;
            }
            SubordinateQueryIndexDescForge indexDesc = index.getForge();
            additionalForgeables.addAll(index.getMultiKeyForgeables());
            IndexKeyInfo indexKeyInfo = indexDesc.getOptionalIndexKeyInfo();
            hashKeys = indexKeyInfo.getOrderedHashDesc();
            hashKeyCoercionTypes = indexKeyInfo.getOrderedKeyCoercionTypes();
            rangeKeys = indexKeyInfo.getOrderedRangeDesc();
            rangeKeyCoercionTypes = indexKeyInfo.getOrderedRangeCoercionTypes();
            SubordinateQueryIndexDescForge desc = new SubordinateQueryIndexDescForge(indexDesc.getOptionalIndexKeyInfo(), indexDesc.getIndexName(), indexDesc.getIndexModuleName(), indexDesc.getIndexMultiKey(), indexDesc.getOptionalQueryPlanIndexItem());
            indexDescs = new SubordinateQueryIndexDescForge[]{desc};

            if (indexDesc.getOptionalQueryPlanIndexItem() == null) {
                MultiKeyPlan multiKeyPlan = MultiKeyPlanner.planMultiKey(hashKeyCoercionTypes.getCoercionTypes(), true, statementRawInfo, services.getSerdeResolver());
                multiKeyClasses = multiKeyPlan.getClassRef();
                additionalForgeables.addAll(multiKeyPlan.getMultiKeyForgeables());
            } else {
                multiKeyClasses = indexDesc.getOptionalQueryPlanIndexItem().getHashMultiKeyClasses();
            }
        }

        if (forceTableScan) {
            return null;
        }

        SubordTableLookupStrategyFactoryForge lookupStrategyFactory = SubordinateTableLookupStrategyUtil.getLookupStrategy(outerStreams,
            hashKeys, hashKeyCoercionTypes, multiKeyClasses, rangeKeys, rangeKeyCoercionTypes, inKeywordSingleIdxKeys, inKeywordMultiIdxKey, isNWOnTrigger);
        SubordinateQueryPlanDescForge forge = new SubordinateQueryPlanDescForge(lookupStrategyFactory, indexDescs);
        return new SubordinateQueryPlan(forge, additionalForgeables);
    }

    private static boolean isCustomIndexMatch(Map.Entry<IndexMultiKey, EventTableIndexMetadataEntry> index, Map.Entry<QueryGraphValueEntryCustomKeyForge, QueryGraphValueEntryCustomOperationForge> op) {
        if (index.getValue().getExplicitIndexNameIfExplicit() == null || index.getValue().getOptionalQueryPlanIndexItem() == null) {
            return false;
        }
        AdvancedIndexIndexMultiKeyPart provision = index.getKey().getAdvancedIndexDesc();
        if (provision == null) {
            return false;
        }
        EventAdvancedIndexProvisionRuntime provisionDesc = index.getValue().getOptionalQueryPlanIndexItem().getAdvancedIndexProvisionDesc();
        if (!provisionDesc.getFactory().getForge().providesIndexForOperation(op.getKey().getOperationName())) {
            return false;
        }

        ExprNode[] opExpressions = op.getKey().getExprNodes();
        String[] opProperties = getPropertiesPerExpressionExpectSingle(opExpressions);
        String[] indexProperties = index.getKey().getAdvancedIndexDesc().getIndexedProperties();
        return Arrays.equals(indexProperties, opProperties);
    }

    private static SubordinateQueryIndexSuggest findOrSuggestIndex(
        Map<String, SubordPropHashKeyForge> hashProps,
        Map<String, SubordPropRangeKeyForge> rangeProps,
        IndexHint optionalIndexHint,
        boolean isIndexShare,
        int subqueryNumber,
        EventTableIndexMetadata indexMetadata,
        Set<String> optionalUniqueKeyProps,
        boolean onlyUseExistingIndexes,
        EventType eventTypeIndexed,
        StatementRawInfo raw,
        StatementCompileTimeServices services) {

        SubordinateQueryPlannerIndexPropDesc indexProps = getIndexPropDesc(hashProps, rangeProps);
        SubordinateQueryPlannerIndexPropListPair hashedAndBtreeProps = indexProps.getListPair();

        // Get or create the table for this index (exact match or property names, type of index and coercion type is expected)
        IndexKeyInfo indexKeyInfo;   // how needs all of IndexKeyInfo+QueryPlanIndexItem+IndexMultiKey
        IndexMultiKey indexMultiKey;
        String indexName = null;
        String indexModuleName = null;
        QueryPlanIndexItemForge planIndexItem = null;

        if (hashedAndBtreeProps.getHashedProps().isEmpty() && hashedAndBtreeProps.getBtreeProps().isEmpty()) {
            return null;
        }

        Pair<IndexMultiKey, NameAndModule> existing = null;
        SubordinateQueryIndexPlan planned = null;

        // consider index hints
        List<IndexHintInstruction> optionalIndexHintInstructions = null;
        if (optionalIndexHint != null) {
            optionalIndexHintInstructions = optionalIndexHint.getInstructionsSubquery(subqueryNumber);
        }

        IndexMultiKey indexFoundPair = EventTableIndexUtil.findIndexConsiderTyping(indexMetadata.getIndexes(), hashedAndBtreeProps.getHashedProps(), hashedAndBtreeProps.getBtreeProps(), optionalIndexHintInstructions);
        if (indexFoundPair != null) {
            EventTableIndexMetadataEntry hintIndex = indexMetadata.getIndexes().get(indexFoundPair);
            existing = new Pair<>(indexFoundPair, new NameAndModule(hintIndex.getOptionalIndexName(), hintIndex.getOptionalIndexModuleName()));
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

            planned = planIndex(unique, proposedHashedProps, proposedBtreeProps, eventTypeIndexed, raw, services);
        }

        // compile index information
        if (existing == null && planned == null) {
            return null;
        }
        // handle existing
        List<StmtClassForgeableFactory> multiKeyForgeables;
        if (existing != null) {
            indexKeyInfo = SubordinateQueryPlannerUtil.compileIndexKeyInfo(existing.getFirst(),
                indexProps.getHashIndexPropsProvided(), indexProps.getHashJoinedProps(),
                indexProps.getRangeIndexPropsProvided(), indexProps.getRangeJoinedProps());
            indexName = existing.getSecond().getName();
            indexModuleName = existing.getSecond().getModuleName();
            indexMultiKey = existing.getFirst();
            multiKeyForgeables = Collections.emptyList();
        } else {
            // handle planned
            indexKeyInfo = SubordinateQueryPlannerUtil.compileIndexKeyInfo(planned.getIndexPropKey(),
                indexProps.getHashIndexPropsProvided(), indexProps.getHashJoinedProps(),
                indexProps.getRangeIndexPropsProvided(), indexProps.getRangeJoinedProps());
            indexMultiKey = planned.getIndexPropKey();
            planIndexItem = planned.getIndexItem();
            multiKeyForgeables = planned.getMultiKeyForgeables();
        }

        SubordinateQueryIndexDescForge forge = new SubordinateQueryIndexDescForge(indexKeyInfo, indexName, indexModuleName, indexMultiKey, planIndexItem);
        return new SubordinateQueryIndexSuggest(forge, multiKeyForgeables);
    }

    private static SubordinateQueryPlannerIndexPropDesc getIndexPropDesc(Map<String, SubordPropHashKeyForge> hashProps, Map<String, SubordPropRangeKeyForge> rangeProps) {

        // hash property names and types
        String[] hashIndexPropsProvided = new String[hashProps.size()];
        Class[] hashIndexCoercionType = new Class[hashProps.size()];
        SubordPropHashKeyForge[] hashJoinedProps = new SubordPropHashKeyForge[hashProps.size()];
        int count = 0;
        for (Map.Entry<String, SubordPropHashKeyForge> entry : hashProps.entrySet()) {
            hashIndexPropsProvided[count] = entry.getKey();
            hashIndexCoercionType[count] = entry.getValue().getCoercionType();
            hashJoinedProps[count++] = entry.getValue();
        }

        // range property names and types
        String[] rangeIndexPropsProvided = new String[rangeProps.size()];
        Class[] rangeIndexCoercionType = new Class[rangeProps.size()];
        SubordPropRangeKeyForge[] rangeJoinedProps = new SubordPropRangeKeyForge[rangeProps.size()];
        count = 0;
        for (Map.Entry<String, SubordPropRangeKeyForge> entry : rangeProps.entrySet()) {
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

    private static SubordinateQueryIndexPlan planIndex(boolean unique,
                                                       List<IndexedPropDesc> hashProps,
                                                       List<IndexedPropDesc> btreeProps,
                                                       EventType eventTypeIndexed,
                                                       StatementRawInfo raw,
                                                       StatementCompileTimeServices services) {

        // not resolved as full match and not resolved as unique index match, allocate
        IndexMultiKey indexPropKey = new IndexMultiKey(unique, hashProps, btreeProps, null);

        IndexedPropDesc[] indexedPropDescs = hashProps.toArray(new IndexedPropDesc[hashProps.size()]);
        String[] indexProps = IndexedPropDesc.getIndexProperties(indexedPropDescs);
        Class[] indexCoercionTypes = IndexedPropDesc.getCoercionTypes(indexedPropDescs);

        IndexedPropDesc[] rangePropDescs = btreeProps.toArray(new IndexedPropDesc[btreeProps.size()]);
        String[] rangeProps = IndexedPropDesc.getIndexProperties(rangePropDescs);
        Class[] rangeCoercionTypes = IndexedPropDesc.getCoercionTypes(rangePropDescs);

        QueryPlanIndexItemForge indexItem = new QueryPlanIndexItemForge(indexProps, indexCoercionTypes, rangeProps, rangeCoercionTypes, unique, null, eventTypeIndexed);

        MultiKeyPlan multiKeyPlan = MultiKeyPlanner.planMultiKey(indexCoercionTypes, true, raw, services.getSerdeResolver());
        indexItem.setHashMultiKeyClasses(multiKeyPlan.getClassRef());

        DataInputOutputSerdeForge[] rangeSerdes = new DataInputOutputSerdeForge[rangeCoercionTypes.length];
        for (int i = 0; i < rangeCoercionTypes.length; i++) {
            rangeSerdes[i] = services.getSerdeResolver().serdeForIndexBtree(rangeCoercionTypes[i], raw);
        }
        indexItem.setRangeSerdes(rangeSerdes);

        return new SubordinateQueryIndexPlan(indexItem, indexPropKey, multiKeyPlan.getMultiKeyForgeables());
    }
}
