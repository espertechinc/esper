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
package com.espertech.esper.regressionlib.support.client;

import com.espertech.esper.runtime.client.DeploymentStateEvent;
import com.espertech.esper.runtime.client.DeploymentStateEventDeployed;
import com.espertech.esper.runtime.client.DeploymentStateEventUndeployed;
import com.espertech.esper.runtime.client.DeploymentStateListener;

import java.util.ArrayList;
import java.util.List;

public class SupportDeploymentStateListener implements DeploymentStateListener {
    private static List<DeploymentStateEvent> events = new ArrayList<>();

    public void onDeployment(DeploymentStateEventDeployed event) {
        events.add(event);
    }

    public void onUndeployment(DeploymentStateEventUndeployed event) {
        events.add(event);
    }

    public static List<DeploymentStateEvent> getEvents() {
        return events;
    }
}
