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
 * Indicates that a variable was not found.
 */
public class VariableNotFoundException extends EPException {
    private static final long serialVersionUID = 8865682065184217658L;

    /**
     * Ctor.
     *
     * @param message supplies exception details
     */
    public VariableNotFoundException(final String message) {
        super(message);
    }
}
