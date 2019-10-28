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
package com.espertech.esper.common.internal.compile.stage2;

import com.espertech.esper.common.internal.compile.stage1.spec.*;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectClauseStreamCompiledSpec;
import com.espertech.esper.common.internal.epl.table.compiletime.TableCompileTimeResolver;

import java.util.ArrayList;
import java.util.List;

public class StatementLifecycleSvcUtil {

    public static boolean determineHasTableAccess(List<ExprSubselectNode> subselectNodes, StatementSpecRaw statementSpecRaw, TableCompileTimeResolver tableCompileTimeResolver) {
        boolean hasTableAccess = (statementSpecRaw.getTableExpressions() != null && !statementSpecRaw.getTableExpressions().isEmpty()) ||
                statementSpecRaw.getIntoTableSpec() != null;
        hasTableAccess = hasTableAccess || isJoinWithTable(statementSpecRaw, tableCompileTimeResolver) || isSubqueryWithTable(subselectNodes, tableCompileTimeResolver) || isInsertIntoTable(statementSpecRaw, tableCompileTimeResolver);
        return hasTableAccess;
    }

    private static boolean isInsertIntoTable(StatementSpecRaw statementSpecRaw, TableCompileTimeResolver tableCompileTimeResolver) {
        if (statementSpecRaw.getInsertIntoDesc() == null) {
            return false;
        }
        return tableCompileTimeResolver.resolve(statementSpecRaw.getInsertIntoDesc().getEventTypeName()) != null;
    }

    public static boolean isSubqueryWithTable(List<ExprSubselectNode> subselectNodes, TableCompileTimeResolver tableCompileTimeResolver) {
        for (ExprSubselectNode node : subselectNodes) {
            FilterStreamSpecRaw spec = (FilterStreamSpecRaw) node.getStatementSpecRaw().getStreamSpecs().get(0);
            if (tableCompileTimeResolver.resolve(spec.getRawFilterSpec().getEventTypeName()) != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean isJoinWithTable(StatementSpecRaw statementSpecRaw, TableCompileTimeResolver tableCompileTimeResolver) {
        for (StreamSpecRaw stream : statementSpecRaw.getStreamSpecs()) {
            if (stream instanceof FilterStreamSpecRaw) {
                FilterStreamSpecRaw filter = (FilterStreamSpecRaw) stream;
                if (tableCompileTimeResolver.resolve(filter.getRawFilterSpec().getEventTypeName()) != null) {
                    return true;
                }
            }
        }
        return false;
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

    public static boolean isWritesToTables(StatementSpecRaw statementSpec, TableCompileTimeResolver tableCompileTimeResolver) {
        // determine if writing to a table:

        // insert-into (single)
        if (statementSpec.getInsertIntoDesc() != null) {
            if (isTable(statementSpec.getInsertIntoDesc().getEventTypeName(), tableCompileTimeResolver)) {
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
                    if (stream.getInsertInto() != null && isTable(stream.getInsertInto().getEventTypeName(), tableCompileTimeResolver)) {
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
                    if (isTable(window.getWindowName(), tableCompileTimeResolver)) {
                        return true;
                    }
                }
            }

            // on-merge with insert-action
            if (onTriggerDesc instanceof OnTriggerMergeDesc) {
                OnTriggerMergeDesc merge = (OnTriggerMergeDesc) onTriggerDesc;
                for (OnTriggerMergeMatched item : merge.getItems()) {
                    for (OnTriggerMergeAction action : item.getActions()) {
                        if (checkOnTriggerMergeAction(action, tableCompileTimeResolver)) {
                            return true;
                        }
                    }
                }
                if (merge.getOptionalInsertNoMatch() != null && checkOnTriggerMergeAction(merge.getOptionalInsertNoMatch(), tableCompileTimeResolver)) {
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
                    return isTable(((FilterStreamSpecRaw) statementSpec.getStreamSpecs().get(0)).getRawFilterSpec().getEventTypeName(), tableCompileTimeResolver);
                }
            }
        }

        return false;
    }

    private static boolean checkOnTriggerMergeAction(OnTriggerMergeAction action, TableCompileTimeResolver tableCompileTimeResolver) {
        if (action instanceof OnTriggerMergeActionInsert) {
            OnTriggerMergeActionInsert insert = (OnTriggerMergeActionInsert) action;
            if (insert.getOptionalStreamName() != null && isTable(insert.getOptionalStreamName(), tableCompileTimeResolver)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTable(String name, TableCompileTimeResolver tableCompileTimeResolver) {
        return tableCompileTimeResolver.resolve(name) != null;
    }
}
