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
package com.espertech.esper.compiler.internal.parse;

import com.espertech.esper.common.internal.compile.stage1.spec.StatementSpecRaw;
import com.espertech.esper.common.internal.compile.stage1.spec.ViewSpec;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import org.antlr.v4.runtime.tree.Tree;

import java.util.List;
import java.util.Map;

public class StatementStackItem {
    private final StatementSpecRaw statementSpec;
    private final Map<Tree, ExprNode> astExprNodeMap;
    private final List<ViewSpec> viewSpecs;

    public StatementStackItem(StatementSpecRaw statementSpec, Map<Tree, ExprNode> astExprNodeMap, List<ViewSpec> viewSpecs) {
        this.statementSpec = statementSpec;
        this.astExprNodeMap = astExprNodeMap;
        this.viewSpecs = viewSpecs;
    }

    public StatementSpecRaw getStatementSpec() {
        return statementSpec;
    }

    public Map<Tree, ExprNode> getAstExprNodeMap() {
        return astExprNodeMap;
    }

    public List<ViewSpec> getViewSpecs() {
        return viewSpecs;
    }
}
