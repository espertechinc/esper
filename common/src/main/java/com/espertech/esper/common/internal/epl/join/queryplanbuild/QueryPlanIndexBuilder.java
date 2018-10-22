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
import com.espertech.esper.common.internal.epl.expression.core.ExprIdentNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;
import com.espertech.esper.common.internal.epl.join.analyze.FilterExprAnalyzer;
import com.espertech.esper.common.internal.epl.join.hint.ExcludePlanHint;
import com.espertech.esper.common.internal.epl.join.querygraph.*;
import com.espertech.esper.common.internal.epl.join.queryplan.*;
import com.espertech.esper.common.internal.epl.lookupplan.*;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.util.*;

/**
 * Build query index plans.
 */
public class QueryPlanIndexBuilder {
    /**
     * Build index specification from navigability info.
     * <p>
     * Looks at each stream and determines which properties in the stream must be indexed
     * in order for other streams to look up into the stream. Determines the unique set of properties
     * to avoid building duplicate indexes on the same set of properties.
     *
     * @param queryGraph                - navigability info
     * @param typePerStream             type info
     * @param indexedStreamsUniqueProps per-stream unique props
     * @return query index specs for each stream
     */
    public static QueryPlanIndexForge[] buildIndexSpec(QueryGraphForge queryGraph, EventType[] typePerStream, String[][][] indexedStreamsUniqueProps) {
        int numStreams = queryGraph.getNumStreams();
        QueryPlanIndexForge[] indexSpecs = new QueryPlanIndexForge[numStreams];

        // For each stream compile a list of index property sets.
        for (int streamIndexed = 0; streamIndexed < numStreams; streamIndexed++) {
            List<QueryPlanIndexItemForge> indexesSet = new ArrayList<QueryPlanIndexItemForge>();

            // Look at the index from the viewpoint of the stream looking up in the index
            for (int streamLookup = 0; streamLookup < numStreams; streamLookup++) {
                if (streamIndexed == streamLookup) {
                    continue;
                }

                QueryGraphValueForge value = queryGraph.getGraphValue(streamLookup, streamIndexed);
                QueryGraphValuePairHashKeyIndexForge hashKeyAndIndexProps = value.getHashKeyProps();

                // Sort index properties, but use the sorted properties only to eliminate duplicates
                String[] hashIndexProps = hashKeyAndIndexProps.getIndexed();
                List<QueryGraphValueEntryHashKeyedForge> hashKeyProps = hashKeyAndIndexProps.getKeys();
                CoercionDesc indexCoercionTypes = CoercionUtil.getCoercionTypesHash(typePerStream, streamLookup, streamIndexed, hashKeyProps, hashIndexProps);
                Class[] hashCoercionTypeArr = indexCoercionTypes.getCoercionTypes();

                QueryGraphValuePairRangeIndexForge rangeAndIndexProps = value.getRangeProps();
                String[] rangeIndexProps = rangeAndIndexProps.getIndexed();
                List<QueryGraphValueEntryRangeForge> rangeKeyProps = rangeAndIndexProps.getKeys();
                CoercionDesc rangeCoercionTypes = CoercionUtil.getCoercionTypesRange(typePerStream, streamIndexed, rangeIndexProps, rangeKeyProps);
                Class[] rangeCoercionTypeArr = rangeCoercionTypes.getCoercionTypes();

                if (hashIndexProps.length == 0 && rangeIndexProps.length == 0) {
                    QueryGraphValuePairInKWSingleIdxForge singles = value.getInKeywordSingles();
                    if (!singles.getKey().isEmpty()) {
                        String indexedProp = singles.getIndexed()[0];
                        Class indexedType = typePerStream[streamIndexed].getPropertyType(indexedProp);
                        QueryPlanIndexItemForge indexItem = new QueryPlanIndexItemForge(new String[]{indexedProp}, new Class[]{indexedType}, new String[0], new Class[0], false, null, typePerStream[streamIndexed]);
                        checkDuplicateOrAdd(indexItem, indexesSet);
                    }

                    List<QueryGraphValuePairInKWMultiIdx> multis = value.getInKeywordMulti();
                    if (!multis.isEmpty()) {
                        QueryGraphValuePairInKWMultiIdx multi = multis.get(0);
                        for (ExprNode propIndexed : multi.getIndexed()) {
                            ExprIdentNode identNode = (ExprIdentNode) propIndexed;
                            Class type = identNode.getForge().getEvaluationType();
                            QueryPlanIndexItemForge indexItem = new QueryPlanIndexItemForge(new String[]{identNode.getResolvedPropertyName()}, new Class[]{type}, new String[0], new Class[0], false, null, typePerStream[streamIndexed]);
                            checkDuplicateOrAdd(indexItem, indexesSet);
                        }
                    }
                    continue;
                }

                // reduce to any unique index if applicable
                boolean unique = false;
                QueryPlanIndexUniqueHelper.ReducedHashKeys reduced = QueryPlanIndexUniqueHelper.reduceToUniqueIfPossible(hashIndexProps, hashCoercionTypeArr, hashKeyProps, indexedStreamsUniqueProps[streamIndexed]);
                if (reduced != null) {
                    hashIndexProps = reduced.getPropertyNames();
                    hashCoercionTypeArr = reduced.getCoercionTypes();
                    unique = true;
                    rangeIndexProps = new String[0];
                    rangeCoercionTypeArr = new Class[0];
                }

                QueryPlanIndexItemForge proposed = new QueryPlanIndexItemForge(hashIndexProps, hashCoercionTypeArr,
                        rangeIndexProps, rangeCoercionTypeArr, unique, null, typePerStream[streamIndexed]);
                checkDuplicateOrAdd(proposed, indexesSet);
            }

            // create full-table-scan
            if (indexesSet.isEmpty()) {
                indexesSet.add(new QueryPlanIndexItemForge(new String[0], new Class[0], new String[0], new Class[0], false, null, typePerStream[streamIndexed]));
            }

            indexSpecs[streamIndexed] = QueryPlanIndexForge.makeIndex(indexesSet);
        }

        return indexSpecs;
    }

