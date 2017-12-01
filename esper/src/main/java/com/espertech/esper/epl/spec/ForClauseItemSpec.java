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

import java.io.Serializable;
import java.util.List;

public class ForClauseItemSpec implements Serializable {
    private static final long serialVersionUID = 4374267047749646423L;

    private String keyword;
    private List<ExprNode> expressions;

    public ForClauseItemSpec(String keyword, List<ExprNode> expressions) {
        this.keyword = keyword;
        this.expressions = expressions;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public List<ExprNode> getExpressions() {
        return expressions;
    }

    public void setExpressions(List<ExprNode> expressions) {
        this.expressions = expressions;
    }
}