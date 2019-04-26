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
package com.espertech.esper.common.internal.serde.compiletime.eventtype;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;

import java.util.Set;

public class SerdeEventPropertyDesc {
    private final DataInputOutputSerdeForge forge;
    private final Set<EventType> nestedTypes;

    public SerdeEventPropertyDesc(DataInputOutputSerdeForge forge, Set<EventType> nestedTypes) {
        this.forge = forge;
        this.nestedTypes = nestedTypes;
    }

    public DataInputOutputSerdeForge getForge() {
        return forge;
    }

    public Set<EventType> getNestedTypes() {
        return nestedTypes;
    }
}
