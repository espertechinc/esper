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

import com.espertech.esper.avro.core.AvroEventPropertyGetter;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.PropertyAccessException;
import org.apache.avro.generic.GenericData;

import java.util.Map;

public class AvroEventBeanGetterMapped implements AvroEventPropertyGetter {
    private final int pos;
    private final String key;

    public AvroEventBeanGetterMapped(int pos, String key) {
        this.pos = pos;
        this.key = key;
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        Map values = (Map) record.get(pos);
        return getMappedValue(values, key);
    }

    public Object getAvroFieldValue(GenericData.Record record) {
        Map values = (Map) record.get(pos);
        return getMappedValue(values, key);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true;
    }

    public boolean isExistsPropertyAvro(GenericData.Record record) {
        return true;
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        return null;
    }

    public Object getAvroFragment(GenericData.Record record) {
        return null;
    }

    protected static Object getMappedValue(Map map, String key) {
        if (map == null) {
            return null;
        }
        return map.get(key);
    }
}
