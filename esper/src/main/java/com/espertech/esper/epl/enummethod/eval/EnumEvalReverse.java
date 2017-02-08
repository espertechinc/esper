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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class EnumEvalReverse implements EnumEval {

    private int numStreams;

    public EnumEvalReverse(int numStreams) {
        this.numStreams = numStreams;
    }

    public int getStreamNumSize() {
        return numStreams;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection target, boolean isNewData, ExprEvaluatorContext context) {
        if (target.isEmpty()) {
            return target;
        }

        ArrayList<Object> result = new ArrayList<Object>(target.size());
        result.addAll(target);
        Collections.reverse(result);
        return result;
    }
}
