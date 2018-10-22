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

public class RuntimeExtensionServicesNoHA implements RuntimeExtensionServicesSPI {
    public final static RuntimeExtensionServicesNoHA INSTANCE = new RuntimeExtensionServicesNoHA();

    public void init(EPServicesContext servicesContext, EPEventServiceSPI runtimeSPI, EPDeploymentServiceSPI adminSPI) {
    }

    public void destroy() {
    }

    public boolean isHAEnabled() {
        return false;
    }
}
