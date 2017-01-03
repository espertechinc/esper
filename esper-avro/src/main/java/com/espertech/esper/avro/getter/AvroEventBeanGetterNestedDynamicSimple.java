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

public class AvroEventBeanGetterNestedDynamicSimple implements EventPropertyGetter {

    private final int posTop;
    private final String propertyName;

    public AvroEventBeanGetterNestedDynamicSimple(int posTop, String propertyName) {
        this.posTop = posTop;
        this.propertyName = propertyName;
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        GenericData.Record inner = (GenericData.Record) record.get(posTop);
        if (inner == null) {
            return null;
        }
        return inner.get(propertyName);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        GenericData.Record inner = (GenericData.Record) record.get(posTop);
        if (inner == null) {
            return false;
        }
        return inner.getSchema().getField(propertyName) != null;
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        return null;
    }
}
