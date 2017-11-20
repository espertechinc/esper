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
package com.espertech.esper.core.context.mgr;

import com.espertech.esper.client.EventBean;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

public class ContextControllerPartitionedState implements Serializable {

    private static final long serialVersionUID = 8041202585242419790L;
    private final Object[] partitionKey;
    private final Map<String, Object> initEvents;

    public ContextControllerPartitionedState(Object[] partitionKey, Map<String, Object> initEvents) {
        this.partitionKey = partitionKey;
        this.initEvents = initEvents;
    }

    public ContextControllerPartitionedState(Object[] partitionKey, String initConditionAsName, EventBean theEvent) {
        this.partitionKey = partitionKey;
        initEvents = initConditionAsName != null ? Collections.singletonMap(initConditionAsName, theEvent) : Collections.emptyMap();
    }

    public Object[] getPartitionKey() {
        return partitionKey;
    }

    public Map<String, Object> getInitEvents() {
        return initEvents;
    }
}
