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
import java.util.LinkedHashMap;
import java.util.Map;

public class EnumEvalMostLeastFrequentScalar extends EnumEvalBase implements EnumEval {

    private final boolean isMostFrequent;

    public EnumEvalMostLeastFrequentScalar(int streamCountIncoming, boolean isMostFrequent) {
        super(streamCountIncoming);
        this.isMostFrequent = isMostFrequent;
    }

    public Object evaluateEnumMethod(EventBean[] eventsLambda, Collection target, boolean isNewData, ExprEvaluatorContext context) {
        Map<Object, Integer> items = new LinkedHashMap<Object, Integer>();

        for (Object next : target) {
            Integer existing = items.get(next);

            if (existing == null) {
                existing = 1;
            } else {
                existing++;
            }
            items.put(next, existing);
        }

        return EnumEvalMostLeastFrequentEvent.getResult(items, isMostFrequent);
    }
}
