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
package com.espertech.esper.epl.variable;

/**
 * Exception indicating a variable does not exists.
 */
public class VariableNotFoundException extends VariableDeclarationException {
    private static final long serialVersionUID = -6559271999235865015L;

    /**
     * Ctor.
     *
     * @param msg the exception message.
     */
    public VariableNotFoundException(String msg) {
        super(msg);
    }
}
