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

import com.espertech.esper.core.service.speccompiled.SelectClauseStreamCompiledSpec;
import com.espertech.esper.epl.expression.core.ExprNode;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.visitor.ExprNodeSubselectDeclaredDotVisitor;
import com.espertech.esper.epl.spec.*;
import com.espertech.esper.epl.table.mgmt.TableService;
import com.espertech.esper.filterspec.FilterSpecCompiled;
import com.espertech.esper.filterspec.FilterSpecParam;
import com.espertech.esper.filterspec.FilterSpecParamExprNode;
import com.espertech.esper.pattern.*;

import java.util.ArrayList;
import java.util.List;

public class StatementLifecycleSvcUtil {

    public static void assignFilterSpecIds(FilterSpecCompiled filterSpec, FilterSpecCompiled[] filterSpecsAll) {
        for (int path = 0; path < filterSpec.getParameters().length; path++) {
            for (FilterSpecParam param : filterSpec.getParameters()[path]) {
                if (param instanceof FilterSpecParamExprNode) {
                    int index = filterSpec.getFilterSpecIndexAmongAll(filterSpecsAll);
                    FilterSpecParamExprNode exprNode = (FilterSpecParamExprNode) param;
                    exprNode.setFilterSpecId(index);
                    exprNode.setFilterSpecParamPathNum(path);
                }
            }
        }
    }

    public static void walkStatement(StatementSpecRaw spec, ExprNodeSubselectDeclaredDotVisitor visitor) throws ExprValidationException {

        // Look for expressions with sub-selects in select expression list and filter expression
        // Recursively compile the statement within the statement.
        for (SelectClauseElementRaw raw : spec.getSelectClauseSpec().getSelectExprList()) {
            if (raw instanceof SelectClauseExprRawSpec) {
                SelectClauseExprRawSpec rawExpr = (SelectClauseExprRawSpec) raw;
                rawExpr.getSelectExpression().accept(visitor);
            } else {
                continue;
            }
        }
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

        // walk streams
        walkStreamSpecs(spec, visitor);
    }

