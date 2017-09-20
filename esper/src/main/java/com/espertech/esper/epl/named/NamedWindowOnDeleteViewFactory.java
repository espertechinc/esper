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
package com.espertech.esper.epl.named;

import com.espertech.esper.client.EventType;
import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.StatementResultService;
import com.espertech.esper.epl.core.resultset.core.ResultSetProcessor;
import com.espertech.esper.epl.lookup.SubordWMatchExprLookupStrategy;

/**
 * View for the on-delete statement that handles removing events from a named window.
 */
public class NamedWindowOnDeleteViewFactory extends NamedWindowOnExprBaseViewFactory {
    private final StatementResultService statementResultService;

    public NamedWindowOnDeleteViewFactory(EventType namedWindowEventType, StatementResultService statementResultService) {
        super(namedWindowEventType);
        this.statementResultService = statementResultService;
    }

    public StatementResultService getStatementResultService() {
        return statementResultService;
    }

    public NamedWindowOnExprBaseView make(SubordWMatchExprLookupStrategy lookupStrategy, NamedWindowRootViewInstance namedWindowRootViewInstance, AgentInstanceContext agentInstanceContext, ResultSetProcessor resultSetProcessor) {
        return new NamedWindowOnDeleteView(lookupStrategy, namedWindowRootViewInstance, agentInstanceContext, this);
    }
}
