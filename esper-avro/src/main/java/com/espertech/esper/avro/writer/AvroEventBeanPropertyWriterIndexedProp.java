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
package com.espertech.esper.avro.writer;

import org.apache.avro.generic.GenericData;

import java.util.List;

public class AvroEventBeanPropertyWriterIndexedProp extends AvroEventBeanPropertyWriter {

    private final int indexTarget;

    public AvroEventBeanPropertyWriterIndexedProp(int propertyIndex, int indexTarget) {
        super(propertyIndex);
        this.indexTarget = indexTarget;
    }

    @Override
    public void write(Object value, GenericData.Record record) {
        Object val = record.get(index);
        if (val != null && val instanceof List) {
            List list = (List) val;
            if (list.size() > indexTarget) {
                list.set(indexTarget, value);
            }
        }
    }
}
