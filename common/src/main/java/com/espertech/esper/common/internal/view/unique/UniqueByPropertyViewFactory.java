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
package com.espertech.esper.common.internal.view.unique;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.view.core.*;

/**
 * Factory for {@link UniqueByPropertyView} instances.
 */
public class UniqueByPropertyViewFactory implements DataWindowViewFactory {
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
        return new UniqueByPropertyView(this, agentInstanceViewFactoryContext);
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

    public Class[] getCriteriaTypes() {
        return criteriaTypes;
    }

    public void setCriteriaTypes(Class[] criteriaTypes) {
        this.criteriaTypes = criteriaTypes;
    }

    public String getViewName() {
        return ViewEnum.UNIQUE_BY_PROPERTY.getName();
    }

    public DataInputOutputSerde<Object> getKeySerde() {
        return keySerde;
    }

    public void setKeySerde(DataInputOutputSerde<Object> keySerde) {
        this.keySerde = keySerde;
    }
}
