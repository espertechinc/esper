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
package com.espertech.esper.client.context;

/**
 * Context state event.
 */
public abstract class ContextStateEvent {
    private final String engineURI;
    private final String contextName;

    /**
     * Ctor.
     * @param engineURI engine URI
     * @param contextName context name
     */
    public ContextStateEvent(String engineURI, String contextName) {
        this.engineURI = engineURI;
        this.contextName = contextName;
    }

    /**
     * Returns the context name
     * @return context name
     */
    public String getContextName() {
        return contextName;
    }

    /**
     * Returns the engine URI
     * @return engine URI
     */
    public String getEngineURI() {
        return engineURI;
    }
}
