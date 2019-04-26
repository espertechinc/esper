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
package com.espertech.esper.common.internal.view.groupwin;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.view.core.*;

/**
 * Factory for {@link GroupByView} instances.
 */
public class GroupByViewFactory implements ViewFactory {

    protected ViewFactory[] groupeds;
    protected ExprEvaluator criteriaEval;
    protected String[] propertyNames;
    protected Class[] criteriaTypes;
    protected DataInputOutputSerde<Object> keySerde;
    protected EventType eventType;
    protected boolean addingProperties;  // when adding properties to the grouped-views output
    protected boolean isReclaimAged;
    protected long reclaimMaxAge;
    protected long reclaimFrequency;

    public void init(ViewFactoryContext viewFactoryContext, EPStatementInitServices services) {
        if (groupeds == null) {
            throw new IllegalStateException("Grouped views not provided");
        }
        for (ViewFactory grouped : groupeds) {
            grouped.init(viewFactoryContext, services);
        }
    }

    public View makeView(AgentInstanceViewFactoryChainContext agentInstanceViewFactoryContext) {
        if (isReclaimAged) {
            return new GroupByViewReclaimAged(this, agentInstanceViewFactoryContext);
        }
        return new GroupByViewImpl(this, agentInstanceViewFactoryContext);
    }

    public EventType getEventType() {
        return eventType;
    }

    public boolean isReclaimAged() {
        return isReclaimAged;
    }

    public long getReclaimMaxAge() {
        return reclaimMaxAge;
    }

    public long getReclaimFrequency() {
        return reclaimFrequency;
    }

    public String[] getPropertyNames() {
        return propertyNames;
    }

    public void setPropertyNames(String[] propertyNames) {
        this.propertyNames = propertyNames;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public void setReclaimAged(boolean reclaimAged) {
        isReclaimAged = reclaimAged;
    }

    public void setReclaimMaxAge(long reclaimMaxAge) {
        this.reclaimMaxAge = reclaimMaxAge;
    }

    public void setReclaimFrequency(long reclaimFrequency) {
        this.reclaimFrequency = reclaimFrequency;
    }

    public void setGroupeds(ViewFactory[] groupeds) {
        this.groupeds = groupeds;
    }

    public ViewFactory[] getGroupeds() {
        return groupeds;
    }

    public void setAddingProperties(boolean addingProperties) {
        this.addingProperties = addingProperties;
    }

    public boolean isAddingProperties() {
        return addingProperties;
    }

    public Class[] getCriteriaTypes() {
        return criteriaTypes;
    }

    public void setCriteriaTypes(Class[] criteriaTypes) {
        this.criteriaTypes = criteriaTypes;
    }

    public String getViewName() {
        return ViewEnum.GROUP_PROPERTY.getName();
    }

    public ExprEvaluator getCriteriaEval() {
        return criteriaEval;
    }

    public void setCriteriaEval(ExprEvaluator criteriaEval) {
        this.criteriaEval = criteriaEval;
    }

    public DataInputOutputSerde<Object> getKeySerde() {
        return keySerde;
    }

    public void setKeySerde(DataInputOutputSerde<Object> keySerde) {
        this.keySerde = keySerde;
    }
}
