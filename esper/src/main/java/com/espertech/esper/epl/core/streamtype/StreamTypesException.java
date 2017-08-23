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

import com.espertech.esper.collection.Pair;

/**
 * Base class for stream and property name resolution errors.
 */
public abstract class StreamTypesException extends Exception {
    private static final long serialVersionUID = -6230611896745775451L;

    private final StreamTypesExceptionSuggestionGen optionalSuggestionGenerator;

    public StreamTypesException(String message, StreamTypesExceptionSuggestionGen optionalSuggestionGenerator) {
        super(message);
        this.optionalSuggestionGenerator = optionalSuggestionGenerator;
    }

    /**
     * Returns the optional suggestion for a matching name.
     *
     * @return suggested match
     */
    public Pair<Integer, String> getOptionalSuggestion() {
        return optionalSuggestionGenerator != null ? optionalSuggestionGenerator.getSuggestion() : null;
    }
}
