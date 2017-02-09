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
package com.espertech.esper.core.service;

/**
 * Marker interface for extension services that provide additional engine or statement-level extensions,
 * such as views backed by a write-behind store.
 */
public interface EngineLevelExtensionServicesContext {
    /**
     * Invoked to initialize extension services after engine services initialization.
     *
     * @param engine     the engine
     * @param runtimeSPI runtime SPI
     * @param adminSPI   admin SPI
     */
    public void init(EPServicesContext engine, EPRuntimeSPI runtimeSPI, EPAdministratorSPI adminSPI);

    /**
     * Invoked to destroy the extension services, when an existing engine is initialized.
     */
    public void destroy();

    public boolean isHAEnabled();
}
