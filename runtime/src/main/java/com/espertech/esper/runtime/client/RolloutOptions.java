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

import com.espertech.esper.runtime.client.util.LockStrategy;
import com.espertech.esper.runtime.client.util.LockStrategyDefault;

/**
 * Option holder for use with {@link EPDeploymentService#rollout} ()}.
 */
public class RolloutOptions {
    private LockStrategy rolloutLockStrategy = LockStrategyDefault.INSTANCE;

    /**
     * Return the rollout lock strategy, the default is {@link LockStrategyDefault}
     *
     * @return lock strategy
     */
    public LockStrategy getRolloutLockStrategy() {
        return rolloutLockStrategy;
    }

    /**
     * Sets the rollout lock strategy, the default is {@link LockStrategyDefault}
     *
     * @param rolloutLockStrategy lock strategy
     */
    public void setRolloutLockStrategy(LockStrategy rolloutLockStrategy) {
        this.rolloutLockStrategy = rolloutLockStrategy;
    }
}
