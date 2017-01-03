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
import org.apache.avro.generic.GenericData;

public class AvroEventBeanGetterDynamicPoly implements AvroEventPropertyGetter {
    private final AvroEventPropertyGetter[] getters;

    public AvroEventBeanGetterDynamicPoly(AvroEventPropertyGetter[] getters) {
        this.getters = getters;
    }

    public Object getAvroFieldValue(GenericData.Record record) {
        return getAvroFieldValuePoly(record, getters);
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        return getAvroFieldValue(record);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true;
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        // TODO
        return null;
    }

    protected static Object getAvroFieldValuePoly(GenericData.Record record, AvroEventPropertyGetter[] getters) {
        if (record == null) {
            return null;
        }
        for (int i = 0; i < getters.length - 1; i++) {
            Object value = getters[i].getAvroFieldValue(record);
            if (!(value instanceof GenericData.Record)) {
                return null;
            }
            record = (GenericData.Record) value;
        }
        return getters[getters.length - 1].getAvroFieldValue(record);
    }

    public boolean isExistsPropertyAvro(GenericData.Record record) {
        return getAvroFieldValuePolyExists(record, getters);
    }

    protected static boolean getAvroFieldValuePolyExists(GenericData.Record record, AvroEventPropertyGetter[] getters) {
        if (record == null) {
            return false;
        }
        for (int i = 0; i < getters.length - 1; i++) {
            Object value = getters[i].getAvroFieldValue(record);
            if (!(value instanceof GenericData.Record)) {
                return false;
            }
            record = (GenericData.Record) value;
        }
        return getters[getters.length - 1].isExistsPropertyAvro(record);
    }
}
