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
package com.espertech.esper.common.internal.view.derived;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.event.core.EventBeanTypedEventFactory;
import com.espertech.esper.common.internal.view.core.ViewFactory;
import com.espertech.esper.common.internal.view.core.ViewForgeEnv;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A view that calculates correlation on two fields. The view uses internally a {@link BaseStatisticsBean}
 * instance for the calculations, it also returns this bean as the result.
 * This class accepts most of its behaviour from its parent, {@link BaseBivariateStatisticsView}. It adds
 * the usage of the correlation bean and the appropriate schema.
 */
public class CorrelationView extends BaseBivariateStatisticsView {
    public CorrelationView(ViewFactory viewFactory, AgentInstanceContext agentInstanceContext, ExprEvaluator xExpressionEval, ExprEvaluator yExpressionEval, EventType eventType, StatViewAdditionalPropsEval additionalProps) {
        super(viewFactory, agentInstanceContext, xExpressionEval, yExpressionEval, eventType, additionalProps);
    }

    public EventBean populateMap(BaseStatisticsBean baseStatisticsBean,
                                 EventBeanTypedEventFactory eventAdapterService,
                                 EventType eventType,
                                 StatViewAdditionalPropsEval additionalProps,
                                 Object[] decoration) {
        return doPopulateMap(baseStatisticsBean, eventAdapterService, eventType, additionalProps, decoration);
    }

    /**
     * Populate bean.
     *
     * @param baseStatisticsBean  results
     * @param eventAdapterService event wrapping
     * @param eventType           type to produce
     * @param additionalProps     addition properties
     * @param decoration          decoration values
     * @return bean
     */
    public static EventBean doPopulateMap(BaseStatisticsBean baseStatisticsBean,
                                          EventBeanTypedEventFactory eventAdapterService,
                                          EventType eventType,
                                          StatViewAdditionalPropsEval additionalProps,
                                          Object[] decoration) {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put(ViewFieldEnum.CORRELATION__CORRELATION.getName(), baseStatisticsBean.getCorrelation());
        if (additionalProps != null) {
            additionalProps.addProperties(result, decoration);
        }
        return eventAdapterService.adapterForTypedMap(result, eventType);
    }

    public EventType getEventType() {
        return eventType;
    }

    protected static EventType createEventType(StatViewAdditionalPropsForge additionalProps, ViewForgeEnv viewForgeEnv, int streamNum) {
        LinkedHashMap<String, Object> eventTypeMap = new LinkedHashMap<String, Object>();
        eventTypeMap.put(ViewFieldEnum.CORRELATION__CORRELATION.getName(), Double.class);
        StatViewAdditionalPropsForge.addCheckDupProperties(eventTypeMap, additionalProps,
                ViewFieldEnum.CORRELATION__CORRELATION);
        return DerivedViewTypeUtil.newType("correlview", eventTypeMap, viewForgeEnv, streamNum);
    }
}
