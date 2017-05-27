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
package com.espertech.esper.supportregression.client;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceStateListener;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

public class SupportServiceStateListener implements EPServiceStateListener {
    private List<EPServiceProvider> destroyedEvents = new ArrayList<EPServiceProvider>();
    private List<EPServiceProvider> initializedEvents = new ArrayList<EPServiceProvider>();

    public void onEPServiceDestroyRequested(EPServiceProvider serviceProvider) {
        destroyedEvents.add(serviceProvider);
    }

    public void onEPServiceInitialized(EPServiceProvider serviceProvider) {
        initializedEvents.add(serviceProvider);
    }

    public EPServiceProvider assertOneGetAndResetDestroyedEvents() {
        Assert.assertEquals(1, destroyedEvents.size());
        EPServiceProvider item = destroyedEvents.get(0);
        destroyedEvents.clear();
        return item;
    }

    public EPServiceProvider assertOneGetAndResetInitializedEvents() {
        Assert.assertEquals(1, initializedEvents.size());
        EPServiceProvider item = initializedEvents.get(0);
        initializedEvents.clear();
        return item;
    }

    public List<EPServiceProvider> getDestroyedEvents() {
        return destroyedEvents;
    }

    public List<EPServiceProvider> getInitializedEvents() {
        return initializedEvents;
    }
}
