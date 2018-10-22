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
package com.espertech.esper.common.internal.epl.join.exec.sorted;

import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphRangeEnum;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryRange;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryRangeIn;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryRangeRelOp;

public class SortedAccessStrategyFactory {

    public static SortedAccessStrategy make(boolean isNWOnTrigger, int lookupStream, int numStreams, QueryGraphValueEntryRange rangeKeyPair) {

        if (rangeKeyPair.getType().isRange()) {
            QueryGraphValueEntryRangeIn rangeIn = (QueryGraphValueEntryRangeIn) rangeKeyPair;
            ExprEvaluator startExpr = rangeIn.getExprStart();
            ExprEvaluator endExpr = rangeIn.getExprEnd();
            boolean includeStart = rangeKeyPair.getType().isIncludeStart();

            boolean includeEnd = rangeKeyPair.getType().isIncludeEnd();
            if (!rangeKeyPair.getType().isRangeInverted()) {
                return new SortedAccessStrategyRange(isNWOnTrigger, lookupStream, numStreams, startExpr, includeStart, endExpr, includeEnd, rangeIn.isAllowRangeReversal());
            } else {
                return new SortedAccessStrategyRangeInverted(isNWOnTrigger, lookupStream, numStreams, startExpr, includeStart, endExpr, includeEnd);
            }
        } else {
            QueryGraphValueEntryRangeRelOp relOp = (QueryGraphValueEntryRangeRelOp) rangeKeyPair;
            ExprEvaluator keyExpr = relOp.getExpression();
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
