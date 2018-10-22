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

import com.espertech.esper.common.internal.epl.pattern.core.EvalNodeBase;
import com.espertech.esper.common.internal.epl.pattern.core.EvalStateNode;
import com.espertech.esper.common.internal.epl.pattern.core.Evaluator;
import com.espertech.esper.common.internal.epl.pattern.core.PatternAgentInstanceContext;
import com.espertech.esper.common.internal.filterspec.FilterAddendumUtil;
import com.espertech.esper.common.internal.filterspec.FilterValueSetParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents a filter of events in the evaluation tree representing any event expressions.
 */
public class EvalFilterNode extends EvalNodeBase {
    protected final EvalFilterFactoryNode factoryNode;
    private final FilterValueSetParam[][] addendumFilters;

    public EvalFilterNode(PatternAgentInstanceContext context, EvalFilterFactoryNode factoryNode) {
        super(context);
        this.factoryNode = factoryNode;

        FilterValueSetParam[][] addendum = null;
        if (context.getAgentInstanceContext().getAgentInstanceFilterProxy() != null) {
            addendum = context.getAgentInstanceContext().getAgentInstanceFilterProxy().getAddendumFilters(factoryNode.getFilterSpec(), context.getAgentInstanceContext());
        }
        FilterValueSetParam[][] contextPathAddendum = context.getFilterAddendumForContextPath(factoryNode.getFilterSpec());
        if (contextPathAddendum != null) {
            if (addendum == null) {
                addendum = contextPathAddendum;
            } else {
                addendum = FilterAddendumUtil.multiplyAddendum(addendum, contextPathAddendum);
            }
        }
        this.addendumFilters = addendum;
    }

    public EvalFilterFactoryNode getFactoryNode() {
        return factoryNode;
    }

    public FilterValueSetParam[][] getAddendumFilters() {
        return addendumFilters;
    }

    public EvalStateNode newState(Evaluator parentNode) {
        if (getContext().getConsumptionHandler() != null) {
            return new EvalFilterStateNodeConsumeImpl(parentNode, this);
        }
        return new EvalFilterStateNode(parentNode, this);
    }

    private static final Logger log = LoggerFactory.getLogger(EvalFilterNode.class);
}
