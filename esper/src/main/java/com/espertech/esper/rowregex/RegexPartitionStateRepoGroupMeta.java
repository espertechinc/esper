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
package com.espertech.esper.rowregex;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprNode;

public class RegexPartitionStateRepoGroupMeta {
    private final boolean hasInterval;
    private final ExprNode[] partitionExpressionNodes;
    private final ExprEvaluator[] partitionExpressions;
    private final ExprEvaluatorContext exprEvaluatorContext;
    private final EventBean[] eventsPerStream = new EventBean[1];

    public RegexPartitionStateRepoGroupMeta(boolean hasInterval, ExprNode[] partitionExpressionNodes, ExprEvaluator[] partitionExpressions, ExprEvaluatorContext exprEvaluatorContext) {
        this.hasInterval = hasInterval;
        this.partitionExpressionNodes = partitionExpressionNodes;
        this.partitionExpressions = partitionExpressions;
        this.exprEvaluatorContext = exprEvaluatorContext;
    }

    public boolean isHasInterval() {
        return hasInterval;
    }

    public ExprNode[] getPartitionExpressionNodes() {
        return partitionExpressionNodes;
    }

    public ExprEvaluator[] getPartitionExpressions() {
        return partitionExpressions;
    }

    public ExprEvaluatorContext getExprEvaluatorContext() {
        return exprEvaluatorContext;
    }

    public EventBean[] getEventsPerStream() {
        return eventsPerStream;
    }
}