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
package com.espertech.esper.core.start;

import com.espertech.esper.core.service.EPServicesContext;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.core.service.speccompiled.StatementSpecCompiled;
import com.espertech.esper.view.ViewProcessingException;

/**
 * Starts and provides the stop method for EPL statements.
 */
public interface EPStatementStartMethod {
    final static int DEFAULT_AGENT_INSTANCE_ID = -1;

    /**
     * Starts the EPL statement.
     *
     * @param statementContext      statement level services
     * @param isNewStatement        indicator whether the statement is new or a stop-restart statement
     * @param isRecoveringStatement true to indicate the statement is in the process of being recovered
     * @param isRecoveringResilient true to indicate the statement is in the process of being recovered and that statement is resilient    @throws com.espertech.esper.epl.expression.ExprValidationException when the expression validation fails
     * @param services              services
     * @return a viewable to attach to for listening to events, and a stop method to invoke to clean up
     * @throws ExprValidationException                          validation exception
     * @throws com.espertech.esper.view.ViewProcessingException when views cannot be started
     */
    public EPStatementStartResult start(EPServicesContext services, StatementContext statementContext, boolean isNewStatement, boolean isRecoveringStatement, boolean isRecoveringResilient)
            throws ExprValidationException, ViewProcessingException;

    public StatementSpecCompiled getStatementSpec();
}
