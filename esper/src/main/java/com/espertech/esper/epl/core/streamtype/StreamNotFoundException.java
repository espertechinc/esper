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
package com.espertech.esper.epl.core.streamtype;

/**
 * Exception to indicate that a stream name could not be resolved.
 */
public class StreamNotFoundException extends StreamTypesException {
    private static final long serialVersionUID = -665030219652415977L;

    public StreamNotFoundException(String message, StreamTypesExceptionSuggestionGen msgGen) {
        super(message, msgGen);
    }
}
