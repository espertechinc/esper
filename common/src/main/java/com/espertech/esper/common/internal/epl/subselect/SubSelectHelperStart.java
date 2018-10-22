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
package com.espertech.esper.common.internal.epl.subselect;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.context.activator.ViewableActivationResult;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.lookup.LookupStrategyDesc;
import com.espertech.esper.common.internal.epl.lookup.SubordTableLookupStrategy;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.util.*;

public class SubSelectHelperStart {

    public static Map<Integer, SubSelectFactoryResult> startSubselects(Map<Integer, SubSelectFactory> subselects, AgentInstanceContext agentInstanceContext, List<AgentInstanceStopCallback> stopCallbacks, boolean isRecoveringResilient) {
        if (subselects == null || subselects.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Integer, SubSelectFactoryResult> subselectStrategies = new HashMap<>();

        for (Map.Entry<Integer, SubSelectFactory> subselectEntry : subselects.entrySet()) {

            SubSelectFactory factory = subselectEntry.getValue();

            // activate viewable
            ViewableActivationResult subselectActivationResult = factory.getActivator().activate(agentInstanceContext, true, isRecoveringResilient);
            stopCallbacks.add(subselectActivationResult.getStopCallback());

            // apply returning the strategy instance
            SubSelectStrategyRealization realization = factory.getStrategyFactory().instantiate(subselectActivationResult.getViewable(), agentInstanceContext, stopCallbacks, subselectEntry.getKey(), isRecoveringResilient);

            // set aggregation
            final SubordTableLookupStrategy lookupStrategyDefault = realization.getLookupStrategy();
            SubselectAggregationPreprocessorBase aggregationPreprocessor = realization.getSubselectAggregationPreprocessor();

            // determine strategy
            SubordTableLookupStrategy lookupStrategy = lookupStrategyDefault;
            if (aggregationPreprocessor != null) {
                lookupStrategy = new SubordTableLookupStrategy() {
                    public Collection<EventBean> lookup(EventBean[] events, ExprEvaluatorContext context) {
                        Collection<EventBean> matchingEvents = lookupStrategyDefault.lookup(events, context);
                        aggregationPreprocessor.evaluate(events, matchingEvents, context);
                        return CollectionUtil.SINGLE_NULL_ROW_EVENT_SET;
                    }

                    public String toQueryPlan() {
                        return lookupStrategyDefault.toQueryPlan();
                    }

                    public LookupStrategyDesc getStrategyDesc() {
                        return lookupStrategyDefault.getStrategyDesc();
                    }
                };
            }

            SubSelectFactoryResult instance = new SubSelectFactoryResult(subselectActivationResult, realization, lookupStrategy);
            subselectStrategies.put(subselectEntry.getKey(), instance);
        }

        return subselectStrategies;
    }
}
