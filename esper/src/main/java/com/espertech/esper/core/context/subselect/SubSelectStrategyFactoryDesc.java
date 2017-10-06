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
package com.espertech.esper.core.context.subselect;

import com.espertech.esper.epl.agg.service.common.AggregationServiceFactoryDesc;
import com.espertech.esper.epl.expression.prev.ExprPreviousNode;
import com.espertech.esper.epl.expression.prior.ExprPriorNode;

import java.util.List;

/**
 * Entry holding lookup resource references for use by {@link SubSelectActivationCollection}.
 */
public class SubSelectStrategyFactoryDesc {
    private final SubSelectActivationHolder subSelectActivationHolder;
    private final SubSelectStrategyFactory factory;
    private final AggregationServiceFactoryDesc aggregationServiceFactoryDesc;
    private final List<ExprPriorNode> priorNodesList;
    private final List<ExprPreviousNode> prevNodesList;
    private final int subqueryNumber;

    public SubSelectStrategyFactoryDesc(SubSelectActivationHolder subSelectActivationHolder, SubSelectStrategyFactory factory, AggregationServiceFactoryDesc aggregationServiceFactoryDesc, List<ExprPriorNode> priorNodesList, List<ExprPreviousNode> prevNodesList, int subqueryNumber) {
        this.subSelectActivationHolder = subSelectActivationHolder;
        this.factory = factory;
        this.aggregationServiceFactoryDesc = aggregationServiceFactoryDesc;
        this.priorNodesList = priorNodesList;
        this.prevNodesList = prevNodesList;
        this.subqueryNumber = subqueryNumber;
    }

    public SubSelectActivationHolder getSubSelectActivationHolder() {
        return subSelectActivationHolder;
    }

    public SubSelectStrategyFactory getFactory() {
        return factory;
    }

    public AggregationServiceFactoryDesc getAggregationServiceFactoryDesc() {
        return aggregationServiceFactoryDesc;
    }

    public List<ExprPriorNode> getPriorNodesList() {
        return priorNodesList;
    }

    public List<ExprPreviousNode> getPrevNodesList() {
        return prevNodesList;
    }

    public int getSubqueryNumber() {
        return subqueryNumber;
    }
}
