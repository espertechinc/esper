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
package com.espertech.esper.metrics;

import com.espertech.esper.core.service.EPServiceProviderSPI;
import com.espertech.esper.metrics.codahale_metrics.metrics.MetricNameFactory;
import com.espertech.esper.metrics.codahale_metrics.metrics.Metrics;
import com.espertech.esper.metrics.codahale_metrics.metrics.core.*;
import com.espertech.esper.util.EPServiceProviderName;
import junit.framework.TestCase;

import javax.management.ObjectInstance;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TestCodahaleMetricsJMX extends TestCase {

    public void testMetricsJMX() throws Exception {

        String engineURI = EPServiceProviderName.DEFAULT_ENGINE_URI;
        List<MetricName> metricNames = new ArrayList<MetricName>();

        // Exposes a single "value" attribute
        final AtomicInteger count = new AtomicInteger();
        MetricName gaugeName = MetricNameFactory.name(engineURI, "type-testgauge", this.getClass());
        Metrics.newGauge(gaugeName, new Gauge<Integer>() {
            public Integer value() {
                return count.get();
            }
        });
        metricNames.add(gaugeName);

        // Exposes a counter, which is more efficient then the gauge when the size() call is expensive
        MetricName counterName = MetricNameFactory.name(engineURI, "type-testcounter", this.getClass());
        Counter counter = Metrics.newCounter(counterName);
        metricNames.add(gaugeName);

        // exposes a 1-second, 10-second etc. exponential weighted average
        MetricName meterName = MetricNameFactory.name(engineURI, "type-testmeter", this.getClass());
        Meter meter = Metrics.newMeter(meterName, "request", TimeUnit.SECONDS);
        metricNames.add(meterName);

        // exposes a histogramm of avg, min, max, 50th%, 95%, 99%
        MetricName histName = MetricNameFactory.name(engineURI, "type-testhist", this.getClass());
        Histogram hist = Metrics.newHistogram(histName, true);
        metricNames.add(histName);

        // exposes a timer with a rates avg, one minute, 5 minute, 15 minute
        MetricName timerName = MetricNameFactory.name(engineURI, "type-testtimer", this.getClass());
        Timer timer = Metrics.newTimer(timerName, TimeUnit.MILLISECONDS, TimeUnit.MILLISECONDS);
        metricNames.add(timerName);

        // assert names found
        for (MetricName name : metricNames) {
            assertFound(name.getMBeanName());
        }

        // Increase here for a longer run
        final long TESTINTERVAL = 300;

        long start = System.currentTimeMillis();
        final long[] histogrammChoices = new long[]{100, 1000, 5000, 8000, 10000};
        while (System.currentTimeMillis() - start < TESTINTERVAL) {
            TimerContext timerContext = timer.time();
            meter.mark();
            count.incrementAndGet();
            counter.inc();
            hist.update(histogrammChoices[(int) (Math.random() * histogrammChoices.length)]);
            Thread.sleep(100);
            timerContext.stop();
        }

        for (MetricName name : metricNames) {
            Metrics.defaultRegistry().removeMetric(name);
            assertNotFound(name.getMBeanName());
        }
    }

    private void assertFound(String name) throws Exception {
        ObjectInstance instance = ManagementFactory.getPlatformMBeanServer().getObjectInstance(new ObjectName(name));
        assertNotNull(instance);
    }

    private void assertNotFound(String name) throws Exception {
        try {
            ManagementFactory.getPlatformMBeanServer().getObjectInstance(new ObjectName(name));
            fail();
        } catch (javax.management.InstanceNotFoundException ex) {
            // expected
        }
    }
}
