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
package com.espertech.esper.supportunit.view;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.supportunit.event.SupportEventTypeFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SupportMapView extends SupportBaseView {
    private static List<SupportMapView> instances = new LinkedList<SupportMapView>();

    public SupportMapView() {
        instances.add(this);
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        super.setInvoked(true);
        this.lastNewData = newData;
        this.lastOldData = oldData;

        updateChildren(newData, oldData);
    }

    public SupportMapView(Map<String, Object> eventTypeMap) {
        super(SupportEventTypeFactory.createMapType(eventTypeMap));
        instances.add(this);
    }

    public static List<SupportMapView> getInstances() {
        return instances;
    }
}
