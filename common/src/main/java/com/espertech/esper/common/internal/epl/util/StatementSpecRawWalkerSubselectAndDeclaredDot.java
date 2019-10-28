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
package com.espertech.esper.common.internal.epl.util;

import com.espertech.esper.common.internal.compile.stage1.spec.*;
import com.espertech.esper.common.internal.compile.stage2.EvalNodeAnalysisResult;
import com.espertech.esper.common.internal.compile.stage2.EvalNodeUtil;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeSubselectDeclaredDotVisitor;
import com.espertech.esper.common.internal.epl.pattern.core.EvalForgeNode;
import com.espertech.esper.common.internal.epl.pattern.filter.EvalFilterForgeNode;
import com.espertech.esper.common.internal.epl.pattern.observer.EvalObserverForgeNode;

import java.util.List;

public class StatementSpecRawWalkerSubselectAndDeclaredDot {

    public static ExprNodeSubselectDeclaredDotVisitor walkSubselectAndDeclaredDotExpr(StatementSpecRaw spec) throws ExprValidationException {
        ExprNodeSubselectDeclaredDotVisitor visitor = new ExprNodeSubselectDeclaredDotVisitor();
        walkSubselectAndDeclaredDotExpr(spec, visitor);
        return visitor;
    }

    public static void walkSubselectAndDeclaredDotExpr(StatementSpecRaw spec, ExprNodeSubselectDeclaredDotVisitor visitor) throws ExprValidationException {
        // Look for expressions with sub-selects in select expression list and filter expression
        // Recursively compile the statement within the statement.
        walkSubselectSelectClause(spec.getSelectClauseSpec().getSelectExprList(), visitor);

        if (spec.getWhereClause() != null) {
            spec.getWhereClause().accept(visitor);
        }
        if (spec.getHavingClause() != null) {
            spec.getHavingClause().accept(visitor);
        }
        if (spec.getUpdateDesc() != null) {
            if (spec.getUpdateDesc().getOptionalWhereClause() != null) {
                spec.getUpdateDesc().getOptionalWhereClause().accept(visitor);
            }
            for (OnTriggerSetAssignment assignment : spec.getUpdateDesc().getAssignments()) {
                assignment.getExpression().accept(visitor);
            }
        }
        if (spec.getOnTriggerDesc() != null) {
            visitSubselectOnTrigger(spec.getOnTriggerDesc(), visitor);
        }

        // walk streams
        walkStreamSpecs(spec, visitor);

        // walk FAF
        walkFAFSpec(spec.getFireAndForgetSpec(), visitor);
    }

    private static void walkFAFSpec(FireAndForgetSpec fireAndForgetSpec, ExprNodeSubselectDeclaredDotVisitor visitor) {
        if (fireAndForgetSpec == null) {
            return;
        }
        if (fireAndForgetSpec instanceof FireAndForgetSpecUpdate) {
            FireAndForgetSpecUpdate update = (FireAndForgetSpecUpdate) fireAndForgetSpec;
            for (OnTriggerSetAssignment assignment : update.getAssignments()) {
                assignment.getExpression().accept(visitor);
            }
        }
    }

    public static void walkStreamSpecs(StatementSpecRaw spec, ExprNodeSubselectDeclaredDotVisitor visitor) throws ExprValidationException {
        // determine pattern-filter subqueries
        for (StreamSpecRaw streamSpecRaw : spec.getStreamSpecs()) {
            if (streamSpecRaw instanceof PatternStreamSpecRaw) {
                PatternStreamSpecRaw patternStreamSpecRaw = (PatternStreamSpecRaw) streamSpecRaw;
                EvalNodeAnalysisResult analysisResult = EvalNodeUtil.recursiveAnalyzeChildNodes(patternStreamSpecRaw.getEvalForgeNode());
                for (EvalForgeNode evalNode : analysisResult.getActiveNodes()) {
                    if (evalNode instanceof EvalFilterForgeNode) {
                        EvalFilterForgeNode filterNode = (EvalFilterForgeNode) evalNode;
                        for (ExprNode filterExpr : filterNode.getRawFilterSpec().getFilterExpressions()) {
                            filterExpr.accept(visitor);
                        }
                    } else if (evalNode instanceof EvalObserverForgeNode) {
                        int beforeCount = visitor.getSubselects().size();
                        EvalObserverForgeNode observerNode = (EvalObserverForgeNode) evalNode;
                        for (ExprNode param : observerNode.getPatternObserverSpec().getObjectParameters()) {
                            param.accept(visitor);
                        }
                        if (visitor.getSubselects().size() != beforeCount) {
                            throw new ExprValidationException("Subselects are not allowed within pattern observer parameters, please consider using a variable instead");
                        }
                    }
                }
            }
        }

        // determine filter streams
        for (StreamSpecRaw rawSpec : spec.getStreamSpecs()) {
            if (rawSpec instanceof FilterStreamSpecRaw) {
                FilterStreamSpecRaw raw = (FilterStreamSpecRaw) rawSpec;
                for (ExprNode filterExpr : raw.getRawFilterSpec().getFilterExpressions()) {
                    filterExpr.accept(visitor);
                }
            }
        }
    }

