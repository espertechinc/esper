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
package com.espertech.esper.runtime.internal.kernel.stage;

import com.espertech.esper.common.internal.context.util.InternalEventRouter;
import com.espertech.esper.common.internal.context.util.InternalEventRouterImpl;
import com.espertech.esper.common.internal.util.ManagedReadWriteLock;
import com.espertech.esper.runtime.internal.deploymentlifesvc.DeploymentLifecycleService;
import com.espertech.esper.runtime.internal.deploymentlifesvc.DeploymentLifecycleServiceImpl;
import com.espertech.esper.runtime.internal.filtersvcimpl.FilterServiceSPI;
import com.espertech.esper.runtime.internal.kernel.service.EPServicesContext;
import com.espertech.esper.runtime.internal.kernel.thread.ThreadingService;
import com.espertech.esper.runtime.internal.kernel.thread.ThreadingServiceImpl;
import com.espertech.esper.runtime.internal.metrics.stmtmetrics.MetricReportingServiceImpl;
import com.espertech.esper.runtime.internal.schedulesvcimpl.SchedulingServiceSPI;

public abstract class StageRecoveryServiceBase implements StageRecoveryService {
    protected abstract FilterServiceSPI makeFilterService(int stageId, EPServicesContext servicesContext);
    protected abstract SchedulingServiceSPI makeSchedulingService(int stageId, EPServicesContext servicesContext);

    public final StageSpecificServices makeSpecificServices(int stageId, String stageUri, EPServicesContext servicesContext) {
        ManagedReadWriteLock eventProcessingRWLock = new ManagedReadWriteLock("EventProcLock_" + stageUri, servicesContext.getConfigSnapshot().getRuntime().getThreading().isRuntimeFairlock());
        FilterServiceSPI filterService = makeFilterService(stageId, servicesContext);
        SchedulingServiceSPI schedulingService = makeSchedulingService(stageId, servicesContext);
        DeploymentLifecycleService deploymentLifecycleService = new DeploymentLifecycleServiceImpl(stageId);
        ThreadingService threadingService = new ThreadingServiceImpl(servicesContext.getConfigSnapshot().getRuntime().getThreading());
        MetricReportingServiceImpl metricsReporting = new MetricReportingServiceImpl(servicesContext.getConfigSnapshot().getRuntime().getMetricsReporting(), stageUri);
        InternalEventRouter internalEventRouter = new InternalEventRouterImpl(servicesContext.getEventBeanTypedEventFactory());
        return new StageSpecificServices(deploymentLifecycleService, eventProcessingRWLock, filterService, internalEventRouter, metricsReporting, schedulingService, servicesContext.getStageRuntimeServices(), threadingService);
    }
}
