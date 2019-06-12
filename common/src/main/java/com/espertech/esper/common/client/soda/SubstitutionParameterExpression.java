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

/**
 * Represents a substitution parameter
 */
public class SubstitutionParameterExpression extends ExpressionBase {
    private static final long serialVersionUID = -3761383588607218180L;
    private String optionalName;
    private String optionalType;

    /**
     * Ctor.
     */
    public SubstitutionParameterExpression() {
    }

    /**
     * Ctor.
     *
     * @param optionalName name of the substitution parameter or null if none provided
     * @param optionalType type of the substitution parameter or null if none provided
     */
    public SubstitutionParameterExpression(String optionalName, String optionalType) {
        this.optionalName = optionalName;
        this.optionalType = optionalType;
    }

    /**
     * Returns the name when provided
     *
     * @return name
     */
    public String getOptionalName() {
        return optionalName;
    }

    public ExpressionPrecedenceEnum getPrecedence() {
        return ExpressionPrecedenceEnum.UNARY;
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.write("?");
        if (optionalName != null) {
            writer.write(":");
            writer.write(optionalName);
            if (optionalType != null) {
                writer.write(":");
                writer.write(optionalType);
            }
        } else if (optionalType != null) {
            writer.write("::");
            writer.write(optionalType);
        }
    }

    /**
     * Sets the name
     *
     * @param optionalName name
     */
    public void setOptionalName(String optionalName) {
        this.optionalName = optionalName;
    }

    /**
     * Returns the type when provided
     *
     * @return type
     */
    public String getOptionalType() {
        return optionalType;
    }

    /**
     * Sets the type
     *
     * @param optionalType type
     */
    public void setOptionalType(String optionalType) {
        this.optionalType = optionalType;
    }
}
