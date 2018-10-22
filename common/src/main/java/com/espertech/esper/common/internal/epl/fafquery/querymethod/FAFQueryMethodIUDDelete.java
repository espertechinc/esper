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
package com.espertech.esper.common.internal.epl.fafquery.querymethod;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;
import com.espertech.esper.common.internal.epl.fafquery.processor.FireAndForgetInstance;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraph;

/**
 * Starts and provides the stop method for EPL statements.
 */
public class FAFQueryMethodIUDDelete extends FAFQueryMethodIUDBase {
    private QueryGraph queryGraph;
    private ExprEvaluator optionalWhereClause;

    public void setQueryGraph(QueryGraph queryGraph) {
        this.queryGraph = queryGraph;
    }

    public void setOptionalWhereClause(ExprEvaluator optionalWhereClause) {
        this.optionalWhereClause = optionalWhereClause;
    }

    protected EventBean[] execute(FireAndForgetInstance fireAndForgetProcessorInstance) {
        return fireAndForgetProcessorInstance.processDelete(this);
    }

    public QueryGraph getQueryGraph() {
        return queryGraph;
    }

    public ExprEvaluator getOptionalWhereClause() {
        return optionalWhereClause;
    }
}
