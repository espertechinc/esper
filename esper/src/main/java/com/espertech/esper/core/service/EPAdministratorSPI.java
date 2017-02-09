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

import com.espertech.esper.client.EPAdministrator;
import com.espertech.esper.client.EPException;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.soda.*;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.spec.StatementSpecRaw;
import com.espertech.esper.pattern.EvalFactoryNode;

/**
 * Administrative SPI.
 */
public interface EPAdministratorSPI extends EPAdministrator {
    /**
     * Compile expression.
     *
     * @param expression to compile
     * @return compiled expression
     * @throws EPException if compile failed
     */
    public ExprNode compileExpression(String expression) throws EPException;

    /**
     * Compile expression.
     *
     * @param expression to compile
     * @return compiled expression
     * @throws EPException if compile failed
     */
    public Expression compileExpressionToSODA(String expression) throws EPException;

    /**
     * Compile pattern.
     *
     * @param expression to compile
     * @return compiled expression
     * @throws EPException if compile failed
     */
    public EvalFactoryNode compilePatternToNode(String expression) throws EPException;

    /**
     * Compile pattern.
     *
     * @param expression to compile
     * @return compiled expression
     * @throws EPException if compile failed
     */
    public PatternExpr compilePatternToSODA(String expression) throws EPException;

    /**
     * Compile pattern.
     *
     * @param expression to compile
     * @return compiled expression
     * @throws EPException if compile failed
     */
    public EPStatementObjectModel compilePatternToSODAModel(String expression) throws EPException;

    /**
     * Compile annotation expressions.
     *
     * @param annotationExpression to compile
     * @return model representation
     */
    public AnnotationPart compileAnnotationToSODA(String annotationExpression);

    /**
     * Compile match recognize pattern expression.
     *
     * @param matchRecogPatternExpression to compile
     * @return model representation
     */
    public MatchRecognizeRegEx compileMatchRecognizePatternToSODA(String matchRecogPatternExpression);

    /**
     * Destroy the administrative interface.
     */
    public void destroy();

    public StatementSpecRaw compileEPLToRaw(String epl);

    public EPStatementObjectModel mapRawToSODA(StatementSpecRaw raw);

    public StatementSpecRaw mapSODAToRaw(EPStatementObjectModel model);

    public EPStatement createEPLStatementId(String eplStatement, String statementName, Object userObject, int statementId) throws EPException;

    public EPStatement createModelStatementId(EPStatementObjectModel sodaStatement, String statementName, Object userObject, int statementId) throws EPException;

    public EPStatement createPatternStatementId(String pattern, String statementName, Object userObject, int statementId) throws EPException;

    public EPStatement createPreparedEPLStatementId(EPPreparedStatementImpl prepared, String statementName, Object userObject, int statementId) throws EPException;

    public String getStatementNameForId(int statementId);
}
