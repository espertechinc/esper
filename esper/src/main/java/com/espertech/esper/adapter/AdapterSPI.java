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
package com.espertech.esper.adapter;

import com.espertech.esper.client.EPServiceProvider;

/**
 * An Adapter takes some external data, converts it into events, and sends it
 * into the runtime engine.
 */
public interface AdapterSPI {
    /**
     * An adapter takes an engine instance to process events.
     *
     * @param epService is the service instance for the adapter.
     */
    public void setEPServiceProvider(EPServiceProvider epService);

    /**
     * Returns the engine instance.
     *
     * @return engine
     */
    public EPServiceProvider getEPServiceProvider();
}
