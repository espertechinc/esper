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
package com.espertech.esper.client.deploy;

/**
 * Exception thrown when an EPL text could not be parsed.
 */
public class ParseException extends Exception {

    private static final long serialVersionUID = 566081132579187386L;

    /**
     * Ctor.
     *
     * @param message error message
     */
    public ParseException(String message) {
        super(message);
    }
}
