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

public class EventTypeCollectedSerde {
    private final EventTypeMetadata metadata;
    private final DataInputOutputSerde<Object> underlyingSerde;
    private final Class underlying;

    public EventTypeCollectedSerde(EventTypeMetadata metadata, DataInputOutputSerde<Object> underlyingSerde, Class underlying) {
        this.metadata = metadata;
        this.underlyingSerde = underlyingSerde;
        this.underlying = underlying;
    }

    public EventTypeMetadata getMetadata() {
        return metadata;
    }

    public DataInputOutputSerde<Object> getUnderlyingSerde() {
        return underlyingSerde;
    }

    public Class getUnderlying() {
        return underlying;
    }
}
