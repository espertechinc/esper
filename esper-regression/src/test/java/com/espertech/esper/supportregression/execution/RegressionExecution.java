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
package com.espertech.esper.supportregression.execution;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;

public interface RegressionExecution {
    default void configure(Configuration configuration) throws Exception {
    }

    void run(EPServiceProvider epService) throws Exception;

    default boolean excludeWhenInstrumented() {
        return false;
    }
}
