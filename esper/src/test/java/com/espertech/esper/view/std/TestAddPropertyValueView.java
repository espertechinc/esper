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
package com.espertech.esper.view.std;

import com.espertech.esper.collection.MultiKeyUntyped;
import junit.framework.TestCase;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.core.support.SupportEventAdapterService;
import com.espertech.esper.supportunit.event.SupportEventBeanFactory;
import com.espertech.esper.supportunit.event.SupportEventTypeFactory;
import com.espertech.esper.supportunit.view.SupportMapView;
import com.espertech.esper.supportunit.view.SupportSchemaNeutralView;
import com.espertech.esper.core.support.SupportStatementContextFactory;

import java.util.HashMap;
import java.util.Map;

public class TestAddPropertyValueView extends TestCase
{
    private AddPropertyValueOptionalView myView;
    private SupportMapView parentView;
    private SupportSchemaNeutralView childView;
    private EventType parentEventType;

    public void setUp()
    {
        Map<String, Object> schema = new HashMap<String, Object>();
        schema.put("STDDEV", Double.class);
        parentEventType = SupportEventTypeFactory.createMapType(schema);

        Map<String, Object> addProps = new HashMap<String, Object>();
        addProps.put("symbol", String.class);
        EventType mergeEventType = SupportEventAdapterService.getService().createAnonymousWrapperType("test", parentEventType, addProps);

        // Set up length window view and a test child view
        myView = new AddPropertyValueOptionalView(SupportStatementContextFactory.makeAgentInstanceViewFactoryContext(), new String[] {"symbol"}, "IBM", mergeEventType);

        parentView = new SupportMapView(schema);
        parentView.addView(myView);

        childView = new SupportSchemaNeutralView();
        myView.addView(childView);
    }

    public void testViewUpdate()
    {
        Map<String, Object> eventData = new HashMap<String, Object>();

        // Generate some events
        eventData.put("STDDEV", 100);
        EventBean eventBeanOne = SupportEventBeanFactory.createMapFromValues(new HashMap<String, Object>(eventData), parentEventType);
        eventData.put("STDDEV", 0);
        EventBean eventBeanTwo = SupportEventBeanFactory.createMapFromValues(new HashMap<String, Object>(eventData), parentEventType);
        eventData.put("STDDEV", 99999);
        EventBean eventBeanThree = SupportEventBeanFactory.createMapFromValues(new HashMap<String, Object>(eventData), parentEventType);

        // Send events
        parentView.update(new EventBean[] { eventBeanOne, eventBeanTwo},
                          new EventBean[] { eventBeanThree });

        // Checks
        EventBean[] newData = childView.getLastNewData();
        assertEquals(2, newData.length);
        assertEquals("IBM", newData[0].get("symbol"));
        assertEquals(100, newData[0].get("STDDEV"));
        assertEquals("IBM", newData[1].get("symbol"));
        assertEquals(0, newData[1].get("STDDEV"));

        EventBean[] oldData = childView.getLastOldData();
        assertEquals(1, oldData.length);
        assertEquals("IBM", oldData[0].get("symbol"));
        assertEquals(99999, oldData[0].get("STDDEV"));
    }

    public void testCopyView() throws Exception
    {
        AddPropertyValueOptionalView copied = (AddPropertyValueOptionalView) myView.cloneView();
        assertEquals(myView.getPropertyNames(), copied.getPropertyNames());
        assertEquals(myView.getPropertyValues(), copied.getPropertyValues());
    }

    public void testAddProperty()
    {
        Map<String, Object> eventData = new HashMap<String, Object>();
        eventData.put("STDDEV", 100);
        EventBean eventBean = SupportEventBeanFactory.createMapFromValues(eventData, parentEventType);

        Map<String, Object> addProps = new HashMap<String, Object>();
        addProps.put("test", Integer.class);
        EventType newEventType = SupportEventAdapterService.getService().createAnonymousWrapperType("test", parentEventType, addProps);
        EventBean newBean = AddPropertyValueOptionalView.addProperty(eventBean, new String[] {"test"}, new MultiKeyUntyped(new Object[] {2}), newEventType, SupportEventAdapterService.getService());

        assertEquals(2, newBean.get("test"));
        assertEquals(100, newBean.get("STDDEV"));
    }
}
