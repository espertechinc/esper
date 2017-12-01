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
package com.espertech.esper.epl.spec;

import java.io.Serializable;

/**
 * Abstract base specification for a stream, consists simply of an optional stream name and a list of views
 * on to of the stream.
 * <p>
 * Implementation classes for views and patterns add additional information defining the
 * stream of events.
 */
public class StreamSpecOptions implements Serializable {
    private static final long serialVersionUID = 0L;

    private final boolean isUnidirectional;
    private final boolean isRetainUnion;
    private final boolean isRetainIntersection;

    public final static StreamSpecOptions DEFAULT = new StreamSpecOptions();

    /**
     * Ctor, sets all options off.
     */
    private StreamSpecOptions() {
        isUnidirectional = false;
        isRetainUnion = false;
        isRetainIntersection = false;
    }

    /**
     * Ctor.
     *
     * @param isUnidirectional     - true to indicate a unidirectional stream in a join, applicable for joins
     * @param isRetainUnion        - for retaining the union of multiple data windows
     * @param isRetainIntersection - for retaining the intersection of multiple data windows
     */
    public StreamSpecOptions(boolean isUnidirectional, boolean isRetainUnion, boolean isRetainIntersection) {
        if (isRetainUnion && isRetainIntersection) {
            throw new IllegalArgumentException("Invalid retain flags");
        }
        this.isUnidirectional = isUnidirectional;
        this.isRetainUnion = isRetainUnion;
        this.isRetainIntersection = isRetainIntersection;
    }

    /**
     * Indicator for retaining the union of multiple expiry policies.
     *
     * @return true for retain union
     */
    public boolean isRetainUnion() {
        return isRetainUnion;
    }

    /**
     * Indicator for retaining the intersection of multiple expiry policies.
     *
     * @return true for retain intersection
     */
    public boolean isRetainIntersection() {
        return isRetainIntersection;
    }

    /**
     * Returns true to indicate a unidirectional stream in a join, applicable for joins.
     *
     * @return indicator whether the stream is unidirectional in a join
     */
    public boolean isUnidirectional() {
        return isUnidirectional;
    }
}
