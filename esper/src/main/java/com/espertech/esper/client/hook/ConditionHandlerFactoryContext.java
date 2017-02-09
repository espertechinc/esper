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
package com.espertech.esper.client.hook;

/**
 * Context provided to {@link ConditionHandlerFactory} implementations providing
 * engine contextual information.
 */
public class ConditionHandlerFactoryContext {
    private final String engineURI;

    /**
     * Ctor.
     *
     * @param engineURI engine URI
     */
    public ConditionHandlerFactoryContext(String engineURI) {
        this.engineURI = engineURI;
    }

    /**
     * Returns the engine URI.
     *
     * @return engine URI
     */
    public String getEngineURI() {
        return engineURI;
    }
}
