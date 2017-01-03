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

public class AvroEventBeanGetterNestedMultiLevel implements EventPropertyGetter {
    private final int top;
    private final int[] path;

    public AvroEventBeanGetterNestedMultiLevel(int top, int[] path) {
        this.top = top;
        this.path = path;
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        GenericData.Record inner = (GenericData.Record) record.get(top);
        if (inner == null) {
            return null;
        }
        for (int i = 0; i < path.length - 1; i++) {
            inner = (GenericData.Record) inner.get(path[i]);
            if (inner == null) {
                return null;
            }
        }
        return inner.get(path[path.length - 1]);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true;
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        // TODO
        return null;
    }
}
