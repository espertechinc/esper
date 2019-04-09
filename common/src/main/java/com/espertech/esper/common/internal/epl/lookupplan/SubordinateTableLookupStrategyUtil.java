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
package com.espertech.esper.common.internal.epl.lookupplan;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.join.queryplan.CoercionDesc;
import com.espertech.esper.common.internal.epl.lookup.*;

import java.util.List;

public class SubordinateTableLookupStrategyUtil {

    public static SubordTableLookupStrategyFactoryForge getLookupStrategy(EventType[] outerStreamTypesZeroIndexed,
                                                                          List<SubordPropHashKeyForge> hashKeys,
                                                                          CoercionDesc hashKeyCoercionTypes,
                                                                          MultiKeyClassRef hashMultikeyClasses,
                                                                          List<SubordPropRangeKeyForge> rangeKeys,
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
        SubordTableLookupStrategyFactoryForge lookupStrategy;
        if (inKeywordSingleIdxKeys != null) {
            lookupStrategy = new SubordInKeywordSingleTableLookupStrategyFactoryForge(isNWOnTrigger, numStreamsTotal, inKeywordSingleIdxKeys);
        } else if (inKeywordMultiIdxKey != null) {
            lookupStrategy = new SubordInKeywordMultiTableLookupStrategyFactoryForge(isNWOnTrigger, numStreamsTotal, inKeywordMultiIdxKey);
        } else if (hashKeys.isEmpty() && rangeKeys.isEmpty()) {
            lookupStrategy = new SubordFullTableScanLookupStrategyFactoryForge();
        } else if (hashKeys.size() > 0 && rangeKeys.isEmpty()) {
            lookupStrategy = new SubordHashedTableLookupStrategyFactoryForge(isNWOnTrigger, numStreamsTotal, hashKeys, hashKeyCoercionTypes, isStrictKeys, hashStrictKeys, hashStrictKeyStreams, outerStreamTypesZeroIndexed, hashMultikeyClasses);
        } else if (hashKeys.size() == 0 && rangeKeys.size() == 1) {
            lookupStrategy = new SubordSortedTableLookupStrategyFactoryForge(isNWOnTrigger, numStreamsTotal, rangeKeys.get(0), rangeKeyCoercionTypes);
        } else {
            lookupStrategy = new SubordCompositeTableLookupStrategyFactoryForge(isNWOnTrigger, numStreamsTotal, hashKeys, hashKeyCoercionTypes.getCoercionTypes(),
                    hashMultikeyClasses, rangeKeys, rangeKeyCoercionTypes.getCoercionTypes());
        }
        return lookupStrategy;
    }
}
