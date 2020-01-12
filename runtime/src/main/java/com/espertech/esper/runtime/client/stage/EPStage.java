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
package com.espertech.esper.runtime.client.stage;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.runtime.client.EPRuntime;

import java.util.Collection;

/**
 * A stage allows staging and unstageing deployments, allowing independent control over event and time for the deployments.
 * <p>
 *     This API is under development for version 8.4 and newer, and is considered UNSTABLE.
 * </p>
 */
public interface EPStage {
    /**
     * Stage deployments.
     * <p>
     *     This effectively removes the deployment from the runtime and adds it to the stage's deployments.
     *     The deployment can be obtained from {@link EPStageDeploymentService} and can no longer be obtained from {@link EPRuntime#getDeploymentService()}.
     * </p>
     * <p>
     *     The staged deployments only receive events that the application sends using the {@link EPStageEventService} for this stage.
     *     The staged deployments only advance time according to the application advancing time using the {@link EPStageEventService} for this stage.
     * </p>
     * <p>
     *     The staged deployments no longer receive events that the application sends into the runtime {@link EPRuntime#getEventService()}.
     *     The staged deployments no longer advance time according to time advancing for the runtime {@link EPRuntime#getEventService()}.
     * </p>
     * <p>
     *     Requires that dependent public or protected (not preconfigured) EPL objects are also getting staged.
     * </p>
     * @param deploymentIds deployment ids of deployments to stage
     * @throws EPStageException if preconditions validation fails or a deployment does not exist
     * @throws EPStageDestroyedException if the stage is already destroyed
     */
    void stage(Collection<String> deploymentIds) throws EPStageException, EPStageDestroyedException;

    /**
     * Un-stage deployments.
     * <p>
     *     This effectively removes the deployment from the stage and adds it to the runtime deployments.
     *     The deployment can be obtained from {@link EPRuntime#getDeploymentService()} and can no longer be obtained from {@link EPStageDeploymentService}.
     * </p>
     * <p>
     *     The un-staged deployments only receive events that the application sends using the runtime {@link EPRuntime#getEventService()}.
     *     The un-staged deployments only advance time according to the application advancing time using the runtime {@link EPRuntime#getEventService()}.
     * </p>
     * <p>
     *     The staged deployments no longer receive events that the application sends into the {@link EPStageEventService} for this stage.
     *     The staged deployments no longer advance time according to time advancing for the {@link EPStageEventService} for this stage.
     * </p>
     * <p>
     *     Requires that dependent public or protected (not preconfigured) EPL objects are also getting un-staged.
     * </p>
     * @param deploymentIds deployment ids of deployments to un-stage
     * @throws EPStageException if preconditions validation fails or a deployment does not exist
     * @throws EPStageDestroyedException if the stage is already destroyed
     */
    void unstage(Collection<String> deploymentIds) throws EPStageException, EPStageDestroyedException;

    /**
     * Returns the stage's deployment service that provides information about staged deployments.
     * @return stage deployment service
     * @throws EPStageDestroyedException if the stage is already destroyed
     */
    EPStageDeploymentService getDeploymentService() throws EPStageDestroyedException;

    /**
     * Returns the stage's event service that can be used to send events to the stage and to advance time for the stage.
     * @return stage event service
     * @throws EPStageDestroyedException if the stage is already destroyed
     */
    EPStageEventService getEventService() throws EPStageDestroyedException;

    /**
     * Destroy the stage.
     * <p>
     *     Requires that any deployments are un-staged.
     * </p>
     * @throws EPException when the destroy operation fails
     */
    void destroy() throws EPException;

    /**
     * Returns the stage unique identifier URI.
     * @return uri
     */
    String getURI();
}
