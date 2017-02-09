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
package com.espertech.esper.epl.spec;

import com.espertech.esper.epl.expression.core.ExprNode;

import java.util.List;

/**
 * Specification for the merge statement insert-part.
 */
public class OnTriggerMergeActionInsert extends OnTriggerMergeAction {
    private static final long serialVersionUID = -657179063417985357L;

    private final String optionalStreamName;
    private final List<String> columns;
    private final List<SelectClauseElementRaw> selectClause;
    private transient List<SelectClauseElementCompiled> selectClauseCompiled;

    public OnTriggerMergeActionInsert(ExprNode optionalWhereClause, String optionalStreamName, List<String> columns, List<SelectClauseElementRaw> selectClause) {
        super(optionalWhereClause);
        this.optionalStreamName = optionalStreamName;
        this.columns = columns;
        this.selectClause = selectClause;
    }

    public String getOptionalStreamName() {
        return optionalStreamName;
    }

    public List<String> getColumns() {
        return columns;
    }

    public List<SelectClauseElementRaw> getSelectClause() {
        return selectClause;
    }

    public void setSelectClauseCompiled(List<SelectClauseElementCompiled> selectClauseCompiled) {
        this.selectClauseCompiled = selectClauseCompiled;
    }

    public List<SelectClauseElementCompiled> getSelectClauseCompiled() {
        return selectClauseCompiled;
    }
}

