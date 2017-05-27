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
package com.espertech.esper.supportregression.bean;

import java.io.Serializable;

public class SupportBeanTimestamp implements Serializable {
    private String id;
    private long timestamp;
    private String groupId;

    public SupportBeanTimestamp(String id, long timestamp) {
        this.id = id;
        this.timestamp = timestamp;
    }

    public SupportBeanTimestamp(String id, String groupId, long timestamp) {
        this.id = id;
        this.groupId = groupId;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getGroupId() {
        return groupId;
    }
}
