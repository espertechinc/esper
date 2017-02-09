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

public class AvroEventBeanGetterSimpleDynamic implements AvroEventPropertyGetter {
    private final String propertyName;

    public AvroEventBeanGetterSimpleDynamic(String propertyName) {
        this.propertyName = propertyName;
    }

    public Object getAvroFieldValue(GenericData.Record record) throws PropertyAccessException {
        return record.get(propertyName);
    }

    public Object get(EventBean theEvent) {
        return getAvroFieldValue((GenericData.Record) theEvent.getUnderlying());
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return isExistsPropertyAvro((GenericData.Record) eventBean.getUnderlying());
    }

    public boolean isExistsPropertyAvro(GenericData.Record record) {
        return record.getSchema().getField(propertyName) != null;
    }

    public Object getFragment(EventBean obj) {
        return null;
    }

    public Object getAvroFragment(GenericData.Record record) {
        return null;
    }
}

