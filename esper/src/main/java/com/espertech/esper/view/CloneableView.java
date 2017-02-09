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
 * Views that can work under a group-by must be able to duplicate and are required to implement this interface.
 */
public interface CloneableView {
    /**
     * Duplicates the view.
     * <p>
     * Expected to return a same view in initialized state for grouping.
     *
     * @return cloned view
     */
    public View cloneView();
}
