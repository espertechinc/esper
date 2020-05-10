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
package com.espertech.esper.regressionlib.support.filter;

import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompilerExecution;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;

public class SupportFilterOptimizableHelper {
    public static boolean hasFilterIndexPlanBasic(RegressionEnvironment env) {
        return env.getConfiguration().getCompiler().getExecution().getFilterIndexPlanning() == ConfigurationCompilerExecution.FilterIndexPlanning.BASIC;
    }

    public static boolean hasFilterIndexPlanBasicOrMore(RegressionEnvironment env) {
        return env.getConfiguration().getCompiler().getExecution().getFilterIndexPlanning().ordinal() >= ConfigurationCompilerExecution.FilterIndexPlanning.BASIC.ordinal();
    }

    public static boolean hasFilterIndexPlanAdvanced(RegressionEnvironment env) {
        return env.getConfiguration().getCompiler().getExecution().getFilterIndexPlanning() == ConfigurationCompilerExecution.FilterIndexPlanning.ADVANCED;
    }
}
