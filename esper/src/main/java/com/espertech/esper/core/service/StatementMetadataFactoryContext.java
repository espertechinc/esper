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
package com.espertech.esper.core.service;

import com.espertech.esper.client.soda.EPStatementObjectModel;
import com.espertech.esper.epl.spec.StatementSpecRaw;

/**
 * Statement metadata factory context.
 */
public class StatementMetadataFactoryContext {
    private final String statementName;
    private final int statementId;
    private final StatementContext statementContext;
    private final StatementSpecRaw statementSpec;
    private final String expression;
    private final boolean isPattern;
    private final EPStatementObjectModel optionalModel;

    public StatementMetadataFactoryContext(String statementName, int statementId, StatementContext statementContext, StatementSpecRaw statementSpec, String expression, boolean pattern, EPStatementObjectModel optionalModel) {
        this.statementName = statementName;
        this.statementId = statementId;
        this.statementContext = statementContext;
        this.statementSpec = statementSpec;
        this.expression = expression;
        isPattern = pattern;
        this.optionalModel = optionalModel;
    }

    public String getStatementName() {
        return statementName;
    }

    public int getStatementId() {
        return statementId;
    }

    public StatementContext getStatementContext() {
        return statementContext;
    }

    public StatementSpecRaw getStatementSpec() {
        return statementSpec;
    }

    public boolean isPattern() {
        return isPattern;
    }

    public String getExpression() {
        return expression;
    }

    public EPStatementObjectModel getOptionalModel() {
        return optionalModel;
    }
}
