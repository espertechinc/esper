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
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.join.plan.CoercionDesc;

import java.util.List;

public class SubordinateTableLookupStrategyUtil {

    public static SubordTableLookupStrategyFactory getLookupStrategy(EventType[] outerStreamTypesZeroIndexed,
                                                                     List<SubordPropHashKey> hashKeys,
                                                                     CoercionDesc hashKeyCoercionTypes,
                                                                     List<SubordPropRangeKey> rangeKeys,
                                                                     CoercionDesc rangeKeyCoercionTypes,
                                                                     ExprNode[] inKeywordSingleIdxKeys,
                                                                     ExprNode inKeywordMultiIdxKey,
                                                                     boolean isNWOnTrigger) {


        boolean isStrictKeys = SubordPropUtil.isStrictKeyJoin(hashKeys);
        String[] hashStrictKeys = null;
        int[] hashStrictKeyStreams = null;
        if (isStrictKeys) {
            hashStrictKeyStreams = SubordPropUtil.getKeyStreamNums(hashKeys);
            hashStrictKeys = SubordPropUtil.getKeyProperties(hashKeys);
        }

        int numStreamsTotal = outerStreamTypesZeroIndexed.length + 1;
        SubordTableLookupStrategyFactory lookupStrategy;
        if (inKeywordSingleIdxKeys != null) {
            lookupStrategy = new SubordInKeywordSingleTableLookupStrategyFactory(isNWOnTrigger, numStreamsTotal, inKeywordSingleIdxKeys);
        } else if (inKeywordMultiIdxKey != null) {
            lookupStrategy = new SubordInKeywordMultiTableLookupStrategyFactory(isNWOnTrigger, numStreamsTotal, inKeywordMultiIdxKey);
        } else if (hashKeys.isEmpty() && rangeKeys.isEmpty()) {
            lookupStrategy = new SubordFullTableScanLookupStrategyFactory();
        } else if (hashKeys.size() > 0 && rangeKeys.isEmpty()) {
            if (hashKeys.size() == 1) {
                if (!hashKeyCoercionTypes.isCoerce()) {
                    if (isStrictKeys) {
                        lookupStrategy = new SubordIndexedTableLookupStrategySinglePropFactory(isNWOnTrigger, outerStreamTypesZeroIndexed, hashStrictKeyStreams[0], hashStrictKeys[0]);
                    } else {
                        lookupStrategy = new SubordIndexedTableLookupStrategySingleExprFactory(isNWOnTrigger, numStreamsTotal, hashKeys.get(0));
                    }
                } else {
                    lookupStrategy = new SubordIndexedTableLookupStrategySingleCoercingFactory(isNWOnTrigger, numStreamsTotal, hashKeys.get(0), hashKeyCoercionTypes.getCoercionTypes()[0]);
                }
            } else {
                if (!hashKeyCoercionTypes.isCoerce()) {
                    if (isStrictKeys) {
                        lookupStrategy = new SubordIndexedTableLookupStrategyPropFactory(isNWOnTrigger, outerStreamTypesZeroIndexed, hashStrictKeyStreams, hashStrictKeys);
                    } else {
                        lookupStrategy = new SubordIndexedTableLookupStrategyExprFactory(isNWOnTrigger, numStreamsTotal, hashKeys);
                    }
                } else {
                    lookupStrategy = new SubordIndexedTableLookupStrategyCoercingFactory(isNWOnTrigger, numStreamsTotal, hashKeys, hashKeyCoercionTypes.getCoercionTypes());
                }
            }
        } else if (hashKeys.size() == 0 && rangeKeys.size() == 1) {
            lookupStrategy = new SubordSortedTableLookupStrategyFactory(isNWOnTrigger, numStreamsTotal, rangeKeys.get(0));
        } else {
            lookupStrategy = new SubordCompositeTableLookupStrategyFactory(isNWOnTrigger, numStreamsTotal, hashKeys, hashKeyCoercionTypes.getCoercionTypes(),
                    rangeKeys, rangeKeyCoercionTypes.getCoercionTypes());
        }
        return lookupStrategy;
    }
}
