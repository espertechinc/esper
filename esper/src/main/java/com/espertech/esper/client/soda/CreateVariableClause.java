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
package com.espertech.esper.client.soda;

import java.io.Serializable;
import java.io.StringWriter;

/**
 * Represents a create-variable syntax for creating a new variable.
 */
public class CreateVariableClause implements Serializable {
    private static final long serialVersionUID = 0L;

    private String variableType;
    private String variableName;
    private Expression optionalAssignment;
    private boolean constant;
    private boolean array;
    private boolean arrayOfPrimitive;

    /**
     * Ctor.
     */
    public CreateVariableClause() {
    }

    /**
     * Ctor.
     *
     * @param variableName variable name
     */
    public CreateVariableClause(String variableName) {
        this.variableName = variableName;
    }

    /**
     * Creates a create-variable syntax for declaring a variable.
     *
     * @param variableType is the variable type name
     * @param variableName is the name of the variable
     * @return create-variable clause
     */
    public static CreateVariableClause create(String variableType, String variableName) {
        return new CreateVariableClause(variableType, variableName, null, false);
    }

    /**
     * Creates a create-variable syntax for declaring a variable.
     *
     * @param variableType is the variable type name
     * @param variableName is the name of the variable
     * @param expression   is the assignment expression supplying the initial value
     * @return create-variable clause
     */
    public static CreateVariableClause create(String variableType, String variableName, Expression expression) {
        return new CreateVariableClause(variableType, variableName, expression, false);
    }

    /**
     * Ctor.
     *
     * @param variableType       is the variable type name
     * @param variableName       is the name of the variable
     * @param optionalAssignment is the optional assignment expression supplying the initial value, or null if the
     *                           initial value is null
     * @param constant           true for constant, false for regular variable
     */
    public CreateVariableClause(String variableType, String variableName, Expression optionalAssignment, boolean constant) {
        this.variableType = variableType;
        this.variableName = variableName;
        this.optionalAssignment = optionalAssignment;
        this.constant = constant;
    }

    /**
     * Returns the variable type name.
     *
     * @return type of the variable
     */
    public String getVariableType() {
        return variableType;
    }

    /**
     * Sets the variable type name.
     *
     * @param variableType type of the variable
     */
    public void setVariableType(String variableType) {
        this.variableType = variableType;
    }

    /**
     * Returns the variable name.
     *
     * @return name of the variable
     */
    public String getVariableName() {
        return variableName;
    }

    /**
     * Sets the variable name
     *
     * @param variableName name of the variable
     */
    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    /**
     * Returns the optional assignment expression, or null to initialize to a null value
     *
     * @return assignment expression, if present
     */
    public Expression getOptionalAssignment() {
        return optionalAssignment;
    }

    /**
     * Sets the optional assignment expression, or null to initialize to a null value
     *
     * @param optionalAssignment assignment expression, if present
     */
    public void setOptionalAssignment(Expression optionalAssignment) {
        this.optionalAssignment = optionalAssignment;
    }

    /**
     * Returns indicator whether the variable is a constant.
     *
     * @return constant false
     */
    public boolean isConstant() {
        return constant;
    }

    /**
     * Sets the indicator whether the variable is a constant.
     *
     * @param constant constant false
     */
    public void setConstant(boolean constant) {
        this.constant = constant;
    }

    /**
     * Returns indictor whether array or not array.
     *
     * @return array indicator
     */
    public boolean isArray() {
        return array;
    }

    /**
     * Sets indictor whether array or not array.
     *
     * @param array array indicator
     */
    public void setArray(boolean array) {
        this.array = array;
    }

    /**
     * Returns true for array of primitive values (also set the array flag)
     *
     * @return indicator
     */
    public boolean isArrayOfPrimitive() {
        return arrayOfPrimitive;
    }

    /**
     * Set true for array of primitive values  (also set the array flag)
     *
     * @param arrayOfPrimitive indicator
     */
    public void setArrayOfPrimitive(boolean arrayOfPrimitive) {
        this.arrayOfPrimitive = arrayOfPrimitive;
    }

    /**
     * Render as EPL.
     *
     * @param writer to output to
     */
    public void toEPL(StringWriter writer) {
        writer.append("create");
        if (constant) {
            writer.append(" constant");
        }
        writer.append(" variable ");
        if (variableType != null) {
            writer.append(variableType);
            if (array) {
                if (arrayOfPrimitive) {
                    writer.append("[primitive]");
                } else {
                    writer.append("[]");
                }
            }
            writer.append(" ");
        }
        writer.append(variableName);
        if (optionalAssignment != null) {
            writer.append(" = ");
            optionalAssignment.toEPL(writer, ExpressionPrecedenceEnum.MINIMUM);
        }
    }
}
