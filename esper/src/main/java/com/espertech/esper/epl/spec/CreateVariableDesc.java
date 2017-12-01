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

/**
 * Descriptor for create-variable statements.
 */
public class CreateVariableDesc implements Serializable {
    private String variableType;
    private String variableName;
    private ExprNode assignment;
    private boolean constant;
    private boolean array;
    private boolean arrayOfPrimitive;
    private static final long serialVersionUID = -7864602464816397227L;

    /**
     * Ctor.
     *
     * @param variableType     type of the variable
     * @param variableName     name of the variable
     * @param assignment       expression assigning the initial value, or null if none
     * @param constant         indicator for constant
     * @param array            indicator for array
     * @param arrayOfPrimitive indicator for array of primitive
     */
    public CreateVariableDesc(String variableType, String variableName, ExprNode assignment, boolean constant, boolean array, boolean arrayOfPrimitive) {
        this.variableType = variableType;
        this.variableName = variableName;
        this.assignment = assignment;
        this.constant = constant;
        this.array = array;
        this.arrayOfPrimitive = arrayOfPrimitive;
    }

    /**
     * Returns the variable type.
     *
     * @return type of variable
     */
    public String getVariableType() {
        return variableType;
    }

    /**
     * Returns the variable name
     *
     * @return name
     */
    public String getVariableName() {
        return variableName;
    }

    /**
     * Returns the assignment expression, or null if none
     *
     * @return expression or null
     */
    public ExprNode getAssignment() {
        return assignment;
    }

    public boolean isConstant() {
        return constant;
    }

    public boolean isArray() {
        return array;
    }

    public boolean isArrayOfPrimitive() {
        return arrayOfPrimitive;
    }

    public void setArrayOfPrimitive(boolean arrayOfPrimitive) {
        this.arrayOfPrimitive = arrayOfPrimitive;
    }
}
