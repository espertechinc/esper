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

import com.espertech.esper.common.internal.schedule.TimeSourceService;
import com.espertech.esper.runtime.internal.filtersvcimpl.FilterServiceLockCoarse;
import com.espertech.esper.runtime.internal.filtersvcimpl.FilterServiceSPI;
import com.espertech.esper.runtime.internal.kernel.service.EPServicesContext;
import com.espertech.esper.runtime.internal.schedulesvcimpl.SchedulingServiceImpl;
import com.espertech.esper.runtime.internal.schedulesvcimpl.SchedulingServiceSPI;

import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class StageRecoveryServiceImpl extends StageRecoveryServiceBase implements StageRecoveryService {
    public final static StageRecoveryServiceImpl INSTANCE = new StageRecoveryServiceImpl();

    private int currentStageNumber;
    private Map<String, String> deploymentIdStages;

    private StageRecoveryServiceImpl() {
    }

    public EPStageEventServiceSPI makeEventService(StageSpecificServices stageSpecificServices, int stageId, String stageUri, EPServicesContext servicesContext) {
        return new EPStageEventServiceImpl(stageSpecificServices, servicesContext.getStageRuntimeServices(), stageUri);
    }

    public String deploymentGetStage(String deploymentId) {
        if (deploymentIdStages == null) {
            return null;
        }
        return deploymentIdStages.get(deploymentId);
    }

    public Iterator<Map.Entry<String, Integer>> stagesIterate() {
        // no action
        return Collections.emptyIterator();
    }

    public int stageAdd(String stageUri) {
        return ++currentStageNumber;
    }

    public void stageDestroy(String stageUri, int stageId) {
        // no action
    }

    public void deploymentSetStage(String deploymentId, String stageUri) {
        initDeploymentStages();
        deploymentIdStages.put(deploymentId, stageUri);
    }

    public void deploymentRemoveFromStages(String deploymentId) {
        initDeploymentStages();
        deploymentIdStages.remove(deploymentId);
        // no action
    }

    protected FilterServiceSPI makeFilterService(int stageId, EPServicesContext servicesContext) {
        return new FilterServiceLockCoarse(stageId);
    }

    protected SchedulingServiceSPI makeSchedulingService(int stageId, EPServicesContext servicesContext) {
        ZoneId zoneId = servicesContext.getClasspathImportServiceRuntime().getTimeZone().toZoneId();
        return new SchedulingServiceImpl(stageId, new TimeSourceService() {
            public long getTimeMillis() {
                return servicesContext.getSchedulingService().getTime() + 1;
            }
        }, zoneId);
    }

    private void initDeploymentStages() {
        if (deploymentIdStages == null) {
            deploymentIdStages = new HashMap<>();
        }
    }
}
