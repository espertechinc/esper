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
package com.espertech.esper.common.internal.epl.variable.core;

/**
 * Exception indicating a problem in a variable declaration.
 */
public class VariableDeclarationException extends Exception {
    private static final long serialVersionUID = -5419410793398465572L;

    /**
     * Ctor.
     *
     * @param msg the exception message.
     */
    public VariableDeclarationException(String msg) {
        super(msg);
    }
}
