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

import com.espertech.esper.common.internal.context.compile.ContextCollector;
import com.espertech.esper.common.internal.event.path.EventTypeResolver;

public class EPModuleContextInitServicesImpl implements EPModuleContextInitServices {
    private final ContextCollector contextCollector;
    private final EventTypeResolver eventTypeResolver;

    public EPModuleContextInitServicesImpl(ContextCollector contextCollector, EventTypeResolver eventTypeResolver) {
        this.contextCollector = contextCollector;
        this.eventTypeResolver = eventTypeResolver;
    }

    public ContextCollector getContextCollector() {
        return contextCollector;
    }

    public EventTypeResolver getEventTypeResolver() {
        return eventTypeResolver;
    }
}
