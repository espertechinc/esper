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
package com.espertech.esper.schedule;

/**
 * This exception is thrown to indicate a problem with scheduling.
 */
public class ScheduleServiceException extends RuntimeException {
    private static final long serialVersionUID = 941833133771185390L;

    /**
     * Constructor.
     *
     * @param message is the error message
     */
    public ScheduleServiceException(final String message) {
        super(message);
    }

    /**
     * Constructor for an inner exception and message.
     *
     * @param message is the error message
     * @param cause   is the inner exception
     */
    public ScheduleServiceException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor.
     *
     * @param cause is the inner exception
     */
    public ScheduleServiceException(final Throwable cause) {
        super(cause);
    }
}
