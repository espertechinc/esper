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
package com.espertech.esper.common.internal.epl.output.view;

import com.espertech.esper.common.client.EventPropertyValueGetter;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.compile.stage1.spec.SelectClauseStreamSelectorEnum;
import com.espertech.esper.common.internal.epl.expression.time.eval.TimePeriodCompute;
import com.espertech.esper.common.internal.epl.output.condition.OutputConditionFactory;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorOutputConditionType;

/**
 * A view that handles the "output snapshot" keyword in output rate stabilizing.
 */
public class OutputProcessViewConditionSpec {
    private EventType resultEventType;
    private OutputStrategyPostProcessFactory postProcessFactory;
    private OutputConditionFactory outputConditionFactory;
    private int streamCount;
    private ResultSetProcessorOutputConditionType conditionType;
    private boolean terminable;
    private boolean isUnaggregatedUngrouped;
    private SelectClauseStreamSelectorEnum selectClauseStreamSelector;
    private boolean isDistinct;
    private EventPropertyValueGetter distinctKeyGetter;
    private boolean hasAfter;
    private TimePeriodCompute afterTimePeriod;
    private Integer afterConditionNumberOfEvents;
    private EventType[] eventTypes;

    public OutputConditionFactory getOutputConditionFactory() {
        return outputConditionFactory;
    }

    public void setOutputConditionFactory(OutputConditionFactory outputConditionFactory) {
        this.outputConditionFactory = outputConditionFactory;
    }

    public int getStreamCount() {
        return streamCount;
    }

    public void setStreamCount(int streamCount) {
        this.streamCount = streamCount;
    }

    public ResultSetProcessorOutputConditionType getConditionType() {
        return conditionType;
    }

    public void setConditionType(ResultSetProcessorOutputConditionType conditionType) {
        this.conditionType = conditionType;
    }

    public boolean isTerminable() {
        return terminable;
    }

    public void setTerminable(boolean terminable) {
        this.terminable = terminable;
    }

    public boolean isHasAfter() {
        return hasAfter;
    }

    public void setHasAfter(boolean hasAfter) {
        this.hasAfter = hasAfter;
    }

    public boolean isUnaggregatedUngrouped() {
        return isUnaggregatedUngrouped;
    }

    public void setUnaggregatedUngrouped(boolean unaggregatedUngrouped) {
        isUnaggregatedUngrouped = unaggregatedUngrouped;
    }

    public SelectClauseStreamSelectorEnum getSelectClauseStreamSelector() {
        return selectClauseStreamSelector;
    }

    public void setSelectClauseStreamSelector(SelectClauseStreamSelectorEnum selectClauseStreamSelector) {
        this.selectClauseStreamSelector = selectClauseStreamSelector;
    }

    public boolean isDistinct() {
        return isDistinct;
    }

    public void setDistinct(boolean distinct) {
        isDistinct = distinct;
    }

    public TimePeriodCompute getAfterTimePeriod() {
        return afterTimePeriod;
    }

    public void setAfterTimePeriod(TimePeriodCompute afterTimePeriod) {
        this.afterTimePeriod = afterTimePeriod;
    }

    public Integer getAfterConditionNumberOfEvents() {
        return afterConditionNumberOfEvents;
    }

    public void setAfterConditionNumberOfEvents(Integer afterConditionNumberOfEvents) {
        this.afterConditionNumberOfEvents = afterConditionNumberOfEvents;
    }

    public OutputStrategyPostProcessFactory getPostProcessFactory() {
        return postProcessFactory;
    }

    public void setPostProcessFactory(OutputStrategyPostProcessFactory postProcessFactory) {
        this.postProcessFactory = postProcessFactory;
    }

    public EventType getResultEventType() {
        return resultEventType;
    }

    public void setResultEventType(EventType resultEventType) {
        this.resultEventType = resultEventType;
    }

    public EventType[] getEventTypes() {
        return eventTypes;
    }

    public void setEventTypes(EventType[] eventTypes) {
        this.eventTypes = eventTypes;
    }

    public EventPropertyValueGetter getDistinctKeyGetter() {
        return distinctKeyGetter;
    }

    public void setDistinctKeyGetter(EventPropertyValueGetter distinctKeyGetter) {
        this.distinctKeyGetter = distinctKeyGetter;
    }
}
