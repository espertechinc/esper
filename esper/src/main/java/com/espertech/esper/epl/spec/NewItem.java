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

public class NewItem {
    private final String name;
    private final ExprNode optExpression;

    public NewItem(String name, ExprNode optExpression) {
        this.name = name;
        this.optExpression = optExpression;
    }

    public String getName() {
        return name;
    }

    public ExprNode getOptExpression() {
        return optExpression;
    }
}
