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
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.PropertyAccessException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

public class AvroEventBeanGetterNestedDynamicPoly implements EventPropertyGetter {

    private final String fieldTop;
    private final AvroEventPropertyGetter getter;

    public AvroEventBeanGetterNestedDynamicPoly(String fieldTop, AvroEventPropertyGetter getter) {
        this.fieldTop = fieldTop;
        this.getter = getter;
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        GenericData.Record inner = (GenericData.Record) record.get(fieldTop);
        return inner == null ? null : getter.getAvroFieldValue(inner);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        Schema.Field field = record.getSchema().getField(fieldTop);
        if (field == null) {
            return false;
        }
        Object inner = record.get(fieldTop);
        if (!(inner instanceof GenericData.Record)) {
            return false;
        }
        return getter.isExistsPropertyAvro((GenericData.Record) inner);
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        return null;
    }
}
