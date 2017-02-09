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
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.PropertyAccessException;
import org.apache.avro.generic.GenericData;

import java.util.Collection;

import static com.espertech.esper.avro.getter.AvroEventBeanGetterIndexed.getIndexedValue;

public class AvroEventBeanGetterNestedIndexRootedMultilevel implements EventPropertyGetter {
    private final int posTop;
    private final int index;
    private final AvroEventPropertyGetter[] nested;

    public AvroEventBeanGetterNestedIndexRootedMultilevel(int posTop, int index, AvroEventPropertyGetter[] nested) {
        this.posTop = posTop;
        this.index = index;
        this.nested = nested;
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        Object value = navigate(eventBean);
        if (value == null || !(value instanceof GenericData.Record)) {
            return null;
        }
        return nested[nested.length - 1].getAvroFieldValue((GenericData.Record) value);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true;
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        Object value = navigate(eventBean);
        if (value == null || !(value instanceof GenericData.Record)) {
            return null;
        }
        return nested[nested.length - 1].getAvroFragment((GenericData.Record) value);
    }

    private Object navigate(EventBean eventBean) {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        Collection values = (Collection) record.get(posTop);
        Object value = getIndexedValue(values, index);
        if (value == null || !(value instanceof GenericData.Record)) {
            return null;
        }
        for (int i = 0; i < nested.length - 1; i++) {
            value = nested[i].getAvroFieldValue((GenericData.Record) value);
            if (value == null || !(value instanceof GenericData.Record)) {
                return null;
            }
        }
        return value;
    }
}
