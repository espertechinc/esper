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
package com.espertech.esper.common.internal.view.core;

public class ViewablePair {
    private final Viewable top;
    private final Viewable last;

    public ViewablePair(Viewable top, Viewable last) {
        this.top = top;
        this.last = last;
    }

    public Viewable getTop() {
        return top;
    }

    public Viewable getLast() {
        return last;
    }
}
