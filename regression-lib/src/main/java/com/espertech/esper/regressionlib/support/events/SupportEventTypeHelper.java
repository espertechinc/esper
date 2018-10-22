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
package com.espertech.esper.regressionlib.support.events;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.meta.EventTypeIdPair;
import com.espertech.esper.common.internal.context.util.StatementContext;

import static org.junit.Assert.fail;

public class SupportEventTypeHelper {
    public static EventTypeIdPair getTypeIdForName(StatementContext statementContext, String eventTypeName) {
        EventType type = statementContext.getEventTypeRepositoryPreconfigured().getTypeByName(eventTypeName);
        if (type == null) {
            fail("Type by name '" + eventTypeName + "' not found as a public type");
        }
        return type.getMetadata().getEventTypeIdPair();
    }

    public static EventType getEventTypeForTypeId(StatementContext statementContext, EventTypeIdPair key) {
        return statementContext.getEventTypeRepositoryPreconfigured().getTypeById(key.getPublicId());
    }
}
