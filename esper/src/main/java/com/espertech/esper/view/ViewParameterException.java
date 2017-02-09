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
 * Thrown to indicate a validation error in view parameterization.
 */
public class ViewParameterException extends Exception {
    private static final long serialVersionUID = 8988506719651160950L;

    /**
     * Ctor.
     *
     * @param message - validation error message
     */
    public ViewParameterException(String message) {
        super(message);
    }

    public ViewParameterException(String message, Throwable cause) {
        super(message, cause);
    }
}
