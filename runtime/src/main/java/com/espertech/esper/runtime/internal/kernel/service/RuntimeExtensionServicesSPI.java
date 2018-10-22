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
package com.espertech.esper.runtime.internal.kernel.service;

import com.espertech.esper.common.internal.context.module.RuntimeExtensionServices;

public interface RuntimeExtensionServicesSPI extends RuntimeExtensionServices {
    /**
     * Invoked to initialize extension services after runtime services initialization.
     *
     * @param servicesContext the runtime
     * @param runtimeSPI      runtime SPI
     * @param adminSPI        admin SPI
     */
    public void init(EPServicesContext servicesContext, EPEventServiceSPI runtimeSPI, EPDeploymentServiceSPI adminSPI);

    /**
     * Invoked to destroy the extension services, when an existing runtime is initialized.
     */
    public void destroy();

    public boolean isHAEnabled();
}
