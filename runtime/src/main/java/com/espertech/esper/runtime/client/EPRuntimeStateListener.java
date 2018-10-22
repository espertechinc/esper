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
 * A listener interface for callbacks regarding {@link EPRuntime} state changes.
 */
public interface EPRuntimeStateListener {
    /**
     * Invoked before an {@link EPRuntime} is destroyed.
     *
     * @param runtime runtime to be destroyed
     */
    public void onEPRuntimeDestroyRequested(EPRuntime runtime);

    /**
     * Invoked after an existing {@link EPRuntime} is initialized upon completion of a call to initialize.
     *
     * @param runtime runtime that has been successfully initialized
     */
    public void onEPRuntimeInitialized(EPRuntime runtime);
}
