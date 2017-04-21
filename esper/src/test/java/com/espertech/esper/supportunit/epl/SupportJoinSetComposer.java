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
package com.espertech.esper.supportunit.epl;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.MultiKey;
import com.espertech.esper.collection.UniformPair;
import com.espertech.esper.core.context.factory.StatementAgentInstancePostLoadIndexVisitor;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.join.base.JoinSetComposer;

import java.util.Set;

public class SupportJoinSetComposer implements JoinSetComposer {
    private UniformPair<Set<MultiKey<EventBean>>> result;

    public SupportJoinSetComposer(UniformPair<Set<MultiKey<EventBean>>> result) {
        this.result = result;
    }

    public void init(EventBean[][] eventsPerStream, ExprEvaluatorContext exprEvaluatorContext) {
    }

    public UniformPair<Set<MultiKey<EventBean>>> join(EventBean[][] newDataPerStream, EventBean[][] oldDataPerStream, ExprEvaluatorContext exprEvaluatorContext) {
        return result;
    }

    public Set<MultiKey<EventBean>> staticJoin() {
        return null;
    }

    public void destroy() {
    }

    public void visitIndexes(StatementAgentInstancePostLoadIndexVisitor visitor) {
    }

    public boolean allowsInit() {
        return true;
    }
}
