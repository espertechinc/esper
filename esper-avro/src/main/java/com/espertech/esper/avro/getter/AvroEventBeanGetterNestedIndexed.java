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

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.PropertyAccessException;
import org.apache.avro.generic.GenericData;

import java.util.Collection;

public class AvroEventBeanGetterNestedIndexed implements EventPropertyGetter {
    private final int top;
    private final int pos;
    private final int index;

    public AvroEventBeanGetterNestedIndexed(int top, int pos, int index) {
        this.top = top;
        this.pos = pos;
        this.index = index;
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        GenericData.Record inner = (GenericData.Record) record.get(top);
        if (inner == null) {
            return null;
        }
        Collection collection = (Collection) inner.get(pos);
        return AvroEventBeanGetterIndexed.getIndexedValue(collection, index);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true;
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        // TODO
        return null;
    }
}
