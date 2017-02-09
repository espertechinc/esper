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

import com.espertech.esper.client.EPException;

/**
 * Exception to represent an error instantiating a class from a class name.
 */
public class ClassInstantiationException extends EPException {
    private static final long serialVersionUID = 7562672676472269666L;

    /**
     * Ctor.
     *
     * @param message supplies the detailed description
     */
    public ClassInstantiationException(final String message) {
        super(message);
    }

    /**
     * Ctor.
     *
     * @param message supplies the detailed description
     * @param cause   the exception cause
     */
    public ClassInstantiationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
