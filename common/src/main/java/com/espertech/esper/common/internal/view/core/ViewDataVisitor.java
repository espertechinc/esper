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


import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.internal.collection.ViewUpdatedCollection;

import java.util.Collection;
import java.util.Map;

public interface ViewDataVisitor {
    void visitPrimary(EventBean event, String viewName);

    void visitPrimary(EventBean[] events, String viewName);

    void visitPrimary(Collection<?> primary, boolean countsEvents, String viewName, Integer count);

    void visitPrimary(Map<?, ?> currentBatch, boolean countsEvents, String viewName, Integer count, Integer keyCountWhenAvailable);

    void visitPrimary(ViewUpdatedCollection buffer, String viewName);
}
