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

import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowCollector;
import com.espertech.esper.common.internal.event.path.EventTypeResolver;

public class EPModuleNamedWindowInitServicesImpl implements EPModuleNamedWindowInitServices {
    private final NamedWindowCollector namedWindowCollector;
    private final EventTypeResolver eventTypeResolver;

    public EPModuleNamedWindowInitServicesImpl(NamedWindowCollector namedWindowCollector, EventTypeResolver eventTypeResolver) {
        this.namedWindowCollector = namedWindowCollector;
        this.eventTypeResolver = eventTypeResolver;
    }

    public NamedWindowCollector getNamedWindowCollector() {
        return namedWindowCollector;
    }

    public EventTypeResolver getEventTypeResolver() {
        return eventTypeResolver;
    }
}
