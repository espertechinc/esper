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
package com.espertech.esper.util;

/**
 * Exception to represent a circular dependency.
 */
public class GraphCircularDependencyException extends Exception {
    private static final long serialVersionUID = 2510973783421132985L;

    /**
     * Ctor.
     *
     * @param message supplies the detailed description
     */
    public GraphCircularDependencyException(final String message) {
        super(message);
    }

    /**
     * Ctor.
     *
     * @param message supplies the detailed description
     * @param cause   the exception cause
     */
    public GraphCircularDependencyException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
