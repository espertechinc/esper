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
package com.espertech.esper.schedule;

/**
 * Interface for a service that allocated schedule buckets for statements,
 * for controlling timer callback orders.
 */
public interface SchedulingMgmtService {
    /**
     * Returns a bucket from which slots can be allocated for ordering concurrent callbacks.
     *
     * @return bucket
     */
    public ScheduleBucket allocateBucket();

    /**
     * Destroy the service.
     */
    public void destroy();
}