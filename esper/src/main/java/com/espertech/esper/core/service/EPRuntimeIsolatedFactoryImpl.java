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

public class EPRuntimeIsolatedFactoryImpl implements EPRuntimeIsolatedFactory {
    public EPRuntimeIsolatedSPI make(EPIsolationUnitServices isolatedServices, EPServicesContext unisolatedSvc) {
        return new EPRuntimeIsolatedImpl(isolatedServices, unisolatedSvc);
    }
}
