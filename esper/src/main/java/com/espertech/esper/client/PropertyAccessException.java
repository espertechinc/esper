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
package com.espertech.esper.client;

/**
 * This exception is thrown to indicate a problem with a accessing a property of an {@link com.espertech.esper.client.EventBean}.
 */
public final class PropertyAccessException extends RuntimeException {
    private String expression;
    private static final long serialVersionUID = 6935348296074757184L;

    /**
     * Constructor.
     *
     * @param message            is the error message
     * @param propertyExpression property expression
     */
    public PropertyAccessException(final String message, String propertyExpression) {
        super(message);
        this.expression = propertyExpression;
    }

    /**
     * Constructor for an inner exception and message.
     *
     * @param message is the error message
     * @param cause   is the inner exception
     */
    public PropertyAccessException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor for an inner exception and message.
     *
     * @param message is the error message
     */
    public PropertyAccessException(final String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param cause is the inner exception
     */
    public PropertyAccessException(final Throwable cause) {
        super(cause);
    }

    public String getMessage() {
        StringBuilder msg;
        if (super.getMessage() != null) {
            msg = new StringBuilder(super.getMessage());
        } else {
            msg = new StringBuilder("Unexpected exception");
        }
        if (expression != null) {
            msg.append(" [");
            msg.append(expression);
            msg.append(']');
        }
        return msg.toString();
    }

    /**
     * Generates the Not-A-Valid-Property exception
     * @param propertyExpression property expression
     * @return exception exception
     */
    public static PropertyAccessException notAValidProperty(String propertyExpression) {
        return new PropertyAccessException("Property named '" + propertyExpression + "' is not a valid property name for this type");
    }
}
