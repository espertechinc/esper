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

package com.espertech.esper.avro.getter;

import com.espertech.esper.avro.core.AvroEventPropertyGetter;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.PropertyAccessException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.util.Collection;
import java.util.Map;

public class AvroEventBeanGetterMappedDynamic implements AvroEventPropertyGetter {
    private final String propertyName;
    private final String key;

    public AvroEventBeanGetterMappedDynamic(String propertyName, String key) {
        this.propertyName = propertyName;
        this.key = key;
    }

    public Object getAvroFieldValue(GenericData.Record record) {
        Object value = record.get(propertyName);
        if (value == null || !(value instanceof Map)) {
            return null;
        }
        return AvroEventBeanGetterMapped.getMappedValue((Map)value, key);
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        return getAvroFieldValue(record);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return isExistsPropertyAvro((GenericData.Record) eventBean.getUnderlying());
    }

    public boolean isExistsPropertyAvro(GenericData.Record record) {
        Schema.Field field = record.getSchema().getField(propertyName);
        if (field == null ) {
            return false;
        }
        Object value = record.get(propertyName);
        return value == null || value instanceof Map;
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        return null;
    }
}