    public static SubordPropPlan getJoinProps(ExprNode filterExpr, int outsideStreamCount, EventType[] allStreamTypesZeroIndexed, ExcludePlanHint excludePlanHint) {
        // No filter expression means full table scan
        if (filterExpr == null) {
            return new SubordPropPlan();
        }

        // analyze query graph
        QueryGraphForge queryGraph = new QueryGraphForge(outsideStreamCount + 1, excludePlanHint, true);
        FilterExprAnalyzer.analyze(filterExpr, queryGraph, false);

        // Build a list of streams and indexes
        LinkedHashMap<String, SubordPropHashKeyForge> joinProps = new LinkedHashMap<String, SubordPropHashKeyForge>();
        LinkedHashMap<String, SubordPropRangeKeyForge> rangeProps = new LinkedHashMap<String, SubordPropRangeKeyForge>();
        Map<QueryGraphValueEntryCustomKeyForge, QueryGraphValueEntryCustomOperationForge> customIndexOps = Collections.emptyMap();

        for (int stream = 0; stream < outsideStreamCount; stream++) {
            int lookupStream = stream + 1;

            QueryGraphValueForge queryGraphValue = queryGraph.getGraphValue(lookupStream, 0);
            QueryGraphValuePairHashKeyIndexForge hashKeysAndIndexes = queryGraphValue.getHashKeyProps();

            // determine application functions
            for (QueryGraphValueDescForge item : queryGraphValue.getItems()) {
                if (item.getEntry() instanceof QueryGraphValueEntryCustomForge) {
                    if (customIndexOps.isEmpty()) {
                        customIndexOps = new HashMap<>();
                    }
                    QueryGraphValueEntryCustomForge custom = (QueryGraphValueEntryCustomForge) item.getEntry();
                    custom.mergeInto(customIndexOps);
                }
            }

            // handle key-lookups
            List<QueryGraphValueEntryHashKeyedForge> keyPropertiesJoin = hashKeysAndIndexes.getKeys();
            String[] indexPropertiesJoin = hashKeysAndIndexes.getIndexed();
            if (!keyPropertiesJoin.isEmpty()) {
                if (keyPropertiesJoin.size() != indexPropertiesJoin.length) {
                    throw new IllegalStateException("Invalid query key and index property collection for stream " + stream);
                }

                for (int i = 0; i < keyPropertiesJoin.size(); i++) {
                    QueryGraphValueEntryHashKeyedForge keyDesc = keyPropertiesJoin.get(i);
                    ExprNode compareNode = keyDesc.getKeyExpr();

                    Class keyPropType = JavaClassHelper.getBoxedType(compareNode.getForge().getEvaluationType());
                    Class indexedPropType = JavaClassHelper.getBoxedType(allStreamTypesZeroIndexed[0].getPropertyType(indexPropertiesJoin[i]));
                    Class coercionType = indexedPropType;
                    if (keyPropType != indexedPropType) {
                        coercionType = JavaClassHelper.getCompareToCoercionType(keyPropType, indexedPropType);
                    }

                    SubordPropHashKeyForge desc;
                    if (keyPropertiesJoin.get(i) instanceof QueryGraphValueEntryHashKeyedForgeExpr) {
                        QueryGraphValueEntryHashKeyedForgeExpr keyExpr = (QueryGraphValueEntryHashKeyedForgeExpr) keyPropertiesJoin.get(i);
                        Integer keyStreamNum = keyExpr.isRequiresKey() ? stream : null;
                        desc = new SubordPropHashKeyForge(keyDesc, keyStreamNum, coercionType);
                    } else {
                        QueryGraphValueEntryHashKeyedForgeProp prop = (QueryGraphValueEntryHashKeyedForgeProp) keyDesc;
                        desc = new SubordPropHashKeyForge(prop, stream, coercionType);
                    }
                    joinProps.put(indexPropertiesJoin[i], desc);
                }
            }

            // handle range lookups
            QueryGraphValuePairRangeIndexForge rangeKeysAndIndexes = queryGraphValue.getRangeProps();
            String[] rangeIndexes = rangeKeysAndIndexes.getIndexed();
            List<QueryGraphValueEntryRangeForge> rangeDescs = rangeKeysAndIndexes.getKeys();
            if (rangeDescs.isEmpty()) {
                continue;
            }

            // get all ranges lookups
            int count = -1;
            for (QueryGraphValueEntryRangeForge rangeDesc : rangeDescs) {
                count++;
                String rangeIndexProp = rangeIndexes[count];

                SubordPropRangeKeyForge subqRangeDesc = rangeProps.get(rangeIndexProp);

                // other streams may specify the start or end endpoint of a range, therefore this operation can be additive
                if (subqRangeDesc != null) {
                    if (subqRangeDesc.getRangeInfo().getType().isRange()) {
                        continue;
                    }

                    // see if we can make this additive by using a range
                    QueryGraphValueEntryRangeRelOpForge relOpOther = (QueryGraphValueEntryRangeRelOpForge) subqRangeDesc.getRangeInfo();
                    QueryGraphValueEntryRangeRelOpForge relOpThis = (QueryGraphValueEntryRangeRelOpForge) rangeDesc;

                    QueryGraphRangeConsolidateDesc opsDesc = QueryGraphRangeUtil.getCanConsolidate(relOpThis.getType(), relOpOther.getType());
                    if (opsDesc != null) {
                        ExprNode start;
                        ExprNode end;
                        if (!opsDesc.isReverse()) {
                            start = relOpOther.getExpression();
                            end = relOpThis.getExpression();
                        } else {
                            start = relOpThis.getExpression();
                            end = relOpOther.getExpression();
                        }
                        boolean allowRangeReversal = relOpOther.isBetweenPart() && relOpThis.isBetweenPart();
                        QueryGraphValueEntryRangeInForge rangeIn = new QueryGraphValueEntryRangeInForge(opsDesc.getType(), start, end, allowRangeReversal);

                        Class indexedPropType = JavaClassHelper.getBoxedType(allStreamTypesZeroIndexed[0].getPropertyType(rangeIndexProp));
                        Class coercionType = indexedPropType;
                        Class proposedType = CoercionUtil.getCoercionTypeRangeIn(indexedPropType, rangeIn.getExprStart(), rangeIn.getExprEnd());
                        if (proposedType != null && proposedType != indexedPropType) {
                            coercionType = proposedType;
                        }

                        subqRangeDesc = new SubordPropRangeKeyForge(rangeIn, coercionType);
                        rangeProps.put(rangeIndexProp, subqRangeDesc);
                    }
                    // ignore
                    continue;
                }

                // an existing entry has not been found
                if (rangeDesc.getType().isRange()) {
                    QueryGraphValueEntryRangeInForge rangeIn = (QueryGraphValueEntryRangeInForge) rangeDesc;
                    Class indexedPropType = JavaClassHelper.getBoxedType(allStreamTypesZeroIndexed[0].getPropertyType(rangeIndexProp));
                    Class coercionType = indexedPropType;
                    Class proposedType = CoercionUtil.getCoercionTypeRangeIn(indexedPropType, rangeIn.getExprStart(), rangeIn.getExprEnd());
                    if (proposedType != null && proposedType != indexedPropType) {
                        coercionType = proposedType;
                    }
                    subqRangeDesc = new SubordPropRangeKeyForge(rangeDesc, coercionType);
                } else {
                    QueryGraphValueEntryRangeRelOpForge relOp = (QueryGraphValueEntryRangeRelOpForge) rangeDesc;
                    Class keyPropType = relOp.getExpression().getForge().getEvaluationType();
                    Class indexedPropType = JavaClassHelper.getBoxedType(allStreamTypesZeroIndexed[0].getPropertyType(rangeIndexProp));
                    Class coercionType = indexedPropType;
                    if (keyPropType != indexedPropType) {
                        coercionType = JavaClassHelper.getCompareToCoercionType(keyPropType, indexedPropType);
                    }
                    subqRangeDesc = new SubordPropRangeKeyForge(rangeDesc, coercionType);
                }
                rangeProps.put(rangeIndexProp, subqRangeDesc);
            }
        }

        SubordPropInKeywordSingleIndex inKeywordSingleIdxProp = null;
        SubordPropInKeywordMultiIndex inKeywordMultiIdxProp = null;
        if (joinProps.isEmpty() && rangeProps.isEmpty()) {
            for (int stream = 0; stream < outsideStreamCount; stream++) {
                int lookupStream = stream + 1;
                QueryGraphValueForge queryGraphValue = queryGraph.getGraphValue(lookupStream, 0);

                QueryGraphValuePairInKWSingleIdxForge inkwSingles = queryGraphValue.getInKeywordSingles();
                if (inkwSingles.getIndexed().length != 0) {
                    ExprNode[] keys = inkwSingles.getKey().get(0).getKeyExprs();
                    String key = inkwSingles.getIndexed()[0];
                    if (inKeywordSingleIdxProp != null) {
                        continue;
                    }
                    Class coercionType = keys[0].getForge().getEvaluationType();  // for in-comparison the same type is required
                    inKeywordSingleIdxProp = new SubordPropInKeywordSingleIndex(key, coercionType, keys);
                }

                List<QueryGraphValuePairInKWMultiIdx> inkwMultis = queryGraphValue.getInKeywordMulti();
                if (!inkwMultis.isEmpty()) {
                    QueryGraphValuePairInKWMultiIdx multi = inkwMultis.get(0);
                    inKeywordMultiIdxProp = new SubordPropInKeywordMultiIndex(ExprNodeUtilityQuery.getIdentResolvedPropertyNames(multi.getIndexed()), multi.getIndexed()[0].getForge().getEvaluationType(), multi.getKey().getKeyExpr());
                }

                if (inKeywordSingleIdxProp != null && inKeywordMultiIdxProp != null) {
                    inKeywordMultiIdxProp = null;
                }
            }
        }

        return new SubordPropPlan(joinProps, rangeProps, inKeywordSingleIdxProp, inKeywordMultiIdxProp, customIndexOps);
    }

    private static void checkDuplicateOrAdd(QueryPlanIndexItemForge proposed, List<QueryPlanIndexItemForge> indexesSet) {
        boolean found = false;
        for (QueryPlanIndexItemForge index : indexesSet) {
            if (proposed.equalsCompareSortedProps(index)) {
                found = true;
                break;
            }
        }

        if (!found) {
            indexesSet.add(proposed);
        }
    }
}
