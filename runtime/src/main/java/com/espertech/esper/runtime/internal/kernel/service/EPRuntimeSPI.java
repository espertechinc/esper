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

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.runtime.client.EPDeployment;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.internal.kernel.thread.ThreadingService;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public interface EPRuntimeSPI extends EPRuntime {

    void setConfiguration(Configuration configuration);

    void postInitialize();

    void initialize(Long currentTime);

    EPServicesContext getServicesContext();

    AtomicBoolean getServiceStatusProvider();

    EPEventServiceSPI getEventServiceSPI();

    ThreadingService getThreadingService();

    void traverseStatements(BiConsumer<EPDeployment, EPStatement> consumer);

    EPRuntimeStatementSelectionSPI getStatementSelectionSvc();

    EPRuntimeCompileReflectiveSPI getReflectiveCompileSvc();
}
