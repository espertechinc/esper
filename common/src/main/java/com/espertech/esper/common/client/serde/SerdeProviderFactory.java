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
package com.espertech.esper.common.client.serde;

/**
 * Factory for serde providers.
 */
public interface SerdeProviderFactory {

    /**
     * Called by the runtimeonce at initialization time, returns a serde provider.
     *
     * @param context runtime contextual information
     * @return serde provide or null if none provided
     */
    SerdeProvider getProvider(SerdeProviderFactoryContext context);
}
