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

import com.espertech.esper.common.internal.epl.variable.core.VariableCollector;
import com.espertech.esper.common.internal.event.path.EventTypeResolver;

public class EPModuleVariableInitServicesImpl implements EPModuleVariableInitServices {
    private final VariableCollector variableCollector;
    private final EventTypeResolver eventTypeResolver;

    public EPModuleVariableInitServicesImpl(VariableCollector variableCollector, EventTypeResolver eventTypeResolver) {
        this.variableCollector = variableCollector;
        this.eventTypeResolver = eventTypeResolver;
    }

    public VariableCollector getVariableCollector() {
        return variableCollector;
    }

    public EventTypeResolver getEventTypeResolver() {
        return eventTypeResolver;
    }
}
