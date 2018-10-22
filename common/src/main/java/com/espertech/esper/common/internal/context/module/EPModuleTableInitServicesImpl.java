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

import com.espertech.esper.common.internal.epl.table.core.TableCollector;
import com.espertech.esper.common.internal.event.path.EventTypeResolver;

public class EPModuleTableInitServicesImpl implements EPModuleTableInitServices {
    private final TableCollector tableCollector;
    private final EventTypeResolver eventTypeResolver;

    public EPModuleTableInitServicesImpl(TableCollector namedWindowCollector, EventTypeResolver eventTypeResolver) {
        this.tableCollector = namedWindowCollector;
        this.eventTypeResolver = eventTypeResolver;
    }

    public TableCollector getTableCollector() {
        return tableCollector;
    }

    public EventTypeResolver getEventTypeResolver() {
        return eventTypeResolver;
    }
}
