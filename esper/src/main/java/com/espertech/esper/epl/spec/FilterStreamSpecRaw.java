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
 * Unvalided filter-based stream specification.
 */
public class FilterStreamSpecRaw extends StreamSpecBase implements StreamSpecRaw, Serializable {
    private FilterSpecRaw rawFilterSpec;
    private static final long serialVersionUID = -7919060568262701953L;


    /**
     * Ctor.
     *
     * @param rawFilterSpec      is unvalidated filter specification
     * @param viewSpecs          is the view definition
     * @param optionalStreamName is the stream name if supplied, or null if not supplied
     * @param streamSpecOptions  - additional options, such as unidirectional stream in a join
     */
    public FilterStreamSpecRaw(FilterSpecRaw rawFilterSpec, ViewSpec[] viewSpecs, String optionalStreamName, StreamSpecOptions streamSpecOptions) {
        super(optionalStreamName, viewSpecs, streamSpecOptions);
        this.rawFilterSpec = rawFilterSpec;
    }

    /**
     * Default ctor.
     */
    public FilterStreamSpecRaw() {
    }

    /**
     * Returns the unvalided filter spec.
     *
     * @return filter def
     */
    public FilterSpecRaw getRawFilterSpec() {
        return rawFilterSpec;
    }
}
