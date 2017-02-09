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
package com.espertech.esper.pattern.guard;

/**
 * Thrown to indicate a validation error in guard parameterization.
 */
public class GuardParameterException extends Exception {
    private static final long serialVersionUID = -7356613905301615187L;

    /**
     * Ctor.
     *
     * @param message - validation error message
     */
    public GuardParameterException(String message) {
        super(message);
    }
}
