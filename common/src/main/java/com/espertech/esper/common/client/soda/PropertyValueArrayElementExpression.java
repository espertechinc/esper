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
package com.espertech.esper.common.client.soda;

import java.io.StringWriter;
import java.util.List;

/**
 * Property value array element expression, i.e. "myarray[someindex]"
 */
public class PropertyValueArrayElementExpression extends ExpressionBase {

    private String propertyName;
    private List<Expression> indexes;

    /**
     * Ctor.
     */
    public PropertyValueArrayElementExpression() {
    }

    /**
     * Ctor
     * @param propertyName property name returning array
     * @param indexes array index or indexes
     */
    public PropertyValueArrayElementExpression(String propertyName, List<Expression> indexes) {
        this.propertyName = propertyName;
        this.indexes = indexes;
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.UNARY;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.write(propertyName);
        for (Expression index : indexes) {
            writer.append("[");
            index.toEPL(writer, ExpressionPrecedenceEnum.UNARY);
            writer.append("]");
        }
    }

    /**
     * Returns the property name.
     * @return property name
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Sets the property name.
     * @param propertyName property name
     */
    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    /**
     * Returns the index expressions
     * @return indexes
     */
    public List<Expression> getIndexes() {
        return indexes;
    }

    /**
     * Sets the index expressions
     * @param indexes indexes
     */
    public void setIndexes(List<Expression> indexes) {
        this.indexes = indexes;
    }
}
