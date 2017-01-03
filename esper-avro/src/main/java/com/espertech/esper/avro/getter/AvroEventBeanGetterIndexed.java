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

import java.util.Collection;
import java.util.List;

public class AvroEventBeanGetterIndexed implements AvroEventPropertyGetter {
    private final int pos;
    private final int index;

    public AvroEventBeanGetterIndexed(int pos, int index) {
        this.pos = pos;
        this.index = index;
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        Collection values = (Collection) record.get(pos);
        return getIndexedValue(values, index);
    }

    public Object getAvroFieldValue(GenericData.Record record) {
        Collection values = (Collection) record.get(pos);
        return getIndexedValue(values, index);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true;
    }

    public boolean isExistsPropertyAvro(GenericData.Record record) {
        return true;
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        // TODO
        throw new UnsupportedOperationException();
    }

    protected static Object getIndexedValue(Collection values, int index) {
        if (values == null) {
            return null;
        }
        if (values instanceof List) {
            return ((List) values).get(index);
        }
        return values.toArray()[index];
    }
}
