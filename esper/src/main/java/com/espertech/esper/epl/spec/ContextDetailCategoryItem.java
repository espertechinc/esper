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
import com.espertech.esper.filterspec.FilterValueSetParam;

import java.io.Serializable;

public class ContextDetailCategoryItem implements Serializable {

    private static final long serialVersionUID = -2022340210686656104L;
    private final ExprNode expression;
    private final String name;
    private FilterValueSetParam[][] compiledFilterParam;

    public ContextDetailCategoryItem(ExprNode expression, String name) {
        this.expression = expression;
        this.name = name;
    }

    public ExprNode getExpression() {
        return expression;
    }

    public String getName() {
        return name;
    }

    public FilterValueSetParam[][] getCompiledFilterParam() {
        return compiledFilterParam;
    }

    public void setCompiledFilterParam(FilterValueSetParam[][] compiledFilterParam) {
        this.compiledFilterParam = compiledFilterParam;
    }
}
