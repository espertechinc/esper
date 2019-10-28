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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.annotation.HintEnum;
import com.espertech.esper.common.internal.compile.stage1.Compilable;
import com.espertech.esper.common.internal.compile.stage1.spec.*;
import com.espertech.esper.common.internal.compile.stage1.specmapper.ExpressionCopier;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StmtClassForgeableFactory;
import com.espertech.esper.common.internal.epl.agg.rollup.GroupByExpressionHelper;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.common.internal.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeSubselectDeclaredDotVisitor;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeViewResourceVisitor;
import com.espertech.esper.common.internal.epl.resultset.select.core.SelectClauseStreamCompiledSpec;
import com.espertech.esper.common.internal.epl.rowrecog.core.RowRecogPatternExpandUtil;
import com.espertech.esper.common.internal.epl.rowrecog.expr.RowRecogExprNode;
import com.espertech.esper.common.internal.epl.util.StatementSpecRawWalkerSubselectAndDeclaredDot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.*;

public class StatementRawCompiler {
    private final static Logger log = LoggerFactory.getLogger(StatementRawCompiler.class);

    public static StatementSpecCompiledDesc compile(StatementSpecRaw spec,
                                                Compilable compilable,
                                                boolean isSubquery,
                                                boolean isOnDemandQuery,
                                                Annotation[] annotations,
                                                List<ExprSubselectNode> subselectNodes,
                                                List<ExprTableAccessNode> tableAccessNodes,
                                                StatementRawInfo statementRawInfo,
                                                StatementCompileTimeServices compileTimeServices) throws StatementSpecCompileException {
        List<StreamSpecCompiled> compiledStreams;
        Set<String> eventTypeReferences = new HashSet<String>();
        List<StmtClassForgeableFactory> additionalForgeables = new ArrayList<>(2);

        if (!isOnDemandQuery && spec.getFireAndForgetSpec() != null) {
            throw new StatementSpecCompileException("Provided EPL expression is an on-demand query expression (not a continuous query)", compilable.toEPL());
        }

        // If not using a join and not specifying a data window, make the where-clause, if present, the filter of the stream
        // if selecting using filter spec, and not subquery in where clause
        if ((spec.getStreamSpecs().size() == 1) &&
                (spec.getStreamSpecs().get(0) instanceof FilterStreamSpecRaw) &&
                (spec.getStreamSpecs().get(0).getViewSpecs().length == 0) &&
                (spec.getWhereClause() != null) &&
                (spec.getOnTriggerDesc() == null) &&
                !isSubquery &&
                !isOnDemandQuery &&
                (tableAccessNodes == null || tableAccessNodes.isEmpty())) {
            boolean disqualified;
            ExprNode whereClause = spec.getWhereClause();

            ExprNodeSubselectDeclaredDotVisitor visitor = new ExprNodeSubselectDeclaredDotVisitor();
            whereClause.accept(visitor);
            disqualified = visitor.getSubselects().size() > 0 || HintEnum.DISABLE_WHEREEXPR_MOVETO_FILTER.getHint(annotations) != null;

            if (!disqualified) {
                ExprNodeViewResourceVisitor viewResourceVisitor = new ExprNodeViewResourceVisitor();
                whereClause.accept(viewResourceVisitor);
                disqualified = viewResourceVisitor.getExprNodes().size() > 0;
            }

            if (!disqualified) {
                spec.setWhereClause(null);
                FilterStreamSpecRaw streamSpec = (FilterStreamSpecRaw) spec.getStreamSpecs().get(0);
                streamSpec.getRawFilterSpec().getFilterExpressions().add(whereClause);
            }
        }

        // compile select-clause
        SelectClauseSpecCompiled selectClauseCompiled = compileSelectClause(spec.getSelectClauseSpec());

        // Determine subselects in filter streams, these may need special handling for locking
        ExprNodeSubselectDeclaredDotVisitor visitor = new ExprNodeSubselectDeclaredDotVisitor();
        try {
            StatementSpecRawWalkerSubselectAndDeclaredDot.walkStreamSpecs(spec, visitor);
        } catch (ExprValidationException ex) {
            throw new StatementSpecCompileException(ex.getMessage(), ex, compilable.toEPL());
        }
        for (ExprSubselectNode subselectNode : visitor.getSubselects()) {
            subselectNode.setFilterStreamSubselect(true);
        }

        // Determine subselects for compilation, and lambda-expression shortcut syntax for named windows
        visitor.reset();
        GroupByClauseExpressions groupByRollupExpressions;
        try {
            StatementSpecRawWalkerSubselectAndDeclaredDot.walkSubselectAndDeclaredDotExpr(spec, visitor);

            ExpressionCopier expressionCopier = new ExpressionCopier(spec, statementRawInfo.getOptionalContextDescriptor(), compileTimeServices, visitor);
            groupByRollupExpressions = GroupByExpressionHelper.getGroupByRollupExpressions(spec.getGroupByExpressions(),
                    spec.getSelectClauseSpec(), spec.getHavingClause(), spec.getOrderByList(), expressionCopier);
        } catch (ExprValidationException ex) {
            throw new StatementSpecCompileException(ex.getMessage(), ex, compilable.toEPL());
        }

        // Expand match-recognize patterns
        if (spec.getMatchRecognizeSpec() != null) {
            RowRecogExprNode expandedPatternNode;
            try {
                ExpressionCopier copier = new ExpressionCopier(spec, statementRawInfo.getOptionalContextDescriptor(), compileTimeServices, visitor);
                expandedPatternNode = RowRecogPatternExpandUtil.expand(spec.getMatchRecognizeSpec().getPattern(), copier);
            } catch (ExprValidationException ex) {
                throw new StatementSpecCompileException(ex.getMessage(), ex, compilable.toEPL());
            }
            spec.getMatchRecognizeSpec().setPattern(expandedPatternNode);
        }

        if (isSubquery && !visitor.getSubselects().isEmpty()) {
            throw new StatementSpecCompileException("Invalid nested subquery, subquery-within-subquery is not supported", compilable.toEPL());
        }
        for (ExprSubselectNode subselectNode : visitor.getSubselects()) {
            if (!subselectNodes.contains(subselectNode)) {
                subselectNodes.add(subselectNode);
            }
        }

        // Compile subselects found
        int subselectNumber = 0;
        for (ExprSubselectNode subselect : subselectNodes) {
            StatementSpecRaw raw = subselect.getStatementSpecRaw();
            StatementSpecCompiledDesc desc = compile(raw, compilable, true, isOnDemandQuery, annotations, Collections.emptyList(), Collections.emptyList(),
                statementRawInfo, compileTimeServices);
            additionalForgeables.addAll(desc.getAdditionalForgeables());
            subselect.setStatementSpecCompiled(desc.getCompiled(), subselectNumber);
            subselectNumber++;
        }

        // Set table-access number
        int tableAccessNumber = 0;
        for (ExprTableAccessNode tableAccess : tableAccessNodes) {
            tableAccess.setTableAccessNumber(tableAccessNumber);
            tableAccessNumber++;
        }

        // compile each stream used
        try {
            compiledStreams = new ArrayList<>(spec.getStreamSpecs().size());
            int streamNum = 0;
            for (StreamSpecRaw rawSpec : spec.getStreamSpecs()) {
                streamNum++;
                StreamSpecCompiledDesc desc = StreamSpecCompiler.compile(rawSpec, eventTypeReferences, spec.getInsertIntoDesc() != null, spec.getStreamSpecs().size() > 1, false, spec.getOnTriggerDesc() != null, rawSpec.getOptionalStreamName(), streamNum, statementRawInfo, compileTimeServices);
                additionalForgeables.addAll(desc.getAdditionalForgeables());
                compiledStreams.add(desc.getStreamSpecCompiled());
            }
        } catch (ExprValidationException ex) {
            if (ex.getMessage() == null) {
                throw new StatementSpecCompileException("Unexpected exception compiling statement, please consult the log file and report the exception", ex, compilable.toEPL());
            } else {
                throw new StatementSpecCompileException(ex.getMessage(), ex, compilable.toEPL());
            }
        } catch (EPException ex) {
            throw new StatementSpecCompileException(ex.getMessage(), ex, compilable.toEPL());
        } catch (RuntimeException ex) {
            String text = "Unexpected error compiling statement";
            log.error(text, ex);
            throw new StatementSpecCompileException(text + ": " + ex.getClass().getName() + ":" + ex.getMessage(), ex, compilable.toEPL());
        }

        StatementSpecCompiled compiled = new StatementSpecCompiled(spec, compiledStreams.toArray(new StreamSpecCompiled[compiledStreams.size()]), selectClauseCompiled, annotations, groupByRollupExpressions, subselectNodes, visitor.getDeclaredExpressions(), tableAccessNodes);
        return new StatementSpecCompiledDesc(compiled, additionalForgeables);
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
}
