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

import com.espertech.esper.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.epl.spec.StatementSpecRaw;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

/**
 * Interface for a factory class that makes statement context specific to a statement.
 */
public interface StatementContextFactory {
    public void setStmtEngineServices(EPServicesContext services);

    public StatementContext makeContext(int statementId,
                                        String statementName,
                                        String expression,
                                        StatementType statementType,
                                        EPServicesContext engineServices,
                                        Map<String, Object> optAdditionalContext,
                                        boolean isFireAndForget,
                                        Annotation[] annotations,
                                        EPIsolationUnitServices isolationUnitServices,
                                        boolean stateless,
                                        StatementSpecRaw statementSpecRaw,
                                        List<ExprSubselectNode> subselectNodes,
                                        boolean writesToTables,
                                        Object statementUserObject);
}
