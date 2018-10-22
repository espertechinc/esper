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
package com.espertech.esper.runtime.internal.filtersvcimpl;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.filterspec.FilterOperator;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;
import com.espertech.esper.common.internal.filterspec.FilterValueSetParam;
import com.espertech.esper.common.internal.filtersvc.FilterService;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.runtime.internal.support.SupportEventTypeFactory;
import junit.framework.TestCase;

import java.util.concurrent.*;

/**
 * Test for multithread-safety for manageing statements, i.e. creating and stopping statements
 */
public class TestFilterServiceMT extends TestCase {
    public void testFilterService() throws Exception {
        runAssertionAddRemoveFilter(new FilterServiceLockCoarse(false));
        runAssertionAddRemoveFilter(new FilterServiceLockFine(false));
    }

    private void runAssertionAddRemoveFilter(final FilterService service) throws Exception {
        EventType eventType = SupportEventTypeFactory.createBeanType(SupportBean.class);
        FilterSpecActivatable spec = SupportFilterSpecBuilder.build(eventType, new Object[]{"string", FilterOperator.EQUAL, "HELLO"});
        final FilterValueSetParam[][] filterValues = spec.getValueSet(null, null, null, null);

        Callable callables[] = new Callable[5];
        for (int i = 0; i < callables.length; i++) {
            callables[i] = new Callable() {
                public Object call() throws Exception {
                    SupportFilterHandle handle = new SupportFilterHandle();
                    for (int i = 0; i < 10000; i++) {
                        service.add(eventType, filterValues, handle);
                        service.remove(handle, eventType, filterValues);
                    }
                    return true;
                }
            };
        }

        Object[] result = tryMT(callables);
        EPAssertionUtil.assertAllBooleanTrue(result);
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
}
