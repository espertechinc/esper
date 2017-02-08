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

import static java.lang.Double.isInfinite;
import static java.lang.Double.isNaN;

/**
 * A gauge which measures the ratio of one value to another.
 * If the denominator is zero, not a number, or infinite, the resulting ratio is not a number.
 */
public abstract class RatioGauge extends Gauge<Double> {
    /**
     * Returns the numerator (the value on the top half of the fraction or the left-hand side of the
     * ratio).
     *
     * @return the numerator
     */
    protected abstract double getNumerator();

    /**
     * Returns the denominator (the value on the bottom half of the fraction or the right-hand side
     * of the ratio).
     *
     * @return the denominator
     */
    protected abstract double getDenominator();

    @Override
    public Double value() {
        final double d = getDenominator();
        if (isNaN(d) || isInfinite(d) || d == 0.0) {
            return Double.NaN;
        }
        return getNumerator() / d;
    }
}
