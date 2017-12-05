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
package com.espertech.esper.core.context.mgr;

import com.espertech.esper.core.context.factory.StatementAgentInstanceFactory;
import com.espertech.esper.core.context.subselect.SubSelectStrategyCollection;
import com.espertech.esper.core.context.util.ContextMergeView;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.agg.service.common.AggregationServiceAggExpressionDesc;
import com.espertech.esper.core.service.speccompiled.StatementSpecCompiled;

import java.util.List;

public class ContextManagedStatementSelectDesc extends ContextControllerStatementBase {

    private final List<AggregationServiceAggExpressionDesc> aggregationExpressions;
    private final SubSelectStrategyCollection subSelectPrototypeCollection;

    public ContextManagedStatementSelectDesc(StatementSpecCompiled statementSpec, StatementContext statementContext, ContextMergeView mergeView, StatementAgentInstanceFactory factory, List<AggregationServiceAggExpressionDesc> aggregationExpressions, SubSelectStrategyCollection subSelectPrototypeCollection) {
        super(statementSpec, statementContext, mergeView, factory);
        this.aggregationExpressions = aggregationExpressions;
        this.subSelectPrototypeCollection = subSelectPrototypeCollection;
    }

    public List<AggregationServiceAggExpressionDesc> getAggregationExpressions() {
        return aggregationExpressions;
    }

    public SubSelectStrategyCollection getSubSelectPrototypeCollection() {
        return subSelectPrototypeCollection;
    }
}
