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
 * An object which can produce statistical summaries.
 */
public interface Summarizable {
    /**
     * Returns the largest recorded value.
     *
     * @return the largest recorded value
     */
    double max();

    /**
     * Returns the smallest recorded value.
     *
     * @return the smallest recorded value
     */
    double min();

    /**
     * Returns the arithmetic mean of all recorded values.
     *
     * @return the arithmetic mean of all recorded values
     */
    double mean();

    /**
     * Returns the standard deviation of all recorded values.
     *
     * @return the standard deviation of all recorded values
     */
    double stdDev();

    /**
     * Returns the sum of all recorded values.
     *
     * @return the sum of all recorded values
     */
    double sum();

}
