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
package com.espertech.esper.common.internal.compile.stage2;

import com.espertech.esper.common.internal.compile.stage1.spec.StreamSpecBase;
import com.espertech.esper.common.internal.compile.stage1.spec.StreamSpecCompiled;
import com.espertech.esper.common.internal.compile.stage1.spec.StreamSpecOptions;
import com.espertech.esper.common.internal.compile.stage1.spec.ViewSpec;

/**
 * Specification for building an event stream out of a filter for events (supplying type and basic filter criteria)
 * and views onto these events which are staggered onto each other to supply a final stream of events.
 */
public class FilterStreamSpecCompiled extends StreamSpecBase implements StreamSpecCompiled {
    private FilterSpecCompiled filterSpec;

    /**
     * Ctor.
     *
     * @param filterSpec         - specifies what events we are interested in.
     * @param viewSpecs          - specifies what view to use to derive data
     * @param optionalStreamName - stream name, or null if none supplied
     * @param streamSpecOptions  - additional options such as unidirectional stream in a join
     */
    public FilterStreamSpecCompiled(FilterSpecCompiled filterSpec, ViewSpec[] viewSpecs, String optionalStreamName, StreamSpecOptions streamSpecOptions) {
        super(optionalStreamName, viewSpecs, streamSpecOptions);
        this.filterSpec = filterSpec;
    }

    /**
     * Returns filter specification for which events the stream will getSelectListEvents.
     *
     * @return filter spec
     */
    public FilterSpecCompiled getFilterSpecCompiled() {
        return filterSpec;
    }

    /**
     * Sets a filter specification.
     *
     * @param filterSpec to set
     */
    public void setFilterSpec(FilterSpecCompiled filterSpec) {
        this.filterSpec = filterSpec;
    }
}
