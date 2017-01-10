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
import org.apache.avro.generic.GenericData;

import static com.espertech.esper.avro.getter.AvroEventBeanGetterDynamicPoly.*;

public class AvroEventBeanGetterNestedPoly implements EventPropertyGetter {
    private final int top;
    private final AvroEventPropertyGetter[] getters;

    public AvroEventBeanGetterNestedPoly(int top, AvroEventPropertyGetter[] getters) {
        this.top = top;
        this.getters = getters;
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        GenericData.Record inner = (GenericData.Record) record.get(top);
        return getAvroFieldValuePoly(inner, getters);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        GenericData.Record inner = (GenericData.Record) record.get(top);
        return getAvroFieldValuePolyExists(inner, getters);
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        GenericData.Record record = (GenericData.Record) eventBean.getUnderlying();
        GenericData.Record inner = (GenericData.Record) record.get(top);
        return getAvroFieldFragmentPoly(inner, getters);
    }
}
