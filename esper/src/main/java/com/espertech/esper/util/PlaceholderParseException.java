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

/**
 * Exception to indicate a parse error in parsing placeholders.
 */
public class PlaceholderParseException extends Exception {
    private static final long serialVersionUID = -2247077092057635902L;

    /**
     * Ctor.
     *
     * @param message is the error message
     */
    public PlaceholderParseException(String message) {
        super(message);
    }
}
