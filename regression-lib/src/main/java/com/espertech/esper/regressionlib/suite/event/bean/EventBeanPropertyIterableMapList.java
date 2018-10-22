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
package com.espertech.esper.regressionlib.suite.event.bean;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class EventBeanPropertyIterableMapList implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        runAssertionIterable(env);
    }

    private void runAssertionIterable(RegressionEnvironment env) {
        MyEventWithField eventField = new MyEventWithField();
        eventField.otherEventsIterable = Arrays.asList(new OtherEvent("id1"));
        eventField.otherEventsMap = Collections.singletonMap("key", new OtherEvent("id2"));
        eventField.otherEventsList = Arrays.asList(new OtherEvent("id3"));

        MyEventWithMethod eventMethod = new MyEventWithMethod(Arrays.asList(new OtherEvent("id1")), Collections.singletonMap("key", new OtherEvent("id2")), Arrays.asList(new OtherEvent("id3")));

        tryAssertionIterable(env, MyEventWithField.class, eventField);
        tryAssertionIterable(env, MyEventWithMethod.class, eventMethod);
    }

    private void tryAssertionIterable(RegressionEnvironment env, Class typeClass, Object event) {
        env.compileDeploy("@name('s0') select otherEventsIterable[0] as c0, otherEventsMap('key') as c1, otherEventsList[0] as c2 from " + typeClass.getSimpleName());
        env.addListener("s0");

        env.sendEventBean(event);
        EPAssertionUtil.assertProps(env.listener("s0").assertOneGetNewAndReset(), "c0.id,c1.id,c2.id".split(","), new Object[]{"id1", "id2", "id3"});

        env.undeployAll();
    }

    public static class MyEventWithMethod implements Serializable {
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

    public static class MyEventWithField implements Serializable {
        public Iterable<OtherEvent> otherEventsIterable;
        public Map<String, OtherEvent> otherEventsMap;
        public List<OtherEvent> otherEventsList;
    }

    public static class OtherEvent {
        private final String id;

        public OtherEvent(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }
}
