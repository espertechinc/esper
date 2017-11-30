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

import com.espertech.esper.client.ConfigurationInformation;
import com.espertech.esper.client.EPStatementException;
import com.espertech.esper.client.EPStatementSyntaxException;
import com.espertech.esper.core.context.mgr.ContextManagementService;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.declexpr.ExprDeclaredService;
import com.espertech.esper.epl.generated.EsperEPL2GrammarParser;
import com.espertech.esper.epl.parse.*;
import com.espertech.esper.epl.spec.PatternStreamSpecRaw;
import com.espertech.esper.epl.spec.SelectClauseElementWildcard;
import com.espertech.esper.epl.spec.SelectClauseStreamSelectorEnum;
import com.espertech.esper.epl.spec.StatementSpecRaw;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.epl.variable.VariableService;
import com.espertech.esper.pattern.PatternNodeFactory;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.tree.Tree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for administrative interface.
 */
public class EPAdministratorHelper {
    private static ParseRuleSelector patternParseRule;
    private static ParseRuleSelector eplParseRule;

    static {
        patternParseRule = null;
        patternParseRule = new ParseRuleSelector() {
            public Tree invokeParseRule(EsperEPL2GrammarParser parser) throws RecognitionException {
                return parser.startPatternExpressionRule();
            }
        };

        eplParseRule = new ParseRuleSelector() {
            public Tree invokeParseRule(EsperEPL2GrammarParser parser) throws RecognitionException {
                return parser.startEPLExpressionRule();
            }
        };
    }

    /**
     * Compile an EPL statement.
     *
     * @param eplStatement            to compile
     * @param eplStatementForErrorMsg the statement to use for indicating error messages
     * @param addPleaseCheck          true to add please-check message text
     * @param statementName           the name of statement
     * @param services                engine services
     * @param defaultStreamSelector   stream selector
     * @return compiled statement
     */
    public static StatementSpecRaw compileEPL(String eplStatement, String eplStatementForErrorMsg, boolean addPleaseCheck, String statementName, EPServicesContext services, SelectClauseStreamSelectorEnum defaultStreamSelector) {
        return compileEPL(eplStatement, eplStatementForErrorMsg, addPleaseCheck, statementName, defaultStreamSelector,
                services.getEngineImportService(), services.getVariableService(), services.getEngineURI(), services.getConfigSnapshot(), services.getPatternNodeFactory(), services.getContextManagementService(), services.getExprDeclaredService(), services.getTableService());
    }

    public static StatementSpecRaw compileEPL(String eplStatement, String eplStatementForErrorMsg, boolean addPleaseCheck, String statementName, SelectClauseStreamSelectorEnum defaultStreamSelector,
                                              EngineImportService engineImportService,
                                              VariableService variableService,
                                              String engineURI,
                                              ConfigurationInformation configSnapshot,
                                              PatternNodeFactory patternNodeFactory,
                                              ContextManagementService contextManagementService,
                                              ExprDeclaredService exprDeclaredService,
                                              TableService tableService) {
        if (log.isDebugEnabled()) {
            log.debug(".createEPLStmt statementName=" + statementName + " eplStatement=" + eplStatement);
        }

        ParseResult parseResult = ParseHelper.parse(eplStatement, eplStatementForErrorMsg, addPleaseCheck, eplParseRule, true);
        Tree ast = parseResult.getTree();

        EPLTreeWalkerListener walker = new EPLTreeWalkerListener(parseResult.getTokenStream(), engineImportService, variableService, defaultStreamSelector, engineURI, configSnapshot, patternNodeFactory, contextManagementService, parseResult.getScripts(), exprDeclaredService, tableService);

        try {
            ParseHelper.walk(ast, walker, eplStatement, eplStatementForErrorMsg);
        } catch (ASTWalkException ex) {
            log.error(".createEPL Error validating expression", ex);
            throw new EPStatementException(ex.getMessage(), ex, eplStatementForErrorMsg);
        } catch (EPStatementSyntaxException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            String message = "Error in expression";
            log.debug(message, ex);
            throw new EPStatementException(getNullableErrortext(message, ex.getMessage()), ex, eplStatementForErrorMsg);
        }

        if (log.isDebugEnabled()) {
            ASTUtil.dumpAST(ast);
        }

        StatementSpecRaw raw = walker.getStatementSpec();
        raw.setExpressionNoAnnotations(parseResult.getExpressionWithoutAnnotations());
        return raw;
    }

    public static StatementSpecRaw compilePattern(String expression, String expressionForErrorMessage, boolean addPleaseCheck, EPServicesContext services, SelectClauseStreamSelectorEnum defaultStreamSelector) {
        // Parse
        ParseResult parseResult = ParseHelper.parse(expression, expressionForErrorMessage, addPleaseCheck, patternParseRule, true);
        Tree ast = parseResult.getTree();
        if (log.isDebugEnabled()) {
            ASTUtil.dumpAST(ast);
        }

        // Walk
        EPLTreeWalkerListener walker = new EPLTreeWalkerListener(parseResult.getTokenStream(), services.getEngineImportService(), services.getVariableService(), defaultStreamSelector, services.getEngineURI(), services.getConfigSnapshot(), services.getPatternNodeFactory(), services.getContextManagementService(), parseResult.getScripts(), services.getExprDeclaredService(), services.getTableService());
        try {
            ParseHelper.walk(ast, walker, expression, expressionForErrorMessage);
        } catch (ASTWalkException ex) {
            log.debug(".createPattern Error validating expression", ex);
            throw new EPStatementException(ex.getMessage(), expression);
        } catch (EPStatementSyntaxException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            String message = "Error in expression";
            log.debug(message, ex);
            throw new EPStatementException(getNullableErrortext(message, ex.getMessage()), expression);
        }

        if (walker.getStatementSpec().getStreamSpecs().size() > 1) {
            throw new IllegalStateException("Unexpected multiple stream specifications encountered");
        }

        // Get pattern specification
        PatternStreamSpecRaw patternStreamSpec = (PatternStreamSpecRaw) walker.getStatementSpec().getStreamSpecs().get(0);

        // Create statement spec, set pattern stream, set wildcard select
        StatementSpecRaw statementSpec = new StatementSpecRaw(SelectClauseStreamSelectorEnum.ISTREAM_ONLY);
        statementSpec.getStreamSpecs().add(patternStreamSpec);
        statementSpec.getSelectClauseSpec().getSelectExprList().clear();
        statementSpec.getSelectClauseSpec().getSelectExprList().add(new SelectClauseElementWildcard());
        statementSpec.setAnnotations(walker.getStatementSpec().getAnnotations());
        statementSpec.setExpressionNoAnnotations(parseResult.getExpressionWithoutAnnotations());

        return statementSpec;
    }

    private static String getNullableErrortext(String msg, String cause) {
        if (cause == null) {
            return msg;
        } else {
            return msg + ": " + cause;
        }
    }

    private final static Logger log = LoggerFactory.getLogger(EPAdministratorHelper.class);
}
