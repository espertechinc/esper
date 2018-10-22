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

import com.espertech.esper.common.internal.compile.stage1.spec.GroupByClauseExpressions;
import com.espertech.esper.common.internal.compile.stage1.spec.SelectClauseStreamSelectorEnum;
import com.espertech.esper.common.internal.compile.stage1.spec.StatementSpecRaw;
import com.espertech.esper.common.internal.compile.stage1.spec.StreamSpecCompiled;
import com.espertech.esper.common.internal.epl.expression.declared.compiletime.ExprDeclaredNode;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.common.internal.epl.expression.table.ExprTableAccessNode;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

/**
 * Specification object representing a complete EPL statement including all EPL constructs.
 */
public class StatementSpecCompiled {

    private final StatementSpecRaw raw;
    private final StreamSpecCompiled[] streamSpecs;
    private SelectClauseSpecCompiled selectClauseCompiled;
    private final Annotation[] annotations;
    private final GroupByClauseExpressions groupByExpressions;
    private final List<ExprSubselectNode> subselectNodes;
    private final List<ExprDeclaredNode> exprDeclaredNodes;
    private final List<ExprTableAccessNode> tableAccessNodes;

    public StatementSpecCompiled() {
        raw = new StatementSpecRaw(SelectClauseStreamSelectorEnum.ISTREAM_ONLY);
        streamSpecs = StreamSpecCompiled.EMPTY_STREAM_ARRAY;
        selectClauseCompiled = new SelectClauseSpecCompiled(false);
        annotations = null;
        groupByExpressions = null;
        subselectNodes = Collections.emptyList();
        exprDeclaredNodes = Collections.emptyList();
        tableAccessNodes = Collections.emptyList();
    }

    public StatementSpecCompiled(StatementSpecRaw raw, StreamSpecCompiled[] streamSpecs, SelectClauseSpecCompiled selectClauseCompiled, Annotation[] annotations, GroupByClauseExpressions groupByExpressions, List<ExprSubselectNode> subselectNodes, List<ExprDeclaredNode> exprDeclaredNodes, List<ExprTableAccessNode> tableAccessNodes) {
        this.raw = raw;
        this.streamSpecs = streamSpecs;
        this.selectClauseCompiled = selectClauseCompiled;
        this.annotations = annotations;
        this.groupByExpressions = groupByExpressions;
        this.subselectNodes = subselectNodes;
        this.exprDeclaredNodes = exprDeclaredNodes;
        this.tableAccessNodes = tableAccessNodes;
    }

    public StatementSpecCompiled(StatementSpecCompiled spec, StreamSpecCompiled[] streamSpecCompileds) {
        this(spec.getRaw(), streamSpecCompileds, spec.selectClauseCompiled, spec.getAnnotations(), spec.groupByExpressions, spec.subselectNodes, spec.exprDeclaredNodes, spec.tableAccessNodes);
    }

    public StatementSpecRaw getRaw() {
        return raw;
    }

    public StreamSpecCompiled[] getStreamSpecs() {
        return streamSpecs;
    }

    public SelectClauseSpecCompiled getSelectClauseCompiled() {
        return selectClauseCompiled;
    }

    public Annotation[] getAnnotations() {
        return annotations;
    }

    public GroupByClauseExpressions getGroupByExpressions() {
        return groupByExpressions;
    }

    public List<ExprSubselectNode> getSubselectNodes() {
        return subselectNodes;
    }

    public List<ExprTableAccessNode> getTableAccessNodes() {
        return tableAccessNodes;
    }

    public ExprDeclaredNode[] getDeclaredExpressions() {
        return exprDeclaredNodes.toArray(new ExprDeclaredNode[exprDeclaredNodes.size()]);
    }

    public void setSelectClauseCompiled(SelectClauseSpecCompiled selectClauseCompiled) {
        this.selectClauseCompiled = selectClauseCompiled;
    }
}
