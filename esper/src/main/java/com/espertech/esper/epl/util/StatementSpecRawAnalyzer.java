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
package com.espertech.esper.epl.util;

import com.espertech.esper.client.EPException;
import com.espertech.esper.epl.expression.core.ExprIdentNode;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.epl.expression.visitor.ExprNodeSubselectDeclaredDotVisitor;
import com.espertech.esper.epl.spec.*;
import com.espertech.esper.pattern.*;

import java.util.ArrayList;
import java.util.List;

public class StatementSpecRawAnalyzer {
    public static List<FilterSpecRaw> analyzeFilters(StatementSpecRaw spec) throws ExprValidationException {
        List<FilterSpecRaw> result = new ArrayList<FilterSpecRaw>();
        addFilters(spec, result);

        ExprNodeSubselectDeclaredDotVisitor subselects = walkSubselectAndDeclaredDotExpr(spec);
        for (ExprSubselectNode subselect : subselects.getSubselects()) {
            addFilters(subselect.getStatementSpecRaw(), result);
        }
        return result;
    }

    private static void addFilters(StatementSpecRaw spec, List<FilterSpecRaw> filters) {
        for (StreamSpecRaw raw : spec.getStreamSpecs()) {
            if (raw instanceof FilterStreamSpecRaw) {
                FilterStreamSpecRaw r = (FilterStreamSpecRaw) raw;
                filters.add(r.getRawFilterSpec());
            }
            if (raw instanceof PatternStreamSpecRaw) {
                PatternStreamSpecRaw r = (PatternStreamSpecRaw) raw;
                EvalNodeAnalysisResult evalNodeAnalysisResult = EvalNodeUtil.recursiveAnalyzeChildNodes(r.getEvalFactoryNode());
                List<EvalFilterFactoryNode> filterNodes = evalNodeAnalysisResult.getFilterNodes();
                for (EvalFilterFactoryNode filterNode : filterNodes) {
                    filters.add(filterNode.getRawFilterSpec());
                }
            }
        }
    }

