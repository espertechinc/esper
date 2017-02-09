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
package com.espertech.esper.avro.core;

import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import junit.framework.TestCase;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.espertech.esper.avro.util.support.SupportAvroUtil.makeAvroSupportEventType;
import static org.apache.avro.SchemaBuilder.record;

public class TestAvroEventBean extends TestCase {
    public void testGet() {
        Schema schema = record("typename").fields()
                .requiredInt("myInt")
                .endRecord();

        EventType eventType = makeAvroSupportEventType(schema);

        GenericData.Record record = new GenericData.Record(schema);
        record.put("myInt", 99);
        AvroGenericDataEventBean eventBean = new AvroGenericDataEventBean(record, eventType);

        assertEquals(eventType, eventBean.getEventType());
        assertEquals(record, eventBean.getUnderlying());
        assertEquals(99, eventBean.get("myInt"));

        // test wrong property name
        try {
            eventBean.get("dummy");
            fail();
        } catch (PropertyAccessException ex) {
            // Expected
            log.debug(".testGetter Expected exception, msg=" + ex.getMessage());
        }
    }

    private static final Logger log = LoggerFactory.getLogger(TestAvroEventBean.class);
}
