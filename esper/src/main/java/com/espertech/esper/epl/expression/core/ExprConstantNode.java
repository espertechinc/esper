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
package com.espertech.esper.epl.expression.core;

/**
 * Represents a constant in an expressiun tree.
 */
public interface ExprConstantNode extends ExprNode {
    public Class getConstantType();

    public Object getConstantValue(ExprEvaluatorContext context);

    public boolean isConstantValue();
}
