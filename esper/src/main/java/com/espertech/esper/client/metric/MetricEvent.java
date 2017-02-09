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
package com.espertech.esper.client.metric;

/**
 * Base metric event.
 */
public abstract class MetricEvent {
    private String engineURI;

    /**
     * Ctor.
     *
     * @param engineURI the engine URI
     */
    protected MetricEvent(String engineURI) {
        this.engineURI = engineURI;
    }

    /**
     * Returns the engine URI.
     *
     * @return uri
     */
    public String getEngineURI() {
        return engineURI;
    }
}
