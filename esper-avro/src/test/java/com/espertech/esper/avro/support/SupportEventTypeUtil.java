/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.avro.support;

import com.espertech.esper.avro.core.AvroEventType;
import com.espertech.esper.core.support.SupportEventAdapterService;
import com.espertech.esper.event.EventTypeMetadata;
import org.apache.avro.Schema;

public class SupportEventTypeUtil {
    public static AvroEventType makeEventType(Schema schema) {
        EventTypeMetadata metadata = EventTypeMetadata.createNonPojoApplicationType(EventTypeMetadata.ApplicationType.AVRO, "typename", true, true, true, false, false);
        return new AvroEventType(metadata, "typename", 1, SupportEventAdapterService.getService(), schema, null, null, null, null);
    }
}
