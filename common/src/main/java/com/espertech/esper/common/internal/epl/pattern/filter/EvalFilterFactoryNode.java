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
package com.espertech.esper.common.internal.epl.pattern.filter;

import com.espertech.esper.common.internal.epl.pattern.core.EvalFactoryNodeBase;
import com.espertech.esper.common.internal.epl.pattern.core.EvalFactoryNodeVisitor;
import com.espertech.esper.common.internal.epl.pattern.core.EvalNode;
import com.espertech.esper.common.internal.epl.pattern.core.PatternAgentInstanceContext;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;

/**
 * This class represents a filter of events in the evaluation tree representing any event expressions.
 */
public class EvalFilterFactoryNode extends EvalFactoryNodeBase {
    private FilterSpecActivatable filterSpec;
    private String eventAsName;
    private Integer consumptionLevel;
    private int eventAsTagNumber;

    public void setFilterSpec(FilterSpecActivatable filterSpec) {
        this.filterSpec = filterSpec;
    }

    public void setEventAsName(String eventAsName) {
        this.eventAsName = eventAsName;
    }

    public void setConsumptionLevel(Integer consumptionLevel) {
        this.consumptionLevel = consumptionLevel;
    }

    public void setEventAsTagNumber(int eventAsTagNumber) {
        this.eventAsTagNumber = eventAsTagNumber;
    }

    public EvalNode makeEvalNode(PatternAgentInstanceContext agentInstanceContext, EvalNode parentNode) {
        return new EvalFilterNode(agentInstanceContext, this);
    }

    public FilterSpecActivatable getFilterSpec() {
        return filterSpec;
    }

    public String getEventAsName() {
        return eventAsName;
    }

    public Integer getConsumptionLevel() {
        return consumptionLevel;
    }

    public int getEventAsTagNumber() {
        return eventAsTagNumber;
    }

    public boolean isStateful() {
        return false;
    }

    public boolean isFilterChildNonQuitting() {
        return false;
    }

    public void accept(EvalFactoryNodeVisitor visitor) {
        visitor.visit(this);
    }
}
