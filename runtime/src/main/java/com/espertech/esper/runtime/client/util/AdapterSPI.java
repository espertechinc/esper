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
package com.espertech.esper.runtime.client.util;

import com.espertech.esper.runtime.client.EPRuntime;

/**
 * An Adapter takes some external data, converts it into events, and sends it
 * into the runtime runtime.
 */
public interface AdapterSPI extends Adapter {
    /**
     * An adapter takes an runtime instance to process events.
     *
     * @param runtime is the service instance for the adapter.
     */
    public void setRuntime(EPRuntime runtime);

    /**
     * Returns the runtime instance.
     *
     * @return runtime
     */
    public EPRuntime getRuntime();
}
