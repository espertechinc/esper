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
package com.espertech.esper.runtime.client;

import com.espertech.esper.runtime.client.stage.EPStage;

/**
 * Stages are used for staging deployments allowing independent control over event and time for subsets of deployments.
 * <p>
 *     This API is under development for version 8.4 and newer, and is considered UNSTABLE.
 * </p>
 * <p>
 *     Stages are uniquely identified by a stage URI.
 * </p>
 * <p>
 *     Use {@link #getStage(String)} to allocate a stage, of if the stage is already allocated to obtain the stage.
 * </p>
 * <p>
 *     Use {@link #getExistingStage(String)} to obtain an existing stage without allocating.
 * </p>
 */
public interface EPStageService {
    /**
     * Allocate a new stage or returns the existing stage if the stage for the same URI is already allocated.
     * @param stageUri unique identifier
     * @return stage
     * @throws EPRuntimeDestroyedException if the runtime is already destroyed
     */
    EPStage getStage(String stageUri) throws EPRuntimeDestroyedException;

    /**
     * Returns the existing stage for the provided URI, or null if a stage for the URI has not already been allocated.
     * @param stageUri stage URI
     * @return stage
     * @throws EPRuntimeDestroyedException if the runtime is already destroyed
     */
    EPStage getExistingStage(String stageUri) throws EPRuntimeDestroyedException;

    /**
     * Returns the URI values of all stages that are currently allocated.
     * @return stage URIs
     * @throws EPRuntimeDestroyedException if the runtime is already destroyed
     */
    String[] getStageURIs() throws EPRuntimeDestroyedException;
}
