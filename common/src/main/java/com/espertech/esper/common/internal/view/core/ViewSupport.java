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

public abstract class ViewSupport implements View {

    protected Viewable parent;
    protected View child;

    public Viewable getParent() {
        return parent;
    }

    public void setParent(Viewable parent) {
        this.parent = parent;
    }

    public void setChild(View view) {
        this.child = view;
    }

    public View getChild() {
        return child;
    }
}
