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
package com.espertech.esper.core.support;

import com.espertech.esper.client.util.ClassForNameProviderDefault;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventAdapterServiceImpl;
import com.espertech.esper.event.EventTypeIdGeneratorImpl;
import com.espertech.esper.event.avro.EventAdapterAvroHandler;
import com.espertech.esper.event.avro.EventAdapterAvroHandlerUnsupported;
import com.espertech.esper.util.JavaClassHelper;

public class SupportEventAdapterService {
    private static EventAdapterService eventAdapterService;

    static {
        eventAdapterService = allocate();
    }

    public static void reset() {
        eventAdapterService = allocate();
    }

    public static EventAdapterService getService() {
        return eventAdapterService;
    }

    private static EventAdapterService allocate() {
        EventAdapterAvroHandler avroHandler = EventAdapterAvroHandlerUnsupported.INSTANCE;
        try {
            avroHandler = (EventAdapterAvroHandler) JavaClassHelper.instantiate(EventAdapterAvroHandler.class, EventAdapterAvroHandler.HANDLER_IMPL, ClassForNameProviderDefault.INSTANCE);
        } catch (Throwable t) {
        }
        return new EventAdapterServiceImpl(new EventTypeIdGeneratorImpl(), 5, avroHandler, SupportEngineImportServiceFactory.make());
    }
}
