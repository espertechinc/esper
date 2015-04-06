/**************************************************************************************
 * Copyright (C) 2006-2015 EsperTech Inc. All rights reserved.                        *
 * http://www.espertech.com/esper                                                          *
 * http://www.espertech.com                                                           *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the GPL license       *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package com.espertech.esper.epl.view;

import com.espertech.esper.core.context.util.AgentInstanceContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.core.service.StatementResultService;
import com.espertech.esper.epl.core.ResultSetProcessor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Factory for output process view that does not enforce any output policies and may simply
 * hand over events to child views, does not handle distinct.
 */
public class OutputProcessViewDirectFactory implements OutputProcessViewFactory
{
	private static final Log log = LogFactory.getLog(OutputProcessViewDirectFactory.class);

    private final StatementContext statementContext;
    private final StatementResultService statementResultService;
    protected final OutputStrategyPostProcessFactory postProcessFactory;

    public OutputProcessViewDirectFactory(StatementContext statementContext, OutputStrategyPostProcessFactory postProcessFactory) {
        this.statementContext = statementContext;
        this.statementResultService = statementContext.getStatementResultService();
        this.postProcessFactory = postProcessFactory;
    }

    public OutputProcessViewBase makeView(ResultSetProcessor resultSetProcessor, AgentInstanceContext agentInstanceContext) {
        if (postProcessFactory == null) {
            return new OutputProcessViewDirect(resultSetProcessor, this);
        }
        OutputStrategyPostProcess postProcess = postProcessFactory.make(agentInstanceContext);
        return new OutputProcessViewDirectPostProcess(resultSetProcessor, this, postProcess);
    }

    public StatementResultService getStatementResultService() {
        return statementResultService;
    }

    public StatementContext getStatementContext() {
        return statementContext;
    }
}
