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
package com.espertech.esper.runtime.client;

/**
 * Deploy exception to indicate a problem taking the necessary lock
 */
public class EPDeployLockException extends EPDeployException {
    /**
     * Ctor
     *
     * @param message message
     * @param cause   cause
     */
    public EPDeployLockException(String message, Throwable cause) {
        super(message, cause);
    }
}
