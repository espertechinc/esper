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
package com.espertech.esper.common.client.serde;

import com.espertech.esper.common.client.EventBean;

import java.io.DataOutput;
import java.io.IOException;

/**
 * Collated writer for events, intented for passing along as a parameter and not intended to be implemented by an application
 */
public interface EventBeanCollatedWriter {
    /**
     * Write event collated.
     *
     * @param event       to write
     * @param dataOutput  destination
     * @param pageFullKey page key
     * @throws IOException for io exceptions
     */
    void writeCollatedEvent(EventBean event, DataOutput dataOutput, byte[] pageFullKey) throws IOException;

    /**
     * Write event id collated.
     *
     * @param id          to write
     * @param output      destination
     * @param pageFullKey page key
     * @throws IOException for io exceptions
     */
    void writeCollatedOID(long id, DataOutput output, byte[] pageFullKey) throws IOException;
}
