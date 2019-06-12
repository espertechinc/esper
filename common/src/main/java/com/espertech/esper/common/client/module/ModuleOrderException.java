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
package com.espertech.esper.common.client.module;

/**
 * Exception indicates a problem when determining delpoyment order and uses-dependency checking.
 */
public class ModuleOrderException extends Exception {

    private static final long serialVersionUID = -1358936423218557867L;

    /**
     * Ctor.
     *
     * @param message error message
     */
    public ModuleOrderException(String message) {
        super(message);
    }
}