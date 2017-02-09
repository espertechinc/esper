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
package com.espertech.esper.client;

/**
 * Isolated service provider for controlling event visibility and scheduling on a statement level.
 */
public interface EPServiceProviderIsolated {
    /**
     * Returns a class instance of EPRuntime.
     *
     * @return an instance of EPRuntime
     */
    public EPRuntimeIsolated getEPRuntime();

    /**
     * Returns a class instance of EPAdministrator.
     *
     * @return an instance of EPAdministrator
     */
    public EPAdministratorIsolated getEPAdministrator();

    /**
     * Name of isolated service.
     *
     * @return isolated service name
     */
    public String getName();

    /**
     * Destroy the isolated service returning all statements to the engine.
     */
    public void destroy();
}
