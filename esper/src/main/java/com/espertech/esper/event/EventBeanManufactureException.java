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
package com.espertech.esper.event;

/**
 * Thrown to indicate a problem creating or populating an underlying event objects.
 */
public class EventBeanManufactureException extends Exception {
    private static final long serialVersionUID = -7713342108994541449L;

    /**
     * Ctor.
     *
     * @param message message
     * @param cause   cause
     */
    public EventBeanManufactureException(String message, Throwable cause) {
        super(message, cause);
    }
}