    public static ExprNodeSubselectDeclaredDotVisitor walkSubselectAndDeclaredDotExpr(StatementSpecRaw spec) throws ExprValidationException {

        // Look for expressions with sub-selects in select expression list and filter expression
        // Recursively compile the statement within the statement.
        ExprNodeSubselectDeclaredDotVisitor visitor = new ExprNodeSubselectDeclaredDotVisitor();
        walkSubselectSelectClause(spec.getSelectClauseSpec().getSelectExprList(), visitor);

        if (spec.getFilterRootNode() != null) {
            spec.getFilterRootNode().accept(visitor);
        }
        if (spec.getHavingExprRootNode() != null) {
            spec.getHavingExprRootNode().accept(visitor);
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
        // Determine pattern-filter subqueries
        for (StreamSpecRaw streamSpecRaw : spec.getStreamSpecs()) {
            if (streamSpecRaw instanceof PatternStreamSpecRaw) {
                PatternStreamSpecRaw patternStreamSpecRaw = (PatternStreamSpecRaw) streamSpecRaw;
                EvalNodeAnalysisResult analysisResult = EvalNodeUtil.recursiveAnalyzeChildNodes(patternStreamSpecRaw.getEvalFactoryNode());
                for (EvalFactoryNode evalNode : analysisResult.getActiveNodes()) {
                    if (evalNode instanceof EvalFilterFactoryNode) {
                        EvalFilterFactoryNode filterNode = (EvalFilterFactoryNode) evalNode;
                        for (ExprNode filterExpr : filterNode.getRawFilterSpec().getFilterExpressions()) {
                            filterExpr.accept(visitor);
                        }
                    } else if (evalNode instanceof EvalObserverFactoryNode) {
                        int beforeCount = visitor.getSubselects().size();
                        EvalObserverFactoryNode observerNode = (EvalObserverFactoryNode) evalNode;
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
        // Determine filter streams
        for (StreamSpecRaw rawSpec : spec.getStreamSpecs()) {
            if (rawSpec instanceof FilterStreamSpecRaw) {
                FilterStreamSpecRaw raw = (FilterStreamSpecRaw) rawSpec;
                for (ExprNode filterExpr : raw.getRawFilterSpec().getFilterExpressions()) {
                    filterExpr.accept(visitor);
                }
            }
        }

        return visitor;
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

    public static List<ExprNode> collectExpressionsShallow(StatementSpecRaw raw) {
        final List<ExprNode> expressions = new ArrayList<ExprNode>();

        if (raw.getExpressionDeclDesc() != null) {
            for (ExpressionDeclItem decl : raw.getExpressionDeclDesc().getExpressions()) {
                expressions.add(decl.getInner());
            }
        }

        if (raw.getCreateExpressionDesc() != null) {
            if (raw.getCreateExpressionDesc().getExpression() != null) {
                expressions.add(raw.getCreateExpressionDesc().getExpression().getInner());
            }
        }

        if (raw.getCreateContextDesc() != null) {
            ContextDetail detail = raw.getCreateContextDesc().getContextDetail();
            if (detail instanceof ContextDetailPartitioned) {
                ContextDetailPartitioned ks = (ContextDetailPartitioned) detail;
                for (ContextDetailPartitionItem item : ks.getItems()) {
                    if (item.getFilterSpecRaw().getFilterExpressions() != null) {
                        expressions.addAll(item.getFilterSpecRaw().getFilterExpressions());
                    }
                }
                if (ks.getOptionalInit() != null) {
                    for (ContextDetailConditionFilter filter : ks.getOptionalInit()) {
                        collectExpressions(expressions, filter);
                    }
                }
                if (ks.getOptionalTermination() != null) {
                    collectExpressions(expressions, ks.getOptionalTermination());
                }
            } else if (detail instanceof ContextDetailCategory) {
                ContextDetailCategory cat = (ContextDetailCategory) detail;
                for (ContextDetailCategoryItem item : cat.getItems()) {
                    if (item.getExpression() != null) {
                        expressions.add(item.getExpression());
                    }
                }
                if (cat.getFilterSpecRaw().getFilterExpressions() != null) {
                    expressions.addAll(cat.getFilterSpecRaw().getFilterExpressions());
                }
            } else if (detail instanceof ContextDetailInitiatedTerminated) {
                ContextDetailInitiatedTerminated ts = (ContextDetailInitiatedTerminated) detail;
                collectExpressions(expressions, ts.getStart());
                collectExpressions(expressions, ts.getEnd());
            } else {
                throw new EPException("Failed to obtain expressions from context detail " + detail);
            }
        }

        if (raw.getCreateVariableDesc() != null) {
            ExprNode expr = raw.getCreateVariableDesc().getAssignment();
            if (expr != null) {
                expressions.add(expr);
            }
        }

        if (raw.getCreateWindowDesc() != null) {
            ExprNode expr = raw.getCreateWindowDesc().getInsertFilter();
            if (expr != null) {
                expressions.add(expr);
            }
            for (ViewSpec view : raw.getCreateWindowDesc().getViewSpecs()) {
                expressions.addAll(view.getObjectParameters());
            }
        }

        if (raw.getUpdateDesc() != null) {
            if (raw.getUpdateDesc().getOptionalWhereClause() != null) {
                expressions.add(raw.getUpdateDesc().getOptionalWhereClause());
            }
            if (raw.getUpdateDesc().getAssignments() != null) {
                for (OnTriggerSetAssignment pair : raw.getUpdateDesc().getAssignments()) {
                    expressions.add(pair.getExpression());
                }
            }
        }

        // on-expr
        if (raw.getOnTriggerDesc() != null) {
            if (raw.getOnTriggerDesc() instanceof OnTriggerSplitStreamDesc) {
                OnTriggerSplitStreamDesc onSplit = (OnTriggerSplitStreamDesc) raw.getOnTriggerDesc();
                for (OnTriggerSplitStream item : onSplit.getSplitStreams()) {
                    if (item.getSelectClause() != null) {
                        addSelectClause(expressions, item.getSelectClause().getSelectExprList());
                    }
                    if (item.getWhereClause() != null) {
                        expressions.add(item.getWhereClause());
                    }
                }
            }
            if (raw.getOnTriggerDesc() instanceof OnTriggerSetDesc) {
                OnTriggerSetDesc onSet = (OnTriggerSetDesc) raw.getOnTriggerDesc();
                if (onSet.getAssignments() != null) {
                    for (OnTriggerSetAssignment aitem : onSet.getAssignments()) {
                        expressions.add(aitem.getExpression());
                    }
                }
            }
            if (raw.getOnTriggerDesc() instanceof OnTriggerWindowUpdateDesc) {
                OnTriggerWindowUpdateDesc onUpdate = (OnTriggerWindowUpdateDesc) raw.getOnTriggerDesc();
                if (onUpdate.getAssignments() != null) {
                    for (OnTriggerSetAssignment bitem : onUpdate.getAssignments()) {
                        expressions.add(bitem.getExpression());
                    }
                }
            }
            if (raw.getOnTriggerDesc() instanceof OnTriggerMergeDesc) {
                OnTriggerMergeDesc onMerge = (OnTriggerMergeDesc) raw.getOnTriggerDesc();
                for (OnTriggerMergeMatched item : onMerge.getItems()) {
                    if (item.getOptionalMatchCond() != null) {
                        expressions.add(item.getOptionalMatchCond());
                    }
                    for (OnTriggerMergeAction action : item.getActions()) {
                        if (action.getOptionalWhereClause() != null) {
                            expressions.add(action.getOptionalWhereClause());
                        }
                        if (action instanceof OnTriggerMergeActionUpdate) {
                            OnTriggerMergeActionUpdate update = (OnTriggerMergeActionUpdate) action;
                            for (OnTriggerSetAssignment assignment : update.getAssignments()) {
                                expressions.add(assignment.getExpression());
                            }
                        } else if (action instanceof OnTriggerMergeActionInsert) {
                            OnTriggerMergeActionInsert insert = (OnTriggerMergeActionInsert) action;
                            addSelectClause(expressions, insert.getSelectClause());
                        }
                    }
                }
                if (onMerge.getOptionalInsertNoMatch() != null) {
                    addSelectClause(expressions, onMerge.getOptionalInsertNoMatch().getSelectClause());
                }
            }
        }

        // select clause
        if (raw.getSelectClauseSpec() != null) {
            addSelectClause(expressions, raw.getSelectClauseSpec().getSelectExprList());
        }

        // from clause
        if (raw.getStreamSpecs() != null) {
            for (StreamSpecRaw stream : raw.getStreamSpecs()) {
                // filter stream
                if (stream instanceof FilterStreamSpecRaw) {
                    FilterStreamSpecRaw filterStream = (FilterStreamSpecRaw) stream;
                    FilterSpecRaw filter = filterStream.getRawFilterSpec();
                    if ((filter != null) && (filter.getFilterExpressions() != null)) {
                        expressions.addAll(filter.getFilterExpressions());
                    }
                    if ((filter != null) && (filter.getOptionalPropertyEvalSpec() != null)) {
                        for (PropertyEvalAtom contained : filter.getOptionalPropertyEvalSpec().getAtoms()) {
                            addSelectClause(expressions, contained.getOptionalSelectClause() == null ? null : contained.getOptionalSelectClause().getSelectExprList());
                            if (contained.getOptionalWhereClause() != null) {
                                expressions.add(contained.getOptionalWhereClause());
                            }
                        }
                    }
                }
                // pattern stream
                if (stream instanceof PatternStreamSpecRaw) {
                    PatternStreamSpecRaw patternStream = (PatternStreamSpecRaw) stream;
                    collectPatternExpressions(expressions, patternStream.getEvalFactoryNode());
                }
                // method stream
                if (stream instanceof MethodStreamSpec) {
                    MethodStreamSpec methodStream = (MethodStreamSpec) stream;
                    if (methodStream.getExpressions() != null) {
                        expressions.addAll(methodStream.getExpressions());
                    }
                }
                if (stream.getViewSpecs() != null) {
                    for (ViewSpec view : stream.getViewSpecs()) {
                        expressions.addAll(view.getObjectParameters());
                    }
                }
            }

            if (raw.getOuterJoinDescList() != null) {
                for (OuterJoinDesc q : raw.getOuterJoinDescList()) {
                    if (q.getOptLeftNode() != null) {
                        expressions.add(q.getOptLeftNode());
                        expressions.add(q.getOptRightNode());
                        for (ExprIdentNode ident : q.getAdditionalLeftNodes()) {
                            expressions.add(ident);
                        }
                        for (ExprIdentNode ident : q.getAdditionalRightNodes()) {
                            expressions.add(ident);
                        }
                    }
                }
            }
        }

        if (raw.getFilterRootNode() != null) {
            expressions.add(raw.getFilterRootNode());
        }

        if (raw.getGroupByExpressions() != null) {
            for (GroupByClauseElement element : raw.getGroupByExpressions()) {
                if (element instanceof GroupByClauseElementExpr) {
                    expressions.add(((GroupByClauseElementExpr) element).getExpr());
                } else if (element instanceof GroupByClauseElementRollupOrCube) {
                    GroupByClauseElementRollupOrCube rollup = (GroupByClauseElementRollupOrCube) element;
                    analyzeRollup(rollup, expressions);
                } else {
                    GroupByClauseElementGroupingSet set = (GroupByClauseElementGroupingSet) element;
                    for (GroupByClauseElement inner : set.getElements()) {
                        if (inner instanceof GroupByClauseElementExpr) {
                            expressions.add(((GroupByClauseElementExpr) inner).getExpr());
                        } else if (inner instanceof GroupByClauseElementCombinedExpr) {
                            expressions.addAll(((GroupByClauseElementCombinedExpr) inner).getExpressions());
                        } else {
                            analyzeRollup((GroupByClauseElementRollupOrCube) inner, expressions);
                        }
                    }
                }
            }
        }

        if (raw.getHavingExprRootNode() != null) {
            expressions.add(raw.getHavingExprRootNode());
        }

        if (raw.getOutputLimitSpec() != null) {
            if (raw.getOutputLimitSpec().getWhenExpressionNode() != null) {
                expressions.add(raw.getOutputLimitSpec().getWhenExpressionNode());
            }
            if (raw.getOutputLimitSpec().getThenExpressions() != null) {
                for (OnTriggerSetAssignment thenAssign : raw.getOutputLimitSpec().getThenExpressions()) {
                    expressions.add(thenAssign.getExpression());
                }
            }
            if (raw.getOutputLimitSpec().getCrontabAtSchedule() != null) {
                expressions.addAll(raw.getOutputLimitSpec().getCrontabAtSchedule());
            }
            if (raw.getOutputLimitSpec().getTimePeriodExpr() != null) {
                expressions.add(raw.getOutputLimitSpec().getTimePeriodExpr());
            }
            if (raw.getOutputLimitSpec().getAfterTimePeriodExpr() != null) {
                expressions.add(raw.getOutputLimitSpec().getAfterTimePeriodExpr());
            }
        }

        if (raw.getOrderByList() != null) {
            for (OrderByItem orderByElement : raw.getOrderByList()) {
                expressions.add(orderByElement.getExprNode());
            }
        }

        if (raw.getMatchRecognizeSpec() != null) {
            if (raw.getMatchRecognizeSpec().getPartitionByExpressions() != null) {
                expressions.addAll(raw.getMatchRecognizeSpec().getPartitionByExpressions());
            }
            for (MatchRecognizeMeasureItem selectItemMR : raw.getMatchRecognizeSpec().getMeasures()) {
                expressions.add(selectItemMR.getExpr());
            }
            for (MatchRecognizeDefineItem define : raw.getMatchRecognizeSpec().getDefines()) {
                expressions.add(define.getExpression());
            }
            if (raw.getMatchRecognizeSpec().getInterval() != null) {
                if (raw.getMatchRecognizeSpec().getInterval().getTimePeriodExpr() != null) {
                    expressions.add(raw.getMatchRecognizeSpec().getInterval().getTimePeriodExpr());
                }
            }
        }

        if (raw.getForClauseSpec() != null) {
            for (ForClauseItemSpec item : raw.getForClauseSpec().getClauses()) {
                if (item.getExpressions() != null) {
                    expressions.addAll(item.getExpressions());
                }
            }
        }

        return expressions;
    }

    private static void analyzeRollup(GroupByClauseElementRollupOrCube rollup, List<ExprNode> expressions) {
        for (GroupByClauseElement ex : rollup.getRollupExpressions()) {
            if (ex instanceof GroupByClauseElementExpr) {
                expressions.add(((GroupByClauseElementExpr) ex).getExpr());
            } else {
                GroupByClauseElementCombinedExpr combined = (GroupByClauseElementCombinedExpr) ex;
                expressions.addAll(combined.getExpressions());
            }
        }
    }

    private static void collectExpressions(List<ExprNode> expressions, ContextDetailCondition endpoint) {
        if (endpoint instanceof ContextDetailConditionCrontab) {
            ContextDetailConditionCrontab crontab = (ContextDetailConditionCrontab) endpoint;
            expressions.addAll(crontab.getCrontab());
        }
    }

    private static void addSelectClause(List<ExprNode> expressions, List<SelectClauseElementRaw> selectClause) {
        if (selectClause == null) {
            return;
        }
        for (SelectClauseElementRaw selement : selectClause) {
            if (!(selement instanceof SelectClauseExprRawSpec)) {
                continue;
            }
            SelectClauseExprRawSpec sexpr = (SelectClauseExprRawSpec) selement;
            expressions.add(sexpr.getSelectExpression());
        }
    }

    private static void collectPatternExpressions(final List<ExprNode> expressions, EvalFactoryNode patternExpression) {

        if (patternExpression instanceof EvalFilterFactoryNode) {
            EvalFilterFactoryNode filter = (EvalFilterFactoryNode) patternExpression;
            if (filter.getRawFilterSpec().getFilterExpressions() != null) {
                expressions.addAll(filter.getRawFilterSpec().getFilterExpressions());
            }
        }

        for (EvalFactoryNode child : patternExpression.getChildNodes()) {
            collectPatternExpressions(expressions, child);
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
