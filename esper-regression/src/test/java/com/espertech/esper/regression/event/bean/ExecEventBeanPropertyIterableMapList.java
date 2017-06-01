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
package com.espertech.esper.regression.event.bean;

import com.espertech.esper.client.ConfigurationEventTypeLegacy;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ExecEventBeanPropertyIterableMapList implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        runAssertionIterable(epService);
    }

    private void runAssertionIterable(EPServiceProvider epService) {
        ConfigurationEventTypeLegacy configField = new ConfigurationEventTypeLegacy();
        configField.setAccessorStyle(ConfigurationEventTypeLegacy.AccessorStyle.PUBLIC);
        epService.getEPAdministrator().getConfiguration().addEventType(MyEventWithField.class.getSimpleName(), MyEventWithField.class.getName(), configField);
        MyEventWithField eventField = new MyEventWithField();
        eventField.otherEventsIterable = Arrays.asList(new OtherEvent("id1"));
        eventField.otherEventsMap = Collections.singletonMap("key", new OtherEvent("id2"));
        eventField.otherEventsList = Arrays.asList(new OtherEvent("id3"));

        ConfigurationEventTypeLegacy configCglib = new ConfigurationEventTypeLegacy();
        epService.getEPAdministrator().getConfiguration().addEventType(MyEventWithMethodWCGLIB.class.getSimpleName(), MyEventWithMethodWCGLIB.class.getName(), configCglib);
        MyEventWithMethodWCGLIB eventMethodCglib = new MyEventWithMethodWCGLIB(Arrays.asList(new OtherEvent("id1")), Collections.singletonMap("key", new OtherEvent("id2")), Arrays.asList(new OtherEvent("id3")));

        ConfigurationEventTypeLegacy configNoCglib = new ConfigurationEventTypeLegacy();
        configNoCglib.setCodeGeneration(ConfigurationEventTypeLegacy.CodeGeneration.DISABLED);
        epService.getEPAdministrator().getConfiguration().addEventType(MyEventWithMethodNoCGLIB.class.getSimpleName(), MyEventWithMethodNoCGLIB.class.getName(), configNoCglib);
        MyEventWithMethodNoCGLIB eventMethodNocglib = new MyEventWithMethodNoCGLIB(Arrays.asList(new OtherEvent("id1")), Collections.singletonMap("key", new OtherEvent("id2")), Arrays.asList(new OtherEvent("id3")));

        tryAssertionIterable(epService, MyEventWithField.class, eventField);
        tryAssertionIterable(epService, MyEventWithMethodWCGLIB.class, eventMethodCglib);
        tryAssertionIterable(epService, MyEventWithMethodNoCGLIB.class, eventMethodNocglib);
    }

    private void tryAssertionIterable(EPServiceProvider epService, Class typeClass, Object event) {
        EPStatement stmt = epService.getEPAdministrator().createEPL("select otherEventsIterable[0] as c0, otherEventsMap('key') as c1, otherEventsList[0] as c2 from " + typeClass.getSimpleName());
        SupportUpdateListener listener = new SupportUpdateListener();
        stmt.addListener(listener);

        epService.getEPRuntime().sendEvent(event);
        EPAssertionUtil.assertProps(listener.assertOneGetNewAndReset(), "c0.id,c1.id,c2.id".split(","), new Object[] {"id1", "id2", "id3"});

        stmt.destroy();
    }

    public static abstract class MyEventWithMethod
    {
        private final Iterable<OtherEvent> otherEventsIterable;
        private final Map<String, OtherEvent> otherEventsMap;
        private final List<OtherEvent> otherEventsList;

        public MyEventWithMethod(Iterable<OtherEvent> otherEventsIterable, Map<String, OtherEvent> otherEventsMap, List<OtherEvent> otherEventsList) {
            this.otherEventsIterable = otherEventsIterable;
            this.otherEventsMap = otherEventsMap;
            this.otherEventsList = otherEventsList;
        }

        public Iterable<OtherEvent> getOtherEventsIterable() {
            return otherEventsIterable;
        }

        public Map<String, OtherEvent> getOtherEventsMap() {
            return otherEventsMap;
        }

        public List<OtherEvent> getOtherEventsList() {
            return otherEventsList;
        }
    }

    public static class MyEventWithMethodWCGLIB extends MyEventWithMethod
    {
        public MyEventWithMethodWCGLIB(Iterable<OtherEvent> otherEventsIterable, Map<String, OtherEvent> otherEventsMap, List<OtherEvent> otherEventsList) {
            super(otherEventsIterable, otherEventsMap, otherEventsList);
        }
    }

    public static class MyEventWithMethodNoCGLIB extends MyEventWithMethod
    {
        public MyEventWithMethodNoCGLIB(Iterable<OtherEvent> otherEventsIterable, Map<String, OtherEvent> otherEventsMap, List<OtherEvent> otherEventsList) {
            super(otherEventsIterable, otherEventsMap, otherEventsList);
        }
    }

    public static class MyEventWithField
    {
        public Iterable<OtherEvent> otherEventsIterable;
        public Map<String, OtherEvent> otherEventsMap;
        public List<OtherEvent> otherEventsList;
    }

    public static class OtherEvent
    {
        private final String id;

        public OtherEvent(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }
}
