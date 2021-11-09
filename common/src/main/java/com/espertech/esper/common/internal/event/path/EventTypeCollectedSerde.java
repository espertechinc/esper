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
package com.espertech.esper.common.internal.event.path;

import com.espertech.esper.common.client.meta.EventTypeMetadata;
import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.client.type.EPTypeClass;

public class EventTypeCollectedSerde {
    private final EventTypeMetadata metadata;
    private final DataInputOutputSerde underlyingSerde;
    private final EPTypeClass underlying;

    public EventTypeCollectedSerde(EventTypeMetadata metadata, DataInputOutputSerde underlyingSerde, EPTypeClass underlying) {
        this.metadata = metadata;
        this.underlyingSerde = underlyingSerde;
        this.underlying = underlying;
    }

    public EventTypeMetadata getMetadata() {
        return metadata;
    }

    public DataInputOutputSerde getUnderlyingSerde() {
        return underlyingSerde;
    }

    public EPTypeClass getUnderlying() {
        return underlying;
    }
}
