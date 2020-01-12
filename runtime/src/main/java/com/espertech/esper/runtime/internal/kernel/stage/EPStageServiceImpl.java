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

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.util.StatementProperty;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.internal.util.CollectionUtil;
import com.espertech.esper.common.internal.util.DestroyCallback;
import com.espertech.esper.runtime.client.EPRuntimeDestroyedException;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.stage.EPStage;
import com.espertech.esper.runtime.internal.kernel.service.DeploymentInternal;
import com.espertech.esper.runtime.internal.kernel.service.EPServicesContext;
import com.espertech.esper.runtime.internal.kernel.statement.EPStatementSPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static com.espertech.esper.runtime.internal.kernel.stage.StageStatementHelper.updateStatement;

public class EPStageServiceImpl implements EPStageServiceSPI {
    private final static Logger log = LoggerFactory.getLogger(EPStageServiceImpl.class);
    private final EPServicesContext services;
    private final AtomicBoolean serviceStatusProvider;
    private final Map<String, EPStageImpl> stages = new HashMap<>(CollectionUtil.capacityHashMap(4));

    public EPStageServiceImpl(EPServicesContext services, AtomicBoolean serviceStatusProvider) {
        this.services = services;
        this.serviceStatusProvider = serviceStatusProvider;
    }

    public EPStage getStage(String stageUri) {
        if (stageUri == null) {
            throw new IllegalArgumentException("Stage-URI is null");
        }
        runtimeDestroyedCheck();
        synchronized (stages) {
            EPStageImpl stage = stages.get(stageUri);
            if (stage != null) {
                return stage;
            }
            int stageId = services.getStageRecoveryService().stageAdd(stageUri);
            stage = allocateStage(stageUri, stageId, services.getSchedulingService().getTime());
            stages.put(stageUri, stage);

            Set<EventType> filterServiceTypes = new LinkedHashSet<>(services.getEventTypeRepositoryBus().getAllTypes());
            Supplier<Collection<EventType>> availableTypes = () -> filterServiceTypes;
            stage.getStageSpecificServices().getFilterService().init(availableTypes);
            stage.getStageSpecificServices().getSchedulingService().init();

            return stage;
        }
    }

    public EPStage getExistingStage(String stageUri) {
        if (stageUri == null) {
            throw new IllegalArgumentException("Stage-URI is null");
        }
        runtimeDestroyedCheck();
        synchronized (stages) {
            return stages.get(stageUri);
        }
    }

    public String[] getStageURIs() throws EPRuntimeDestroyedException {
        runtimeDestroyedCheck();
        synchronized (stages) {
            Set<String> uris = stages.keySet();
            return CollectionUtil.toArray(uris);
        }
    }

    public void clear() {
        stages.clear();
    }

    public void recoverStage(String stageURI, int stageId, long stageCurrentTime) {
        EPStageImpl stage = allocateStage(stageURI, stageId, stageCurrentTime);
        stages.put(stageURI, stage);
    }

    public void recoverDeployment(String stageUri, DeploymentInternal deployment) {
        String deploymentId = deployment.getDeploymentId();
        services.getDeploymentLifecycleService().removeDeployment(deploymentId);
        stages.get(stageUri).getStageSpecificServices().getDeploymentLifecycleService().addDeployment(deploymentId, deployment);

        StageSpecificServices stageSpecificServices = stages.get(stageUri).getEventService().getSpecificServices();
        for (EPStatement statement : deployment.getStatements()) {
            EPStatementSPI spi = (EPStatementSPI) statement;
            updateStatement(spi.getStatementContext(), stageSpecificServices);

            if (spi.getProperty(StatementProperty.STATEMENTTYPE) == StatementType.UPDATE) {
                services.getInternalEventRouter().movePreprocessing(spi.getStatementContext(), stageSpecificServices.getInternalEventRouter());
            }
        }
    }

    public void recoveredStageInitialize(Supplier<Collection<EventType>> availableTypes) {
        for (Map.Entry<String, EPStageImpl> stage : stages.entrySet()) {
            stage.getValue().getEventService().getSpecificServices().getFilterService().init(availableTypes);
            stage.getValue().getEventService().getSpecificServices().getSchedulingService().init();
        }
    }

    public boolean isEmpty() {
        return stages.isEmpty();
    }

    public Map<String, EPStageImpl> getStages() {
        return stages;
    }

    public void destroy() {
        EPStageImpl[] stageArray = stages.values().toArray(new EPStageImpl[0]);
        for (EPStageImpl stageEntry : stageArray) {
            try {
                stageEntry.destroyNoCheck();
            } catch (RuntimeException t) {
                log.error("Failed to destroy stage: " + t.getMessage(), t);
            }
        }
    }

    private EPStageImpl allocateStage(String stageUri, int stageId, long stageTime) {
        StageSpecificServices stageSpecificServices = services.getStageRecoveryService().makeSpecificServices(stageId, stageUri, services);
        EPStageEventServiceSPI eventService = services.getStageRecoveryService().makeEventService(stageSpecificServices, stageId, stageUri, services);
        stageSpecificServices.initialize(eventService);
        eventService.setInternalEventRouter(stageSpecificServices.getInternalEventRouter());

        eventService.getSpecificServices().getSchedulingService().setTime(stageTime);
        EPStageDeploymentServiceImpl deploymentService = new EPStageDeploymentServiceImpl(stageUri, services, eventService.getSpecificServices());
        DestroyCallback stageDestroyCallback = new DestroyCallback() {
            public void destroy() {
                synchronized (stages) {
                    services.getStageRecoveryService().stageDestroy(stageUri, stageId);
                    stages.remove(stageUri);
                }
            }
        };
        return new EPStageImpl(stageUri, stageId, services, eventService.getSpecificServices(), eventService, deploymentService, stageDestroyCallback);
    }

    private void runtimeDestroyedCheck() throws EPException {
        if (!serviceStatusProvider.get()) {
            throw new EPRuntimeDestroyedException("Runtime has already been destroyed");
        }
    }
}
