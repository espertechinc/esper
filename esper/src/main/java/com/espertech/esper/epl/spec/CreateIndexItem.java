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

/**
 * Specification for creating a named window index column.
 */
public class CreateIndexItem implements Serializable {
    private static final long serialVersionUID = 1739215547545216200L;
    private final List<ExprNode> expressions;
    private final String type;
    private final List<ExprNode> parameters;

    public CreateIndexItem(List<ExprNode> expressions, String type, List<ExprNode> parameters) {
        this.expressions = expressions;
        this.type = type;
        this.parameters = parameters;
    }

    public List<ExprNode> getExpressions() {
        return expressions;
    }

    public String getType() {
        return type;
    }

    public List<ExprNode> getParameters() {
        return parameters;
    }
}
