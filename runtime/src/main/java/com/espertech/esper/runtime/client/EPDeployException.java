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

/**
 * Exception during a deploy operation by {@link EPDeploymentService#deploy}
 */
public class EPDeployException extends Exception {
    private final int rolloutItemNumber;

    /**
     * Ctor.
     *
     * @param message message
     * @param rolloutItemNumber rollout item number when using rollout
     */
    public EPDeployException(String message, int rolloutItemNumber) {
        super(message);
        this.rolloutItemNumber = rolloutItemNumber;
    }

    /**
     * Ctor.
     *
     * @param cause cause
     * @param rolloutItemNumber rollout item number when using rollout
     */
    public EPDeployException(Throwable cause, int rolloutItemNumber) {
        super(cause);
        this.rolloutItemNumber = rolloutItemNumber;
    }

    /**
     * Ctor.
     *
     * @param message message
     * @param cause   cause
     * @param rolloutItemNumber rollout item number when using rollout
     */
    public EPDeployException(String message, Throwable cause, int rolloutItemNumber) {
        super(message, cause);
        this.rolloutItemNumber = rolloutItemNumber;
    }

    /**
     * Returns the rollout item number, or -1 when not using rollout
     * @return number, starting at zero
     */
    public int getRolloutItemNumber() {
        return rolloutItemNumber;
    }
}
