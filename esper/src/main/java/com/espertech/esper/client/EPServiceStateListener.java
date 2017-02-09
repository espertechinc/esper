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
package com.espertech.esper.client;

/**
 * A listener interface for callbacks regarding {@link EPServiceProvider} state changes.
 */
public interface EPServiceStateListener {
    /**
     * Invoked before an {@link EPServiceProvider} is destroyed.
     *
     * @param serviceProvider service provider to be destroyed
     */
    public void onEPServiceDestroyRequested(EPServiceProvider serviceProvider);

    /**
     * Invoked after an existing {@link EPServiceProvider} is initialized upon completion of a call to initialize.
     *
     * @param serviceProvider service provider that has been successfully initialized
     */
    public void onEPServiceInitialized(EPServiceProvider serviceProvider);
}
