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
package com.espertech.esper.common.internal.event.eventtyperepo;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.event.core.EventTypeNameResolver;

import java.util.Collection;
import java.util.Map;

public interface EventTypeRepository extends EventTypeNameResolver {
    Collection<? extends EventType> getAllTypes();

    EventType getTypeById(long eventTypeIdPublic);

    void addType(EventType eventType);

    Map<String, EventType> getNameToTypeMap();
}
