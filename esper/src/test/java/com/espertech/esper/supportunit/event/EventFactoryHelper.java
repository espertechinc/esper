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
package com.espertech.esper.supportunit.event;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.supportunit.bean.SupportBeanString;
import junit.framework.TestCase;

import java.util.*;

public class EventFactoryHelper {
    public static EventBean makeEvent(String id) {
        SupportBeanString bean = new SupportBeanString(id);
        return SupportEventBeanFactory.createObject(bean);
    }

    public static EventBean[] makeEvents(String[] ids) {
        EventBean events[] = new EventBean[ids.length];
        for (int i = 0; i < ids.length; i++) {
            events[i] = makeEvent(ids[i]);
        }
        return events;
    }

    public static Map<String, EventBean> makeEventMap(String[] ids) {
        Map<String, EventBean> events = new HashMap<String, EventBean>();
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            EventBean eventBean = makeEvent(id);
            events.put(id, eventBean);
        }
        return events;
    }

    public static List<EventBean> makeEventList(String[] ids) {
        EventBean events[] = makeEvents(ids);
        return Arrays.asList(events);
    }

    public static EventBean[] makeArray(Map<String, EventBean> events, String[] ids) {
        EventBean[] eventArr = new EventBean[ids.length];
        for (int i = 0; i < eventArr.length; i++) {
            eventArr[i] = events.get(ids[i]);
            if (eventArr[i] == null) {
                TestCase.fail();
            }
        }
        return eventArr;
    }

    public static List<EventBean> makeList(Map<String, EventBean> events, String[] ids) {
        List<EventBean> eventList = new LinkedList<EventBean>();
        for (int i = 0; i < ids.length; i++) {
            EventBean bean = events.get(ids[i]);
            if (bean == null) {
                TestCase.fail();
            }
            eventList.add(bean);
        }
        return eventList;
    }
}
