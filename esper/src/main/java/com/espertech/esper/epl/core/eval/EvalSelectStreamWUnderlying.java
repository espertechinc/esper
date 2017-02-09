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
package com.espertech.esper.epl.core.eval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.core.SelectExprProcessor;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.spec.SelectClauseStreamCompiledSpec;
import com.espertech.esper.epl.table.mgmt.TableMetadata;
import com.espertech.esper.event.DecoratingEventBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class EvalSelectStreamWUnderlying extends EvalSelectStreamBaseMap implements SelectExprProcessor {

    private static final Logger log = LoggerFactory.getLogger(EvalSelectStreamWUnderlying.class);

    private final List<SelectExprStreamDesc> unnamedStreams;
    private final boolean singleStreamWrapper;
    private final boolean underlyingIsFragmentEvent;
    private final int underlyingStreamNumber;
    private final EventPropertyGetter underlyingPropertyEventGetter;
    private final ExprEvaluator underlyingExprEvaluator;
    private final TableMetadata tableMetadata;

    public EvalSelectStreamWUnderlying(SelectExprContext selectExprContext,
                                       EventType resultEventType,
                                       List<SelectClauseStreamCompiledSpec> namedStreams,
                                       boolean usingWildcard,
                                       List<SelectExprStreamDesc> unnamedStreams,
                                       boolean singleStreamWrapper,
                                       boolean underlyingIsFragmentEvent,
                                       int underlyingStreamNumber,
                                       EventPropertyGetter underlyingPropertyEventGetter,
                                       ExprEvaluator underlyingExprEvaluator,
                                       TableMetadata tableMetadata) {
        super(selectExprContext, resultEventType, namedStreams, usingWildcard);
        this.unnamedStreams = unnamedStreams;
        this.singleStreamWrapper = singleStreamWrapper;
        this.underlyingIsFragmentEvent = underlyingIsFragmentEvent;
        this.underlyingStreamNumber = underlyingStreamNumber;
        this.underlyingPropertyEventGetter = underlyingPropertyEventGetter;
        this.underlyingExprEvaluator = underlyingExprEvaluator;
        this.tableMetadata = tableMetadata;
    }

    public EventBean processSpecific(Map<String, Object> props, EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        // In case of a wildcard and single stream that is itself a
        // wrapper bean, we also need to add the map properties
        if (singleStreamWrapper) {
            DecoratingEventBean wrapper = (DecoratingEventBean) eventsPerStream[0];
            if (wrapper != null) {
                Map<String, Object> map = wrapper.getDecoratingProperties();
                props.putAll(map);
            }
        }

        EventBean theEvent = null;
        if (underlyingIsFragmentEvent) {
            EventBean eventBean = eventsPerStream[underlyingStreamNumber];
            theEvent = (EventBean) eventBean.getFragment(unnamedStreams.get(0).getStreamSelected().getStreamName());
        } else if (underlyingPropertyEventGetter != null) {
            Object value = underlyingPropertyEventGetter.get(eventsPerStream[underlyingStreamNumber]);
            if (value != null) {
                theEvent = super.getSelectExprContext().getEventAdapterService().adapterForBean(value);
            }
        } else if (underlyingExprEvaluator != null) {
            Object value = underlyingExprEvaluator.evaluate(eventsPerStream, true, exprEvaluatorContext);
            if (value != null) {
                theEvent = super.getSelectExprContext().getEventAdapterService().adapterForBean(value);
            }
        } else {
            theEvent = eventsPerStream[underlyingStreamNumber];
            if (tableMetadata != null && theEvent != null) {
                theEvent = tableMetadata.getEventToPublic().convert(theEvent, eventsPerStream, isNewData, exprEvaluatorContext);
            }
        }

        // Using a wrapper bean since we cannot use the same event type else same-type filters match.
        // Wrapping it even when not adding properties is very inexpensive.
        return super.getSelectExprContext().getEventAdapterService().adapterForTypedWrapper(theEvent, props, super.getResultEventType());
    }
}
