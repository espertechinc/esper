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
package com.espertech.esper.common.internal.statement.helper;

import com.espertech.esper.common.internal.view.core.DataWindowViewWithPrevious;
import com.espertech.esper.common.internal.view.core.ViewFactory;
import com.espertech.esper.common.internal.view.groupwin.GroupByViewFactory;

public class EPStatementStartMethodHelperPrevious {
    public static DataWindowViewWithPrevious findPreviousViewFactory(ViewFactory[] factories) {
        ViewFactory factoryFound = null;
        for (ViewFactory factory : factories) {
            if (factory instanceof DataWindowViewWithPrevious) {
                factoryFound = factory;
                break;
            }
            if (factory instanceof GroupByViewFactory) {
                GroupByViewFactory grouped = (GroupByViewFactory) factory;
                return findPreviousViewFactory(grouped.getGroupeds());
            }
        }
        if (factoryFound == null) {
            throw new RuntimeException("Failed to find 'previous'-handling view factory");  // was verified earlier, should not occur
        }
        return (DataWindowViewWithPrevious) factoryFound;
    }
}
