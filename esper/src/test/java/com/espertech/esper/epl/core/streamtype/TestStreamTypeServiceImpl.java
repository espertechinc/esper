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
package com.espertech.esper.epl.core.streamtype;

import com.espertech.esper.client.EventType;
import com.espertech.esper.collection.Pair;
import com.espertech.esper.core.support.SupportEventAdapterService;
import com.espertech.esper.epl.core.streamtype.*;
import com.espertech.esper.supportunit.bean.SupportBean;
import com.espertech.esper.supportunit.bean.SupportBean_A;
import com.espertech.esper.supportunit.bean.SupportMarketDataBean;
import com.espertech.esper.supportunit.event.SupportEventTypeFactory;
import junit.framework.TestCase;

import java.util.LinkedHashMap;

public class TestStreamTypeServiceImpl extends TestCase {
    private StreamTypeServiceImpl serviceRegular;
    private StreamTypeServiceImpl serviceStreamZeroUnambigous;
    private StreamTypeServiceImpl serviceRequireStreamName;

    public void setUp() {
        SupportEventAdapterService.reset();

        // Prepare regualar test service
        EventType[] eventTypes = new EventType[]{
                SupportEventTypeFactory.createBeanType(SupportBean.class),
                SupportEventTypeFactory.createBeanType(SupportBean.class),
                SupportEventTypeFactory.createBeanType(SupportBean_A.class),
                SupportEventTypeFactory.createBeanType(SupportMarketDataBean.class, "SupportMarketDataBean")
        };
        String[] eventTypeName = new String[]{"SupportBean", "SupportBean", "SupportBean_A", "SupportMarketDataBean"};
        String[] streamNames = new String[]{"s1", null, "s3", "s4"};
        serviceRegular = new StreamTypeServiceImpl(eventTypes, streamNames, new boolean[10], "default", false, false);

        // Prepare with stream-zero being unambigous
        LinkedHashMap<String, Pair<EventType, String>> streamTypes = new LinkedHashMap<String, Pair<EventType, String>>();
        for (int i = 0; i < streamNames.length; i++) {
            streamTypes.put(streamNames[i], new Pair<EventType, String>(eventTypes[i], eventTypeName[i]));
        }
        serviceStreamZeroUnambigous = new StreamTypeServiceImpl(streamTypes, "default", true, false);

        // Prepare requiring stream names for non-zero streams
        serviceRequireStreamName = new StreamTypeServiceImpl(streamTypes, "default", true, true);
    }

    public void testResolveByStreamAndPropNameInOne() throws Exception {
        tryResolveByStreamAndPropNameInOne(serviceRegular);
        tryResolveByStreamAndPropNameInOne(serviceStreamZeroUnambigous);
        tryResolveByStreamAndPropNameInOne(serviceRequireStreamName);
    }

    public void testResolveByPropertyName() throws Exception {
        tryResolveByPropertyName(serviceRegular);
        serviceStreamZeroUnambigous.resolveByPropertyName("boolPrimitive", false);
        serviceRequireStreamName.resolveByPropertyName("boolPrimitive", false);

        try {
            serviceRequireStreamName.resolveByPropertyName("volume", false);
            fail();
        } catch (PropertyNotFoundException ex) {
            // expected
        }
    }

    public void testResolveByStreamAndPropNameBoth() throws Exception {
        tryResolveByStreamAndPropNameBoth(serviceRegular);
        tryResolveByStreamAndPropNameBoth(serviceStreamZeroUnambigous);
        tryResolveByStreamAndPropNameBoth(serviceRequireStreamName);
    }

    private static void tryResolveByStreamAndPropNameBoth(StreamTypeService service) throws Exception {
        // Test lookup by stream name and prop name
        PropertyResolutionDescriptor desc = service.resolveByStreamAndPropName("s4", "volume", false);
        assertEquals(3, (int) desc.getStreamNum());
        assertEquals(Long.class, desc.getPropertyType());
        assertEquals("volume", desc.getPropertyName());
        assertEquals("s4", desc.getStreamName());
        assertEquals(SupportMarketDataBean.class, desc.getStreamEventType().getUnderlyingType());

        try {
            service.resolveByStreamAndPropName("xxx", "volume", false);
            fail();
        } catch (StreamNotFoundException ex) {
            // Expected
        }

        try {
            service.resolveByStreamAndPropName("s4", "xxxx", false);
            fail();
        } catch (PropertyNotFoundException ex) {
            // Expected
        }
    }

    private static void tryResolveByPropertyName(StreamTypeService service) throws Exception {
        // Test lookup by property name only
        PropertyResolutionDescriptor desc = service.resolveByPropertyName("volume", false);
        assertEquals(3, (int) (desc.getStreamNum()));
        assertEquals(Long.class, desc.getPropertyType());
        assertEquals("volume", desc.getPropertyName());
        assertEquals("s4", desc.getStreamName());
        assertEquals(SupportMarketDataBean.class, desc.getStreamEventType().getUnderlyingType());

        try {
            service.resolveByPropertyName("boolPrimitive", false);
            fail();
        } catch (DuplicatePropertyException ex) {
            // Expected
        }

        try {
            service.resolveByPropertyName("xxxx", false);
            fail();
        } catch (PropertyNotFoundException ex) {
            // Expected
        }
    }

    private static void tryResolveByStreamAndPropNameInOne(StreamTypeService service) throws Exception {
        // Test lookup by stream name and prop name
        PropertyResolutionDescriptor desc = service.resolveByStreamAndPropName("s4.volume", false);
        assertEquals(3, (int) desc.getStreamNum());
        assertEquals(Long.class, desc.getPropertyType());
        assertEquals("volume", desc.getPropertyName());
        assertEquals("s4", desc.getStreamName());
        assertEquals(SupportMarketDataBean.class, desc.getStreamEventType().getUnderlyingType());

        try {
            service.resolveByStreamAndPropName("xxx.volume", false);
            fail();
        } catch (PropertyNotFoundException ex) {
            // Expected
        }

        try {
            service.resolveByStreamAndPropName("s4.xxxx", false);
            fail();
        } catch (PropertyNotFoundException ex) {
            // Expected
        }

        // resolve by event type alias (table name)
        desc = service.resolveByStreamAndPropName("SupportMarketDataBean.volume", false);
        assertEquals(3, (int) desc.getStreamNum());

        // resolve by engine URI plus event type alias
        desc = service.resolveByStreamAndPropName("default.SupportMarketDataBean.volume", false);
        assertEquals(3, (int) desc.getStreamNum());
    }
}
