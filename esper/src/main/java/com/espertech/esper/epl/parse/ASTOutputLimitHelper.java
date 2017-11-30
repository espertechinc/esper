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
package com.espertech.esper.epl.parse;

import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.time.ExprTimePeriod;
import com.espertech.esper.epl.generated.EsperEPL2GrammarParser;
import com.espertech.esper.epl.spec.*;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.Tree;

import java.util.List;
import java.util.Map;

/**
 * Builds an output limit spec from an output limit AST node.
 */
public class ASTOutputLimitHelper {
    public static OutputLimitSpec buildOutputLimitSpec(CommonTokenStream tokenStream, EsperEPL2GrammarParser.OutputLimitContext ctx, Map<Tree, ExprNode> astExprNodeMap) {
        OutputLimitLimitType displayLimit = OutputLimitLimitType.DEFAULT;
        if (ctx.k != null) {
            switch (ctx.k.getType()) {
                case EsperEPL2GrammarParser.FIRST:
                    displayLimit = OutputLimitLimitType.FIRST;
                    break;
                case EsperEPL2GrammarParser.LAST:
                    displayLimit = OutputLimitLimitType.LAST;
                    break;
                case EsperEPL2GrammarParser.SNAPSHOT:
                    displayLimit = OutputLimitLimitType.SNAPSHOT;
                    break;
                case EsperEPL2GrammarParser.ALL:
                    displayLimit = OutputLimitLimitType.ALL;
                    break;
                default:
                    throw ASTWalkException.from("Encountered unrecognized token " + ctx.k.getText(), tokenStream, ctx);
            }
        }

        // next is a variable, or time period, or number
        String variableName = null;
        Double rate = null;
        ExprNode whenExpression = null;
        List<ExprNode> crontabScheduleSpec = null;
        List<OnTriggerSetAssignment> thenExpressions = null;
        ExprTimePeriod timePeriodExpr = null;
        OutputLimitRateType rateType;
        ExprNode andAfterTerminateExpr = null;
        List<OnTriggerSetAssignment> andAfterTerminateSetExpressions = null;

        if (ctx.t != null) {
            rateType = OutputLimitRateType.TERM;
            if (ctx.expression() != null) {
                andAfterTerminateExpr = ASTExprHelper.exprCollectSubNodes(ctx.expression(), 0, astExprNodeMap).get(0);
            }
            if (ctx.onSetExpr() != null) {
                andAfterTerminateSetExpressions = ASTExprHelper.getOnTriggerSetAssignments(ctx.onSetExpr().onSetAssignmentList(), astExprNodeMap);
            }
        } else if (ctx.wh != null) {
            rateType = OutputLimitRateType.WHEN_EXPRESSION;
            whenExpression = ASTExprHelper.exprCollectSubNodes(ctx.expression(), 0, astExprNodeMap).get(0);
            if (ctx.onSetExpr() != null) {
                thenExpressions = ASTExprHelper.getOnTriggerSetAssignments(ctx.onSetExpr().onSetAssignmentList(), astExprNodeMap);
            }
        } else if (ctx.at != null) {
            rateType = OutputLimitRateType.CRONTAB;
            crontabScheduleSpec = ASTExprHelper.exprCollectSubNodes(ctx.crontabLimitParameterSet(), 0, astExprNodeMap);
        } else {
            if (ctx.ev != null) {
                rateType = ctx.e != null ? OutputLimitRateType.EVENTS : OutputLimitRateType.TIME_PERIOD;
                if (ctx.i != null) {
                    variableName = ctx.i.getText();
                } else if (ctx.timePeriod() != null) {
                    timePeriodExpr = (ExprTimePeriod) ASTExprHelper.exprCollectSubNodes(ctx.timePeriod(), 0, astExprNodeMap).get(0);
                } else {
                    ASTExprHelper.exprCollectSubNodes(ctx.number(), 0, astExprNodeMap);  // remove
                    rate = Double.parseDouble(ctx.number().getText());
                }
            } else {
                rateType = OutputLimitRateType.AFTER;
            }
        }

        // get the AFTER time period
        ExprTimePeriod afterTimePeriodExpr = null;
        Integer afterNumberOfEvents = null;
        if (ctx.outputLimitAfter() != null) {
            if (ctx.outputLimitAfter().timePeriod() != null) {
                ExprNode expression = ASTExprHelper.exprCollectSubNodes(ctx.outputLimitAfter(), 0, astExprNodeMap).get(0);
                afterTimePeriodExpr = (ExprTimePeriod) expression;
            } else {
                Object constant = ASTConstantHelper.parse(ctx.outputLimitAfter().number());
                afterNumberOfEvents = ((Number) constant).intValue();
            }
        }

        boolean andAfterTerminate = false;
        if (ctx.outputLimitAndTerm() != null) {
            andAfterTerminate = true;
            if (ctx.outputLimitAndTerm().expression() != null) {
                andAfterTerminateExpr = ASTExprHelper.exprCollectSubNodes(ctx.outputLimitAndTerm().expression(), 0, astExprNodeMap).get(0);
            }
            if (ctx.outputLimitAndTerm().onSetExpr() != null) {
                andAfterTerminateSetExpressions = ASTExprHelper.getOnTriggerSetAssignments(ctx.outputLimitAndTerm().onSetExpr().onSetAssignmentList(), astExprNodeMap);
            }
        }

        return new OutputLimitSpec(rate, variableName, rateType, displayLimit, whenExpression, thenExpressions, crontabScheduleSpec, timePeriodExpr, afterTimePeriodExpr, afterNumberOfEvents, andAfterTerminate, andAfterTerminateExpr, andAfterTerminateSetExpressions);
    }

    public static RowLimitSpec buildRowLimitSpec(EsperEPL2GrammarParser.RowLimitContext ctx) {
        Object numRows;
        Object offset;
        if (ctx.o != null) {    // format "rows offset offsetcount"
            numRows = parseNumOrVariableIdent(ctx.n1, ctx.i1);
            offset = parseNumOrVariableIdent(ctx.n2, ctx.i2);
        } else if (ctx.c != null) {   // format "offsetcount, rows"
            offset = parseNumOrVariableIdent(ctx.n1, ctx.i1);
            numRows = parseNumOrVariableIdent(ctx.n2, ctx.i2);
        } else {
            numRows = parseNumOrVariableIdent(ctx.n1, ctx.i1);
            offset = null;
        }

        Integer numRowsInt = null;
        String numRowsVariable = null;
        if (numRows instanceof String) {
            numRowsVariable = (String) numRows;
        } else {
            numRowsInt = (Integer) numRows;
        }

        Integer offsetInt = null;
        String offsetVariable = null;
        if (offset instanceof String) {
            offsetVariable = (String) offset;
        } else {
            offsetInt = (Integer) offset;
        }

        return new RowLimitSpec(numRowsInt, offsetInt, numRowsVariable, offsetVariable);
    }

    private static Object parseNumOrVariableIdent(EsperEPL2GrammarParser.NumberconstantContext num, Token ident) {
        if (ident != null) {
            return ident.getText();
        } else {
            return ASTConstantHelper.parse(num);
        }
    }
}
