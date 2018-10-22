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
package com.espertech.esper.common.internal.compile.stage1.spec;

import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.type.ClassIdentifierWArray;

/**
 * Descriptor for create-variable statements.
 */
public class CreateVariableDesc {
    private ClassIdentifierWArray variableType;
    private String variableName;
    private ExprNode assignment;
    private boolean constant;

    /**
     * Ctor.
     *
     * @param variableType type of the variable
     * @param variableName name of the variable
     * @param assignment   expression assigning the initial value, or null if none
     * @param constant     indicator for constant
     */
    public CreateVariableDesc(ClassIdentifierWArray variableType, String variableName, ExprNode assignment, boolean constant) {
        this.variableType = variableType;
        this.variableName = variableName;
        this.assignment = assignment;
        this.constant = constant;
    }

    /**
     * Returns the variable type.
     *
     * @return type of variable
     */
    public ClassIdentifierWArray getVariableType() {
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

    public void setAssignment(ExprNode assignment) {
        this.assignment = assignment;
    }
}
