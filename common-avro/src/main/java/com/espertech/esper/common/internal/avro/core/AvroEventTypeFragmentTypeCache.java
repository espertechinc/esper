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
package com.espertech.esper.common.internal.avro.core;

import com.espertech.esper.common.internal.event.avro.AvroSchemaEventType;

import java.util.HashMap;
import java.util.Map;

public class AvroEventTypeFragmentTypeCache {
    private Map<String, AvroSchemaEventType> cacheByRecordSchemaName;


    public AvroSchemaEventType get(String recordSchemaName) {
        if (cacheByRecordSchemaName == null) {
            cacheByRecordSchemaName = new HashMap<>();
        }
        return cacheByRecordSchemaName.get(recordSchemaName);
    }

    public void add(String recordSchemaName, AvroSchemaEventType fragmentType) {
        if (cacheByRecordSchemaName == null) {
            cacheByRecordSchemaName = new HashMap<>();
        }
        cacheByRecordSchemaName.put(recordSchemaName, fragmentType);
    }
}
