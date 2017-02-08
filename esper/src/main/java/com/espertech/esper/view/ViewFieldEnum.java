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
package com.espertech.esper.view;

/**
 * Enumerates the valid values for each view's public fields. The name of the field or property can be used
 * to obtain values from the view rather than using the hardcoded String value for the field.
 */
public enum ViewFieldEnum {
    /**
     * Count.
     */
    UNIVARIATE_STATISTICS__DATAPOINTS("datapoints"),

    /**
     * Sum.
     */
    UNIVARIATE_STATISTICS__TOTAL("total"),

    /**
     * Average.
     */
    UNIVARIATE_STATISTICS__AVERAGE("average"),

    /**
     * Standard dev population.
     */
    UNIVARIATE_STATISTICS__STDDEVPA("stddevpa"),

    /**
     * Standard dev.
     */
    UNIVARIATE_STATISTICS__STDDEV("stddev"),

    /**
     * Variance.
     */
    UNIVARIATE_STATISTICS__VARIANCE("variance"),

    /**
     * Weighted average.
     */
    WEIGHTED_AVERAGE__AVERAGE("average"),

    /**
     * Correlation.
     */
    CORRELATION__CORRELATION("correlation"),

    /**
     * Slope.
     */
    REGRESSION__SLOPE("slope"),

    /**
     * Y-intercept.
     */
    REGRESSION__YINTERCEPT("YIntercept"),

    /**
     * XAverage
     */
    REGRESSION__XAVERAGE("XAverage"),

    /**
     * XStandardDeviationPop
     */
    REGRESSION__XSTANDARDDEVIATIONPOP("XStandardDeviationPop"),

    /**
     * XStandardDeviationSample
     */
    REGRESSION__XSTANDARDDEVIATIONSAMPLE("XStandardDeviationSample"),

    /**
     * XSum
     */
    REGRESSION__XSUM("XSum"),

    /**
     * XVariance
     */
    REGRESSION__XVARIANCE("XVariance"),

    /**
     * YAverage
     */
    REGRESSION__YAVERAGE("YAverage"),

    /**
     * YStandardDeviationPop
     */
    REGRESSION__YSTANDARDDEVIATIONPOP("YStandardDeviationPop"),

    /**
     * YStandardDeviationSample
     */
    REGRESSION__YSTANDARDDEVIATIONSAMPLE("YStandardDeviationSample"),

    /**
     * YSum
     */
    REGRESSION__YSUM("YSum"),

    /**
     * YVariance
     */
    REGRESSION__YVARIANCE("YVariance"),

    /**
     * dataPoints
     */
    REGRESSION__DATAPOINTS("dataPoints"),

    /**
     * n
     */
    REGRESSION__N("n"),

    /**
     * sumX
     */
    REGRESSION__SUMX("sumX"),

    /**
     * sumXSq
     */
    REGRESSION__SUMXSQ("sumXSq"),

    /**
     * sumXY
     */
    REGRESSION__SUMXY("sumXY"),

    /**
     * sumY
     */
    REGRESSION__SUMY("sumY"),

    /**
     * sumYSq
     */
    REGRESSION__SUMYSQ("sumYSq"),

    /**
     * Size.
     */
    SIZE_VIEW__SIZE("size");

    private final String name;

    ViewFieldEnum(String name) {
        this.name = name;
    }

    /**
     * Returns the field name of fields that contain data within a view's posted objects.
     *
     * @return field name for use with DataSchema to obtain values out of objects.
     */
    public String getName() {
        return name;
    }
}
