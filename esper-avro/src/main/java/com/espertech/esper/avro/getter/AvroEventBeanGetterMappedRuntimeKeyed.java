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
package com.espertech.esper.avro.getter;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetterMapped;
import com.espertech.esper.client.PropertyAccessException;
import org.apache.avro.generic.GenericData;

import java.util.Map;

import static com.espertech.esper.avro.getter.AvroEventBeanGetterMapped.getMappedValue;

public class AvroEventBeanGetterMappedRuntimeKeyed implements EventPropertyGetterMapped {
    private final int pos;

    public AvroEventBeanGetterMappedRuntimeKeyed(int pos) {
        this.pos = pos;
    }

    public Object get(EventBean eventBean, String mapKey) throws PropertyAccessException {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        Map values = (Map) record.get(pos);
        return getMappedValue(values, mapKey);
    }
}
