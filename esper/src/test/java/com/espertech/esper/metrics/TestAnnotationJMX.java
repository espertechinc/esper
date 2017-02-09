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

import com.espertech.esper.metrics.codahale_metrics.metrics.MetricNameFactory;
import com.espertech.esper.metrics.codahale_metrics.metrics.core.MetricName;
import com.espertech.esper.metrics.jmx.*;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestAnnotationJMX extends TestCase {

    private static final Logger log = LoggerFactory.getLogger(TestAnnotationJMX.class);

    public void testAnnotations() throws Exception {
        MetricName metricName = MetricNameFactory.name("default", "test", TestAnnotationJMX.class);
        CommonJMXUtil.registerMbean(new MyJMXExposedClass(), metricName);
        CommonJMXUtil.unregisterMbean(metricName);
    }

    @JmxManaged
    public static class MyJMXExposedClass {

        private String value = "initial value";

        @JmxSetter(name = "propertyToSet", description = "Set some value")
        public void setValue(@JmxParam(name = "value to set", description = "description for the value to set") String value) {
            log.info("Setting value " + value);
            this.value = value;
        }

        @JmxOperation(description = "Perform an operation")
        public void doSomething() {
            log.info("Invoking operation");
        }

        @JmxGetter(name = "propertyToGet", description = "Get some value")
        public String getValue() {
            log.info("Getting value " + value);
            return value;
        }

        @JmxGetter(name = "secondPropertyToGet", description = "Get a second value that is the same as the first value")
        public String getValueTwo() {
            log.info("Getting value two " + value);
            return value;
        }
    }
}
