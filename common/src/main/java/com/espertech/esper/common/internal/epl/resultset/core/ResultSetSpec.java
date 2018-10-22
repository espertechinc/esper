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
package com.espertech.esper.common.internal.epl.resultset.core;

import com.espertech.esper.common.internal.compile.stage1.spec.*;
import com.espertech.esper.common.internal.compile.stage2.SelectClauseSpecCompiled;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompiled;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;

import java.lang.annotation.Annotation;
import java.util.List;

public class ResultSetSpec {
    private final SelectClauseStreamSelectorEnum selectClauseStreamSelector;
    private final List<OrderByItem> orderByList;
    private final SelectClauseSpecCompiled selectClauseSpec;
    private final InsertIntoDesc insertIntoDesc;
    private final GroupByClauseExpressions groupByClauseExpressions;
    private final ExprNode whereClause;
    private final ExprNode havingClause;
    private final OutputLimitSpec optionalOutputLimitSpec;
    private final RowLimitSpec rowLimitSpec;
    private final String contextName;
    private final ForClauseSpec forClauseSpec;
    private final IntoTableSpec intoTableSpec;
    private final StreamSpecCompiled[] streamSpecs;
    private final Annotation[] annotations;

    public ResultSetSpec(SelectClauseStreamSelectorEnum selectClauseStreamSelector, List<OrderByItem> orderByList, SelectClauseSpecCompiled selectClauseSpec, InsertIntoDesc insertIntoDesc, GroupByClauseExpressions groupByClauseExpressions, ExprNode whereClause, ExprNode havingClause, OutputLimitSpec optionalOutputLimitSpec, RowLimitSpec rowLimitSpec, String contextName, ForClauseSpec forClauseSpec, IntoTableSpec intoTableSpec, StreamSpecCompiled[] streamSpecs, Annotation[] annotations) {
        this.selectClauseStreamSelector = selectClauseStreamSelector;
        this.orderByList = orderByList;
        this.selectClauseSpec = selectClauseSpec;
        this.insertIntoDesc = insertIntoDesc;
        this.groupByClauseExpressions = groupByClauseExpressions;
        this.whereClause = whereClause;
        this.havingClause = havingClause;
        this.optionalOutputLimitSpec = optionalOutputLimitSpec;
        this.rowLimitSpec = rowLimitSpec;
        this.contextName = contextName;
        this.forClauseSpec = forClauseSpec;
        this.intoTableSpec = intoTableSpec;
        this.streamSpecs = streamSpecs;
        this.annotations = annotations;
    }

    public ResultSetSpec(StatementSpecCompiled statementSpec) {
        this(statementSpec.getRaw().getSelectStreamSelectorEnum(), statementSpec.getRaw().getOrderByList(), statementSpec.getSelectClauseCompiled(),
                statementSpec.getRaw().getInsertIntoDesc(), statementSpec.getGroupByExpressions(), statementSpec.getRaw().getWhereClause(), statementSpec.getRaw().getHavingClause(), statementSpec.getRaw().getOutputLimitSpec(),
                statementSpec.getRaw().getRowLimitSpec(), statementSpec.getRaw().getOptionalContextName(), statementSpec.getRaw().getForClauseSpec(), statementSpec.getRaw().getIntoTableSpec(),
                statementSpec.getStreamSpecs(), statementSpec.getAnnotations());
    }

    public List<OrderByItem> getOrderByList() {
        return orderByList;
    }

    public SelectClauseSpecCompiled getSelectClauseSpec() {
        return selectClauseSpec;
    }

    public InsertIntoDesc getInsertIntoDesc() {
        return insertIntoDesc;
    }

    public ExprNode getHavingClause() {
        return havingClause;
    }

    public OutputLimitSpec getOptionalOutputLimitSpec() {
        return optionalOutputLimitSpec;
    }

    public SelectClauseStreamSelectorEnum getSelectClauseStreamSelector() {
        return selectClauseStreamSelector;
    }

    public GroupByClauseExpressions getGroupByClauseExpressions() {
        return groupByClauseExpressions;
    }

    public RowLimitSpec getRowLimitSpec() {
        return rowLimitSpec;
    }

    public String getContextName() {
        return contextName;
    }

    public ExprNode getWhereClause() {
        return whereClause;
    }

    public ForClauseSpec getForClauseSpec() {
        return forClauseSpec;
    }

    public IntoTableSpec getIntoTableSpec() {
        return intoTableSpec;
    }

    public StreamSpecCompiled[] getStreamSpecs() {
        return streamSpecs;
    }

    public Annotation[] getAnnotations() {
        return annotations;
    }
}
