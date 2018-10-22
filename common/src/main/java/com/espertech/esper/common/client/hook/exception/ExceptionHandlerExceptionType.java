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
package com.espertech.esper.common.client.hook.exception;

/**
 * Indicates the phase during which and exception was encountered.
 */
public enum ExceptionHandlerExceptionType {
    /**
     * Exception occurred during event processing
     */
    PROCESS,

    /**
     * Exception occurred upon undeploy
     */
    UNDEPLOY;
}
