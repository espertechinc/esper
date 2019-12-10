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
 * Deploy exception to indicate that substitution parameter values have not been provided
 */
public class EPDeploySubstitutionParameterException extends EPDeployException {

    /**
     * Ctor
     *
     * @param message message
     * @param rolloutItemNumber rollout item number when using rollout
     */
    public EPDeploySubstitutionParameterException(String message, int rolloutItemNumber) {
        this(message, null, rolloutItemNumber);
    }

    /**
     * Ctor.
     *
     * @param message message
     * @param cause   cause
     * @param rolloutItemNumber rollout item number when using rollout
     */
    public EPDeploySubstitutionParameterException(String message, Throwable cause, int rolloutItemNumber) {
        super("Substitution parameters have not been provided: " + message, cause, rolloutItemNumber);
    }
}
