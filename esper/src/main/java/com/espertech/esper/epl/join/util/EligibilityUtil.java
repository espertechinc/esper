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

import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.visitor.ExprNodeIdentifierCollectVisitor;

import java.util.Set;

public class EligibilityUtil {

    public static EligibilityDesc verifyInputStream(ExprNode expression, int indexedStream) {
        ExprNodeIdentifierCollectVisitor visitor = new ExprNodeIdentifierCollectVisitor();
        expression.accept(visitor);
        Set<Integer> inputStreamsRequired = visitor.getStreamsRequired();
        if (inputStreamsRequired.size() > 1) {  // multi-stream dependency no optimization (i.e. a+b=c)
            return new EligibilityDesc(Eligibility.INELIGIBLE, null);
        }
        if (inputStreamsRequired.size() == 1 && inputStreamsRequired.iterator().next() == indexedStream) {  // self-compared no optimization
            return new EligibilityDesc(Eligibility.INELIGIBLE, null);
        }
        if (inputStreamsRequired.isEmpty()) {
            return new EligibilityDesc(Eligibility.REQUIRE_NONE, null);
        }
        return new EligibilityDesc(Eligibility.REQUIRE_ONE, inputStreamsRequired.iterator().next());
    }
}
