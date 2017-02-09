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

public class ExpressionDeclItem implements Serializable {
    private static final long serialVersionUID = 1823345580817519502L;

    private final String name;
    private final List<String> parametersNames;
    private final ExprNode inner;
    private final boolean alias;

    public ExpressionDeclItem(String name, List<String> parametersNames, ExprNode inner, boolean alias) {
        this.name = name;
        this.parametersNames = parametersNames;
        this.inner = inner;
        this.alias = alias;
    }

    public String getName() {
        return name;
    }

    public ExprNode getInner() {
        return inner;
    }

    public List<String> getParametersNames() {
        return parametersNames;
    }

    public boolean isAlias() {
        return alias;
    }
}


