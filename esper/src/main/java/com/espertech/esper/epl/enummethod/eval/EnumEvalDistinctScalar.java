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
package com.espertech.esper.epl.enummethod.eval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class EnumEvalDistinctScalar extends EnumEvalBase implements EnumEval {

    public EnumEvalDistinctScalar(int streamCountIncoming) {
        super(streamCountIncoming);
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection target, boolean isNewData, ExprEvaluatorContext context) {
        if (target == null || target.size() < 2) {
            return target;
        }

        if (target instanceof Set) {
            return target;
        }

        Set<Object> set = new LinkedHashSet<Object>();
        for (Object entry : target) {
            set.add(entry);
        }
        return set;
    }
}