    public static void walkStreamSpecs(StatementSpecRaw spec, ExprNodeSubselectDeclaredDotVisitor visitor) {

        // Determine filter streams
        for (StreamSpecRaw rawSpec : spec.getStreamSpecs()) {
            if (rawSpec instanceof FilterStreamSpecRaw) {
                FilterStreamSpecRaw raw = (FilterStreamSpecRaw) rawSpec;
                for (ExprNode filterExpr : raw.getRawFilterSpec().getFilterExpressions()) {
                    filterExpr.accept(visitor);
                }
            }
            if (rawSpec instanceof PatternStreamSpecRaw) {
                PatternStreamSpecRaw patternStreamSpecRaw = (PatternStreamSpecRaw) rawSpec;
                EvalNodeAnalysisResult analysisResult = EvalNodeUtil.recursiveAnalyzeChildNodes(patternStreamSpecRaw.getEvalFactoryNode());
                for (EvalFactoryNode evalNode : analysisResult.getActiveNodes()) {
                    if (evalNode instanceof EvalFilterFactoryNode) {
                        EvalFilterFactoryNode filterNode = (EvalFilterFactoryNode) evalNode;
                        for (ExprNode filterExpr : filterNode.getRawFilterSpec().getFilterExpressions()) {
                            filterExpr.accept(visitor);
                        }
                    }
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
                    for (SelectClauseElementRaw element : split.getSelectClause().getSelectExprList()) {
                        if (element instanceof SelectClauseExprRawSpec) {
                            SelectClauseExprRawSpec selectExpr = (SelectClauseExprRawSpec) element;
                            selectExpr.getSelectExpression().accept(visitor);
                        }
                    }
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
                        selectWalkOnMergeInsert((OnTriggerMergeActionInsert) action, visitor);
                    }
                }
            }
            if (merge.getOptionalInsertNoMatch() != null) {
                selectWalkOnMergeInsert(merge.getOptionalInsertNoMatch(), visitor);
            }
        }
    }

    private static void selectWalkOnMergeInsert(OnTriggerMergeActionInsert action, ExprNodeSubselectDeclaredDotVisitor visitor) {
        for (SelectClauseElementRaw element : action.getSelectClause()) {
            if (element instanceof SelectClauseExprRawSpec) {
                SelectClauseExprRawSpec selectExpr = (SelectClauseExprRawSpec) element;
                selectExpr.getSelectExpression().accept(visitor);
            }
        }
    }

    public static SelectClauseSpecCompiled compileSelectClause(SelectClauseSpecRaw spec) {
        List<SelectClauseElementCompiled> selectElements = new ArrayList<SelectClauseElementCompiled>();
        for (SelectClauseElementRaw raw : spec.getSelectExprList()) {
            if (raw instanceof SelectClauseExprRawSpec) {
                SelectClauseExprRawSpec rawExpr = (SelectClauseExprRawSpec) raw;
                selectElements.add(new SelectClauseExprCompiledSpec(rawExpr.getSelectExpression(), rawExpr.getOptionalAsName(), rawExpr.getOptionalAsName(), rawExpr.isEvents()));
            } else if (raw instanceof SelectClauseStreamRawSpec) {
                SelectClauseStreamRawSpec rawExpr = (SelectClauseStreamRawSpec) raw;
                selectElements.add(new SelectClauseStreamCompiledSpec(rawExpr.getStreamName(), rawExpr.getOptionalAsName()));
            } else if (raw instanceof SelectClauseElementWildcard) {
                SelectClauseElementWildcard wildcard = (SelectClauseElementWildcard) raw;
                selectElements.add(wildcard);
            } else {
                throw new IllegalStateException("Unexpected select clause element class : " + raw.getClass().getName());
            }
        }
        return new SelectClauseSpecCompiled(selectElements.toArray(new SelectClauseElementCompiled[selectElements.size()]), spec.isDistinct());
    }

    public static boolean isWritesToTables(StatementSpecRaw statementSpec, TableService tableService) {
        // determine if writing to a table:

        // insert-into (single)
        if (statementSpec.getInsertIntoDesc() != null) {
            if (isTable(statementSpec.getInsertIntoDesc().getEventTypeName(), tableService)) {
                return true;
            }
        }

        // into-table
        if (statementSpec.getIntoTableSpec() != null) {
            return true;
        }

        // triggers
        if (statementSpec.getOnTriggerDesc() != null) {
            OnTriggerDesc onTriggerDesc = statementSpec.getOnTriggerDesc();

            // split-stream insert-into
            if (onTriggerDesc.getOnTriggerType() == OnTriggerType.ON_SPLITSTREAM) {
                OnTriggerSplitStreamDesc split = (OnTriggerSplitStreamDesc) onTriggerDesc;
                for (OnTriggerSplitStream stream : split.getSplitStreams()) {
                    if (stream.getInsertInto() != null && isTable(stream.getInsertInto().getEventTypeName(), tableService)) {
                        return true;
                    }
                }
            }

            // on-delete/update/merge/on-selectdelete
            if (onTriggerDesc instanceof OnTriggerWindowDesc) {
                OnTriggerWindowDesc window = (OnTriggerWindowDesc) onTriggerDesc;
                if (onTriggerDesc.getOnTriggerType() == OnTriggerType.ON_DELETE ||
                        onTriggerDesc.getOnTriggerType() == OnTriggerType.ON_UPDATE ||
                        onTriggerDesc.getOnTriggerType() == OnTriggerType.ON_MERGE ||
                        window.isDeleteAndSelect()) {
                    if (isTable(window.getWindowName(), tableService)) {
                        return true;
                    }
                }
            }

            // on-merge with insert-action
            if (onTriggerDesc instanceof OnTriggerMergeDesc) {
                OnTriggerMergeDesc merge = (OnTriggerMergeDesc) onTriggerDesc;
                for (OnTriggerMergeMatched item : merge.getItems()) {
                    for (OnTriggerMergeAction action : item.getActions()) {
                        if (checkOnTriggerMergeAction(action, tableService)) {
                            return true;
                        }
                    }
                }
                if (merge.getOptionalInsertNoMatch() != null && checkOnTriggerMergeAction(merge.getOptionalInsertNoMatch(), tableService)) {
                    return true;
                }
            }
        } // end of trigger handling

        // fire-and-forget insert/update/delete
        if (statementSpec.getFireAndForgetSpec() != null) {
            FireAndForgetSpec faf = statementSpec.getFireAndForgetSpec();
            if (faf instanceof FireAndForgetSpecDelete ||
                    faf instanceof FireAndForgetSpecInsert ||
                    faf instanceof FireAndForgetSpecUpdate) {
                if (statementSpec.getStreamSpecs().size() == 1) {
                    return isTable(((FilterStreamSpecRaw) statementSpec.getStreamSpecs().get(0)).getRawFilterSpec().getEventTypeName(), tableService);
                }
            }
        }

        return false;
    }

    private static boolean checkOnTriggerMergeAction(OnTriggerMergeAction action, TableService tableService) {
        if (action instanceof OnTriggerMergeActionInsert) {
            OnTriggerMergeActionInsert insert = (OnTriggerMergeActionInsert) action;
            if (insert.getOptionalStreamName() != null && isTable(insert.getOptionalStreamName(), tableService)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTable(String name, TableService tableService) {
        return tableService.getTableMetadata(name) != null;
    }
}
