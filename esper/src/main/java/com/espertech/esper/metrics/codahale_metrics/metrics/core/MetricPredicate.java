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
package com.espertech.esper.metrics.codahale_metrics.metrics.core;

/**
 * A {@link MetricPredicate} is used to determine whether a metric should be included when sorting
 * and filtering metrics. This is especially useful for limited metric reporting.
 */
public interface MetricPredicate {
    /**
     * A predicate which matches all inputs.
     */
    MetricPredicate ALL = new MetricPredicate() {
        @Override
        public boolean matches(MetricName name, Metric metric) {
            return true;
        }
    };

    /**
     * Returns {@code true} if the metric matches the predicate.
     *
     * @param name   the name of the metric
     * @param metric the metric itself
     * @return {@code true} if the predicate applies, {@code false} otherwise
     */
    boolean matches(MetricName name, Metric metric);
}
