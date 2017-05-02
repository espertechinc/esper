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
package com.espertech.esper.event;

import com.espertech.esper.client.EventType;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.core.support.SupportEngineImportServiceFactory;
import com.espertech.esper.event.bean.BeanEventType;
import com.espertech.esper.event.map.MapEventType;
import com.espertech.esper.supportunit.bean.SupportBean_S0;
import com.espertech.esper.supportunit.bean.SupportBean_S1;
import com.espertech.esper.supportunit.bean.SupportMarketDataBean;
import junit.framework.TestCase;

import java.util.*;
import java.util.concurrent.*;

/**
 * Test for multithread-safety for manageing statements, i.e. creating and stopping statements
 */
public class TestEventAdapterSvcMT extends TestCase {
    private EventAdapterService service;

    public void setUp() {
        service = new EventAdapterServiceImpl(new EventTypeIdGeneratorImpl(), 5, null, SupportEngineImportServiceFactory.make());
    }

    public void testAddBeanTypeClass() throws Exception {
        final Collection<EventType> types = Collections.synchronizedCollection(new HashSet<EventType>());

        Callable callables[] = new Callable[2];
        for (int i = 0; i < callables.length; i++) {
            callables[i] = new Callable() {
                public Object call() throws Exception {
                    EventType type = service.addBeanType("a", SupportMarketDataBean.class, true, true, true);
                    types.add(type);

                    type = service.addBeanType("b", SupportMarketDataBean.class, true, true, true);
                    types.add(type);
                    return true;
                }
            };
        }

        Object[] result = tryMT(callables);
        EPAssertionUtil.assertAllBooleanTrue(result);
        assertEquals(1, types.size());
    }

    public void testAddMapType() throws Exception {
        final Map<String, Object> typeOne = new HashMap<String, Object>();
        typeOne.put("f1", Integer.class);
        final Map<String, Object> typeTwo = new HashMap<String, Object>();
        typeTwo.put("f2", Integer.class);

        Callable callables[] = new Callable[2];
        for (int i = 0; i < callables.length; i++) {
            final int index = i;
            callables[i] = new Callable() {
                public Object call() throws Exception {
                    try {
                        if (index == 0) {
                            return service.addNestableMapType("A", typeOne, null, true, true, true, false, false);
                        } else {
                            return service.addNestableMapType("A", typeTwo, null, true, true, true, false, false);
                        }
                    } catch (EventAdapterException ex) {
                        return ex;
                    }
                }
            };
        }

        // the result should be one exception and one type
        Object[] results = tryMT(callables);
        EPAssertionUtil.assertTypeEqualsAnyOrder(new Class[]{EventAdapterException.class, MapEventType.class}, results);
    }

    public void testAddBeanType() throws Exception {
        final Map<String, Class> typeOne = new HashMap<String, Class>();
        typeOne.put("f1", Integer.class);

        Callable callables[] = new Callable[2];
        for (int i = 0; i < callables.length; i++) {
            final int index = i;
            callables[i] = new Callable() {
                public Object call() throws Exception {
                    try {
                        if (index == 0) {
                            return service.addBeanType("X", SupportBean_S1.class, true, true, true);
                        } else {
                            return service.addBeanType("X", SupportBean_S0.class, true, true, true);
                        }
                    } catch (EventAdapterException ex) {
                        return ex;
                    }
                }
            };
        }

        // the result should be one exception and one type
        Object[] results = tryMT(callables);
        EPAssertionUtil.assertTypeEqualsAnyOrder(new Class[]{EventAdapterException.class, BeanEventType.class}, results);
    }

    private Object[] tryMT(Callable[] callables) throws Exception {
        ExecutorService threadPool = Executors.newFixedThreadPool(callables.length);

        Future futures[] = new Future[callables.length];
        for (int i = 0; i < callables.length; i++) {
            futures[i] = threadPool.submit(callables[i]);
        }

        threadPool.shutdown();
        threadPool.awaitTermination(10, TimeUnit.SECONDS);

        Object[] results = new Object[futures.length];
        for (int i = 0; i < futures.length; i++) {
            results[i] = futures[i].get();
        }

        return results;
    }

    private static interface CallableFactory {
        public Callable makeCallable(int threadNum);
    }
}
