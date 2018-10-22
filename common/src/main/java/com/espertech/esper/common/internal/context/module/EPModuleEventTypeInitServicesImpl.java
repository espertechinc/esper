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
package com.espertech.esper.common.internal.context.module;

import com.espertech.esper.common.internal.event.path.EventTypeCollector;
import com.espertech.esper.common.internal.event.path.EventTypeResolver;

public class EPModuleEventTypeInitServicesImpl implements EPModuleEventTypeInitServices {
    private final EventTypeCollector eventTypeCollector;
    private final EventTypeResolver eventTypeByMetaResolver;

    public EPModuleEventTypeInitServicesImpl(EventTypeCollector eventTypeCollector, EventTypeResolver eventTypeByMetaResolver) {
        this.eventTypeCollector = eventTypeCollector;
        this.eventTypeByMetaResolver = eventTypeByMetaResolver;
    }

    public EventTypeCollector getEventTypeCollector() {
        return eventTypeCollector;
    }

    public EventTypeResolver getEventTypeResolver() {
        return eventTypeByMetaResolver;
    }
}
