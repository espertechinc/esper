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
package com.espertech.esper.core.start;

import com.espertech.esper.view.Viewable;

/**
 * Result holder returned by @{link EPStatementStartMethod}.
 */
public class EPStatementStartResult {
    private final Viewable viewable;
    private final EPStatementStopMethod stopMethod;
    private final EPStatementDestroyMethod destroyMethod;

    /**
     * Ctor.
     *
     * @param viewable   last view to attach listeners to
     * @param stopMethod method to stop
     */
    public EPStatementStartResult(Viewable viewable, EPStatementStopMethod stopMethod) {
        this.viewable = viewable;
        this.stopMethod = stopMethod;
        this.destroyMethod = null;
    }

    /**
     * Ctor.
     *
     * @param viewable      last view to attach listeners to
     * @param stopMethod    method to stop
     * @param destroyMethod method to call when destroying
     */
    public EPStatementStartResult(Viewable viewable, EPStatementStopMethod stopMethod, EPStatementDestroyMethod destroyMethod) {
        this.viewable = viewable;
        this.stopMethod = stopMethod;
        this.destroyMethod = destroyMethod;
    }

    /**
     * Returns last view to attached to.
     *
     * @return view
     */
    public Viewable getViewable() {
        return viewable;
    }

    /**
     * Returns stop method.
     *
     * @return stop method.
     */
    public EPStatementStopMethod getStopMethod() {
        return stopMethod;
    }

    /**
     * Returns destroy method.
     *
     * @return destroy method
     */
    public EPStatementDestroyMethod getDestroyMethod() {
        return destroyMethod;
    }
}