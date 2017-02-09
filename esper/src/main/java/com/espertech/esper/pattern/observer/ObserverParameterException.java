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
package com.espertech.esper.pattern.observer;

/**
 * Thrown to indicate a validation error in guard parameterization.
 */
public class ObserverParameterException extends Exception {
    private static final long serialVersionUID = -7069000986550813236L;

    /**
     * Ctor.
     *
     * @param message - validation error message
     */
    public ObserverParameterException(String message) {
        super(message);
    }

    /**
     * Ctor.
     *
     * @param message the error message
     * @param cause   the causal exception
     */
    public ObserverParameterException(String message, Throwable cause) {
        super(message, cause);
    }
}
