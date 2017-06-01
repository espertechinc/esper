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
import com.espertech.esper.client.EventPropertyGetterIndexed;
import com.espertech.esper.client.PropertyAccessException;
import org.apache.avro.generic.GenericData;

import java.util.Collection;

import static com.espertech.esper.avro.getter.AvroEventBeanGetterIndexed.getAvroIndexedValue;

public class AvroEventBeanGetterIndexedRuntimeKeyed implements EventPropertyGetterIndexed {
    private final int pos;

    public AvroEventBeanGetterIndexedRuntimeKeyed(int pos) {
        this.pos = pos;
    }

    public Object get(EventBean eventBean, int index) throws PropertyAccessException {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        Collection values = (Collection) record.get(pos);
        return getAvroIndexedValue(values, index);
    }
}
