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

import java.util.concurrent.TimeUnit;

/**
 * An object which maintains mean and exponentially-weighted rate.
 */
public interface Metered extends Metric {
    /**
     * Returns the meter's rate unit.
     *
     * @return the meter's rate unit
     */
    TimeUnit rateUnit();

    /**
     * Returns the type of events the meter is measuring.
     *
     * @return the meter's event type
     */
    String eventType();

    /**
     * Returns the number of events which have been marked.
     *
     * @return the number of events which have been marked
     */
    long count();

    /**
     * Returns the fifteen-minute exponentially-weighted moving average rate at which events have
     * occurred since the meter was created.
     * This rate has the same exponential decay factor as the fifteen-minute load average in the
     * {@code top} Unix command.
     *
     * @return the fifteen-minute exponentially-weighted moving average rate at which events have
     * occurred since the meter was created
     */
    double fifteenMinuteRate();

    /**
     * Returns the five-minute exponentially-weighted moving average rate at which events have
     * occurred since the meter was created.
     * This rate has the same exponential decay factor as the five-minute load average in the {@code
     * top} Unix command.
     *
     * @return the five-minute exponentially-weighted moving average rate at which events have
     * occurred since the meter was created
     */
    double fiveMinuteRate();

    /**
     * Returns the mean rate at which events have occurred since the meter was created.
     *
     * @return the mean rate at which events have occurred since the meter was created
     */
    double meanRate();

    /**
     * Returns the one-minute exponentially-weighted moving average rate at which events have
     * occurred since the meter was created.
     * This rate has the same exponential decay factor as the one-minute load average in the {@code
     * top} Unix command.
     *
     * @return the one-minute exponentially-weighted moving average rate at which events have
     * occurred since the meter was created
     */
    double oneMinuteRate();
}
