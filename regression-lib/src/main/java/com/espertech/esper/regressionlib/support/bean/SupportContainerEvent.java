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
package com.espertech.esper.regressionlib.support.bean;

import java.io.Serializable;

public class SupportContainerEvent implements Serializable {
    private final String containerId;
    private final SupportContainedItem[] items;

    public SupportContainerEvent(String containerId, SupportContainedItem... items) {
        this.containerId = containerId;
        this.items = items;
    }

    public String getContainerId() {
        return containerId;
    }

    public SupportContainedItem[] getItems() {
        return items;
    }
}
