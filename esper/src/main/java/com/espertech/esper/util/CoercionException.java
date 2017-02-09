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
 * Exception to represent a mismatch in Java types in an expression.
 */
public class CoercionException extends EPException {
    private static final long serialVersionUID = 6562892150768930307L;

    /**
     * Ctor.
     *
     * @param message supplies the detailed description
     */
    public CoercionException(final String message) {
        super(message);
    }
}
