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
package com.espertech.esper.compiler.client.option;

import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;

/**
 * Provides the environment to {@link BusModifierEventTypeOption}.
 */
public class BusModifierEventTypeContext extends StatementOptionContextBase {

    private final String eventTypeName;

    /**
     * Ctor.
     *
     * @param raw           statement info
     * @param eventTypeName event type name
     */
    public BusModifierEventTypeContext(StatementRawInfo raw, String eventTypeName) {
        super(() -> raw.getCompilable().toEPL(), raw.getStatementName(), raw.getModuleName(), raw.getAnnotations(), raw.getStatementNumber());
        this.eventTypeName = eventTypeName;
    }

    /**
     * Returns the event type name
     *
     * @return event type name
     */
    public String getEventTypeName() {
        return eventTypeName;
    }
}
