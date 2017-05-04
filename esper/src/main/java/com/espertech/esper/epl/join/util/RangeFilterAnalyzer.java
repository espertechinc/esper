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
package com.espertech.esper.epl.join.util;

import com.espertech.esper.epl.expression.core.ExprIdentNode;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.join.plan.QueryGraph;
import com.espertech.esper.epl.join.plan.QueryGraphRangeEnum;

public class RangeFilterAnalyzer {

    public static void apply(ExprNode target, ExprNode start, ExprNode end,
                             boolean includeStart, boolean includeEnd, boolean isNot,
                             QueryGraph queryGraph) {

        QueryGraphRangeEnum rangeOp = QueryGraphRangeEnum.getRangeOp(includeStart, includeEnd, isNot);

        if (((target instanceof ExprIdentNode)) &&
                ((start instanceof ExprIdentNode)) &&
                ((end instanceof ExprIdentNode))) {
            ExprIdentNode identNodeValue = (ExprIdentNode) target;
            ExprIdentNode identNodeStart = (ExprIdentNode) start;
            ExprIdentNode identNodeEnd = (ExprIdentNode) end;

            int keyStreamStart = identNodeStart.getStreamId();
            int keyStreamEnd = identNodeEnd.getStreamId();
            int valueStream = identNodeValue.getStreamId();
            queryGraph.addRangeStrict(keyStreamStart, identNodeStart, keyStreamEnd,
                    identNodeEnd, valueStream,
                    identNodeValue, rangeOp);
            return;
        }

        // handle constant-compare or transformation case
        if (target instanceof ExprIdentNode) {
            ExprIdentNode identNode = (ExprIdentNode) target;
            int indexedStream = identNode.getStreamId();

            EligibilityDesc eligibilityStart = EligibilityUtil.verifyInputStream(start, indexedStream);
            if (!eligibilityStart.getEligibility().isEligible()) {
                return;
            }
            EligibilityDesc eligibilityEnd = EligibilityUtil.verifyInputStream(end, indexedStream);
            if (!eligibilityEnd.getEligibility().isEligible()) {
                return;
            }

            queryGraph.addRangeExpr(indexedStream, identNode, start, eligibilityStart.getStreamNum(), end, eligibilityEnd.getStreamNum(), rangeOp);
        }
    }
}
