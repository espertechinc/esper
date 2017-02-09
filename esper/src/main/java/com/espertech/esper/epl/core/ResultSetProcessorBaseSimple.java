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
package com.espertech.esper.epl.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.epl.spec.OutputLimitLimitType;
import com.espertech.esper.epl.view.OutputProcessViewConditionLastAllUnord;
import com.espertech.esper.event.EventBeanUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

/**
 * Result set processor for the simplest case: no aggregation functions used in the select clause, and no group-by.
 * <p>
 * The processor generates one row for each event entering (new event) and one row for each event leaving (old event).
 */
public abstract class ResultSetProcessorBaseSimple implements ResultSetProcessor {
    private static final Logger log = LoggerFactory.getLogger(ResultSetProcessorBaseSimple.class);

    public void clear() {
        // No need to clear state, there is no state held
    }

    public UniformPair<EventBean[]> processOutputLimitedJoin(List<UniformPair<Set<MultiKey<EventBean>>>> joinEventsSet, boolean generateSynthetic, OutputLimitLimitType outputLimitLimitType) {
        if (outputLimitLimitType != OutputLimitLimitType.LAST) {
            UniformPair<Set<MultiKey<EventBean>>> flattened = EventBeanUtility.flattenBatchJoin(joinEventsSet);
            return processJoinResult(flattened.getFirst(), flattened.getSecond(), generateSynthetic);
        }

        throw new IllegalStateException("Output last is provided by " + OutputProcessViewConditionLastAllUnord.class.getSimpleName());
    }

    public UniformPair<EventBean[]> processOutputLimitedView(List<UniformPair<EventBean[]>> viewEventsList, boolean generateSynthetic, OutputLimitLimitType outputLimitLimitType) {
        if (outputLimitLimitType != OutputLimitLimitType.LAST) {
            UniformPair<EventBean[]> pair = EventBeanUtility.flattenBatchStream(viewEventsList);
            return processViewResult(pair.getFirst(), pair.getSecond(), generateSynthetic);
        }

        throw new IllegalStateException("Output last is provided by " + OutputProcessViewConditionLastAllUnord.class.getSimpleName());
    }
}