    private static void visitSubselectOnTrigger(OnTriggerDesc onTriggerDesc, ExprNodeSubselectDeclaredDotVisitor visitor) {
        if (onTriggerDesc instanceof OnTriggerWindowUpdateDesc) {
            OnTriggerWindowUpdateDesc updates = (OnTriggerWindowUpdateDesc) onTriggerDesc;
            for (OnTriggerSetAssignment assignment : updates.getAssignments()) {
                assignment.getExpression().accept(visitor);
            }
        } else if (onTriggerDesc instanceof OnTriggerSetDesc) {
            OnTriggerSetDesc sets = (OnTriggerSetDesc) onTriggerDesc;
            for (OnTriggerSetAssignment assignment : sets.getAssignments()) {
                assignment.getExpression().accept(visitor);
            }
        } else if (onTriggerDesc instanceof OnTriggerSplitStreamDesc) {
            OnTriggerSplitStreamDesc splits = (OnTriggerSplitStreamDesc) onTriggerDesc;
            for (OnTriggerSplitStream split : splits.getSplitStreams()) {
                if (split.getWhereClause() != null) {
                    split.getWhereClause().accept(visitor);
                }
                if (split.getSelectClause().getSelectExprList() != null) {
                    walkSubselectSelectClause(split.getSelectClause().getSelectExprList(), visitor);
                }
            }
        } else if (onTriggerDesc instanceof OnTriggerMergeDesc) {
            OnTriggerMergeDesc merge = (OnTriggerMergeDesc) onTriggerDesc;
            for (OnTriggerMergeMatched matched : merge.getItems()) {
                if (matched.getOptionalMatchCond() != null) {
                    matched.getOptionalMatchCond().accept(visitor);
                }
                for (OnTriggerMergeAction action : matched.getActions()) {
                    if (action.getOptionalWhereClause() != null) {
                        action.getOptionalWhereClause().accept(visitor);
                    }

                    if (action instanceof OnTriggerMergeActionUpdate) {
                        OnTriggerMergeActionUpdate update = (OnTriggerMergeActionUpdate) action;
                        for (OnTriggerSetAssignment assignment : update.getAssignments()) {
                            assignment.getExpression().accept(visitor);
                        }
                    }
                    if (action instanceof OnTriggerMergeActionInsert) {
                        OnTriggerMergeActionInsert insert = (OnTriggerMergeActionInsert) action;
                        walkSubselectSelectClause(insert.getSelectClause(), visitor);
                    }
                }
            }
            if (merge.getOptionalInsertNoMatch() != null) {
                walkSubselectSelectClause(merge.getOptionalInsertNoMatch().getSelectClause(), visitor);
            }
        }
    }

    private static void walkSubselectSelectClause(List<SelectClauseElementRaw> selectClause, ExprNodeSubselectDeclaredDotVisitor visitor) {
        for (SelectClauseElementRaw element : selectClause) {
            if (element instanceof SelectClauseExprRawSpec) {
                SelectClauseExprRawSpec selectExpr = (SelectClauseExprRawSpec) element;
                selectExpr.getSelectExpression().accept(visitor);
            }
        }
    }
}
