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
public abstract class StreamSpecBase implements Serializable {
    private static final long serialVersionUID = 0L;

    private String optionalStreamName;
    private ViewSpec[] viewSpecs;
    private StreamSpecOptions streamSpecOptions;

    /**
     * Ctor.
     *
     * @param optionalStreamName - stream name, or null if none supplied
     * @param viewSpecs          - specifies what view to use to derive data
     * @param streamSpecOptions  - indicates additional options such as unidirectional stream or retain-union or retain-intersection
     */
    public StreamSpecBase(String optionalStreamName, ViewSpec[] viewSpecs, StreamSpecOptions streamSpecOptions) {
        this.optionalStreamName = optionalStreamName;
        this.viewSpecs = viewSpecs;
        this.streamSpecOptions = streamSpecOptions;
    }

    /**
     * Default ctor.
     */
    public StreamSpecBase() {
        viewSpecs = ViewSpec.EMPTY_VIEWSPEC_ARRAY;
    }

    /**
     * Returns the name assigned.
     *
     * @return stream name or null if not assigned
     */
    public String getOptionalStreamName() {
        return optionalStreamName;
    }

    /**
     * Returns view definitions to use to construct views to derive data on stream.
     *
     * @return view defs
     */
    public ViewSpec[] getViewSpecs() {
        return viewSpecs;
    }

    /**
     * Returns the options for the stream such as unidirectional, retain-union etc.
     *
     * @return stream options
     */
    public StreamSpecOptions getOptions() {
        return streamSpecOptions;
    }
}
