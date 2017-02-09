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
package com.espertech.esper.view;

/**
 * This exception is thrown to indicate a problem with a view expression.
 */
public final class ViewProcessingException extends RuntimeException {
    private static final long serialVersionUID = -4098966582612999347L;

    /**
     * Constructor.
     *
     * @param message is the error message
     */
    public ViewProcessingException(final String message) {
        super(message);
    }

    /**
     * Constructor for an inner exception and message.
     *
     * @param message is the error message
     * @param cause   is the inner exception
     */
    public ViewProcessingException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor.
     *
     * @param cause is the inner exception
     */
    public ViewProcessingException(final Throwable cause) {
        super(cause);
    }
}
