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
package com.espertech.esper.runtime.client.plugin;

import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPRuntimeProvider;

/**
 * Interface for loaders of input/output adapters or any other adapter that may participate in a runtime lifecycle.
 */
public interface PluginLoader {
    /**
     * Initializes the adapter loader.
     * <p>
     * Invoked before the runtime instance is fully initialized. Thereby this is not the place to
     * look up an runtime instance from {@link EPRuntimeProvider}
     * and use it. Use the {@link #postInitialize} method instead.
     *
     * @param context the plug in context
     */
    public void init(PluginLoaderInitContext context);

    /**
     * Called after an runtime instances has fully initialized and is already
     * registered with {@link EPRuntimeProvider}.
     *
     * @since 3.3.0
     */
    public void postInitialize();

    /**
     * Destroys adapter loader and adapters loaded.
     * <p>
     * Invoked upon {@link EPRuntime#destroy} before the runtime instance is actually destroyed.
     * <p>
     * Implementations may block to ensure dependent threads are stopped or other resources released.
     */
    public void destroy();
}
