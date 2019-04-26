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
package com.espertech.esper.common.internal.view.firstunique;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.view.core.*;

/**
 * Factory for {@link FirstUniqueByPropertyView} instances.
 */
public class FirstUniqueByPropertyViewFactory implements ViewFactory {
    protected ExprEvaluator criteriaEval;
    protected Class[] criteriaTypes;
    protected DataInputOutputSerde<Object> keySerde;
    protected EventType eventType;

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public void init(ViewFactoryContext viewFactoryContext, EPStatementInitServices services) {
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        return new FirstUniqueByPropertyView(this, agentInstanceViewFactoryContext);
    }

    public EventType getEventType() {
        return eventType;
    }

    public ExprEvaluator getCriteriaEval() {
        return criteriaEval;
    }

    public void setCriteriaEval(ExprEvaluator criteriaEval) {
        this.criteriaEval = criteriaEval;
    }

    public void setCriteriaTypes(Class[] criteriaTypes) {
        this.criteriaTypes = criteriaTypes;
    }

    public void setKeySerde(DataInputOutputSerde<Object> keySerde) {
        this.keySerde = keySerde;
    }

    public String getViewName() {
        return ViewEnum.UNIQUE_FIRST_BY_PROPERTY.getName();
    }
}
