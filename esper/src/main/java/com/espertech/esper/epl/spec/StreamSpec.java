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
 * Specification for a stream, consists simply of an optional stream name and a list of views
 * on to of the stream.
 * <p>
 * Implementation classes for views and patterns add additional information defining the
 * stream of events.
 */
public interface StreamSpec extends Serializable {
    /**
     * Returns the stream name, or null if undefined.
     *
     * @return stream name
     */
    public String getOptionalStreamName();

    /**
     * Returns views definitions onto the stream
     *
     * @return view defs
     */
    public ViewSpec[] getViewSpecs();

    /**
     * Returns the options for the stream such as unidirectional, retain-union etc.
     *
     * @return stream options
     */
    public StreamSpecOptions getOptions();
}
