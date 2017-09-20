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
import com.espertech.esper.epl.updatehelper.EventBeanUpdateHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * View for the on-delete statement that handles removing events from a named window.
 */
public class NamedWindowOnUpdateViewFactory extends NamedWindowOnExprBaseViewFactory {
    private static final Logger log = LoggerFactory.getLogger(NamedWindowOnUpdateViewFactory.class);
    private final StatementResultService statementResultService;
    private final EventBeanUpdateHelper updateHelper;

    public NamedWindowOnUpdateViewFactory(EventType namedWindowEventType, StatementResultService statementResultService, EventBeanUpdateHelper updateHelper) {
        super(namedWindowEventType);
        this.statementResultService = statementResultService;
        this.updateHelper = updateHelper;
    }

    public NamedWindowOnExprBaseView make(SubordWMatchExprLookupStrategy lookupStrategy, NamedWindowRootViewInstance namedWindowRootViewInstance, AgentInstanceContext agentInstanceContext, ResultSetProcessor resultSetProcessor) {
        return new NamedWindowOnUpdateView(lookupStrategy, namedWindowRootViewInstance, agentInstanceContext, this);
    }

    public StatementResultService getStatementResultService() {
        return statementResultService;
    }

    public EventBeanUpdateHelper getUpdateHelper() {
        return updateHelper;
    }
}
