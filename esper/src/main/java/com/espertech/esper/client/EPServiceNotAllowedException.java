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
 * This exception is thrown to indicate that the operation is not allowed.
 */
public class EPServiceNotAllowedException extends RuntimeException {
    private static final long serialVersionUID = 7753532348030296756L;

    /**
     * Ctor.
     *
     * @param message message
     */
    public EPServiceNotAllowedException(String message) {
        super(message);
    }
}