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
package com.espertech.esper.epl.join.exec.sorted;

import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.join.plan.QueryGraphRangeEnum;
import com.espertech.esper.epl.join.plan.QueryGraphValueEntryRange;
import com.espertech.esper.epl.join.plan.QueryGraphValueEntryRangeIn;
import com.espertech.esper.epl.join.plan.QueryGraphValueEntryRangeRelOp;
import com.espertech.esper.epl.lookup.SubordPropRangeKey;

public class SortedAccessStrategyFactory {

    public static SortedAccessStrategy make(boolean isNWOnTrigger, int lookupStream, int numStreams, QueryGraphValueEntryRange rangeKeyPair, Class coercionType) {
        return make(isNWOnTrigger, lookupStream, numStreams, new SubordPropRangeKey(rangeKeyPair, coercionType));
    }

    public static SortedAccessStrategy make(boolean isNWOnTrigger, int lookupStream, int numStreams, SubordPropRangeKey streamRangeKey) {

        QueryGraphValueEntryRange rangeKeyPair = streamRangeKey.getRangeInfo();

        if (rangeKeyPair.getType().isRange()) {
            QueryGraphValueEntryRangeIn rangeIn = (QueryGraphValueEntryRangeIn) rangeKeyPair;
            ExprEvaluator startExpr = rangeIn.getExprStart().getForge().getExprEvaluator();
            ExprEvaluator endExpr = rangeIn.getExprEnd().getForge().getExprEvaluator();
            boolean includeStart = rangeKeyPair.getType().isIncludeStart();

            boolean includeEnd = rangeKeyPair.getType().isIncludeEnd();
            if (!rangeKeyPair.getType().isRangeInverted()) {
                return new SortedAccessStrategyRange(isNWOnTrigger, lookupStream, numStreams, startExpr, includeStart, endExpr, includeEnd, rangeIn.isAllowRangeReversal());
            } else {
                return new SortedAccessStrategyRangeInverted(isNWOnTrigger, lookupStream, numStreams, startExpr, includeStart, endExpr, includeEnd);
            }
        } else {
            QueryGraphValueEntryRangeRelOp relOp = (QueryGraphValueEntryRangeRelOp) rangeKeyPair;
            ExprEvaluator keyExpr = relOp.getExpression().getForge().getExprEvaluator();
            if (rangeKeyPair.getType() == QueryGraphRangeEnum.GREATER_OR_EQUAL) {
                return new SortedAccessStrategyGE(isNWOnTrigger, lookupStream, numStreams, keyExpr);
            } else if (rangeKeyPair.getType() == QueryGraphRangeEnum.GREATER) {
                return new SortedAccessStrategyGT(isNWOnTrigger, lookupStream, numStreams, keyExpr);
            } else if (rangeKeyPair.getType() == QueryGraphRangeEnum.LESS_OR_EQUAL) {
                return new SortedAccessStrategyLE(isNWOnTrigger, lookupStream, numStreams, keyExpr);
            } else if (rangeKeyPair.getType() == QueryGraphRangeEnum.LESS) {
                return new SortedAccessStrategyLT(isNWOnTrigger, lookupStream, numStreams, keyExpr);
            } else {
                throw new IllegalArgumentException("Comparison operator " + rangeKeyPair.getType() + " not supported");
            }
        }
    }
}
