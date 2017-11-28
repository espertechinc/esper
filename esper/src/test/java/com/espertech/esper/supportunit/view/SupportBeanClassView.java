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
import com.espertech.esper.view.View;

import java.util.LinkedList;
import java.util.List;

public class SupportBeanClassView extends SupportBaseView {
    private static List<SupportBeanClassView> instances = new LinkedList<SupportBeanClassView>();
    private Class clazz;

    public SupportBeanClassView() {
        instances.add(this);
    }

    public SupportBeanClassView(Class clazz) {
        super(SupportEventTypeFactory.createBeanType(clazz));
        this.clazz = clazz;
        instances.add(this);
    }

    public void update(EventBean[] newData, EventBean[] oldData) {
        super.setInvoked(true);
        this.lastNewData = newData;
        this.lastOldData = oldData;

        updateChildren(newData, oldData);
    }

    public static List<SupportBeanClassView> getInstances() {
        return instances;
    }
}
