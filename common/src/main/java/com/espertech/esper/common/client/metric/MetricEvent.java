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
package com.espertech.esper.common.client.metric;

import java.io.Serializable;

/**
 * Base metric event.
 */
public abstract class MetricEvent implements Serializable {
    private static final long serialVersionUID = -7971073046305352106L;
    private String runtimeURI;

    /**
     * Ctor.
     *
     * @param runtimeURI the runtime URI
     */
    protected MetricEvent(String runtimeURI) {
        this.runtimeURI = runtimeURI;
    }

    /**
     * Returns the runtime URI.
     *
     * @return uri
     */
    public String getRuntimeURI() {
        return runtimeURI;
    }
}
