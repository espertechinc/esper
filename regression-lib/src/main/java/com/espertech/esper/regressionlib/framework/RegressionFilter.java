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
package com.espertech.esper.regressionlib.framework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RegressionFilter {
    private final static String TEST_SYSTEM_PROPERTY = "esper_test";

    public static Collection<? extends RegressionExecution> filterBySystemProperty(Collection<? extends RegressionExecution> executions) {
        String property = System.getProperty(TEST_SYSTEM_PROPERTY);
        if (property == null) {
            return executions;
        }
        List<RegressionExecution> filtered = new ArrayList<>();
        for (RegressionExecution execution : executions) {
            String simpleName = execution.getClass().getSimpleName();
            if (simpleName.equals(property)) {
                filtered.add(execution);
            }
        }
        return filtered;
    }
}
