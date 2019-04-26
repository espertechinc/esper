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
package com.espertech.esper.common.internal.serde.runtime.eventtype;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.event.bean.service.BeanEventTypeFactoryPrivate;
import com.espertech.esper.common.internal.event.path.EventTypeCollectedSerde;

import java.util.List;
import java.util.Map;

public interface EventTypeSerdeRepository {
    void addSerdes(String deploymentId, List<EventTypeCollectedSerde> serdes, Map<String, EventType> moduleEventTypes, BeanEventTypeFactoryPrivate beanEventTypeFactory);
    void removeSerdes(String deploymentId);
}
