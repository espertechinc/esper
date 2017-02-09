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

import java.util.List;

public class ViewServiceCreateResult {
    private final Viewable finalViewable;
    private final Viewable topViewable;
    private final List<View> newViews;

    public ViewServiceCreateResult(Viewable finalViewable, Viewable topViewable, List<View> newViews) {
        this.finalViewable = finalViewable;
        this.topViewable = topViewable;
        this.newViews = newViews;
    }

    public Viewable getFinalViewable() {
        return finalViewable;
    }

    public Viewable getTopViewable() {
        return topViewable;
    }

    public List<View> getNewViews() {
        return newViews;
    }
}
