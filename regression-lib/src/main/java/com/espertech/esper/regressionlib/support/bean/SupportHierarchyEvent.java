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

/**
 * Test event; only serializable because it *may* go over the wire  when running remote tests and serialization is just convenient. Serialization generally not used for HA and HA testing.
 */
public class SupportHierarchyEvent implements Serializable {
    private static final long serialVersionUID = -3575580037598025356L;
    private Integer event_criteria_id;
    private Integer priority;
    private Integer parent_event_criteria_id;

    public SupportHierarchyEvent(Integer event_criteria_id, Integer priority, Integer parent_event_criteria_id) {
        this.event_criteria_id = event_criteria_id;
        this.priority = priority;
        this.parent_event_criteria_id = parent_event_criteria_id;
    }

    public Integer getEvent_criteria_id() {
        return event_criteria_id;
    }

    public Integer getPriority() {
        return priority;
    }

    public Integer getParent_event_criteria_id() {
        return parent_event_criteria_id;
    }

    public String toString() {
        return "ecid=" + event_criteria_id +
            " prio=" + priority +
            " parent=" + parent_event_criteria_id;
    }
}
