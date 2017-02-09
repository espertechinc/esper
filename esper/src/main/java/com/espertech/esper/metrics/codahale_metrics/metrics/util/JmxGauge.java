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
/***************************************************************************************
 * Attribution Notice
 *
 * This file is imported from Metrics (https://github.com/codahale/metrics subproject metrics-core).
 * Metrics is Copyright (c) 2010-2012 Coda Hale, Yammer.com
 * Metrics is Published under Apache Software License 2.0, see LICENSE in root folder.
 *
 * Thank you for the Metrics developers efforts in making their library available under an Apache license.
 * EsperTech incorporates Metrics version 0.2.2 in source code form since Metrics depends on SLF4J
 * and this dependency is not possible to introduce for Esper.
 * *************************************************************************************
 */

package com.espertech.esper.metrics.codahale_metrics.metrics.util;

import com.espertech.esper.metrics.codahale_metrics.metrics.core.Gauge;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

/**
 * A gauge which exposes an attribute of a JMX MBean.
 */
public class JmxGauge extends Gauge<Object> {
    private static final MBeanServer SERVER = ManagementFactory.getPlatformMBeanServer();
    private final ObjectName objectName;
    private final String attribute;

    /**
     * Creates a new {@link JmxGauge} for the given attribute of the given MBean.
     *
     * @param objectName the string value of the MBean's {@link javax.management.ObjectName}
     * @param attribute  the MBean attribute's name
     * @throws javax.management.MalformedObjectNameException if {@code objectName} is malformed
     */
    public JmxGauge(String objectName, String attribute) throws MalformedObjectNameException {
        this(new ObjectName(objectName), attribute);
    }

    /**
     * Creates a new {@link JmxGauge} for the given attribute of the given MBean.
     *
     * @param objectName the MBean's {@link javax.management.ObjectName}
     * @param attribute  the MBean attribute's name
     */
    public JmxGauge(ObjectName objectName, String attribute) {
        this.objectName = objectName;
        this.attribute = attribute;
    }

    @Override
    public Object value() {
        try {
            return SERVER.getAttribute(objectName, attribute);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
