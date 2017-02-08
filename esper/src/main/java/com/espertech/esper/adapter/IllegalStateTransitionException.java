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
package com.espertech.esper.adapter;

import com.espertech.esper.client.EPException;

/**
 * Thrown when an illegal Adapter state transition is attempted.
 */
public class IllegalStateTransitionException extends EPException {
    private static final long serialVersionUID = -2496061738130404650L;

    /**
     * @param message - an explanation of the cause of the exception
     */
    public IllegalStateTransitionException(String message) {
        super(message);
    }
}
