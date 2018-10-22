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
package com.espertech.esper.regressionlib.suite.event.bean;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

public class EventBeanPropertyResolutionCaseInsensitiveConfigureType implements RegressionExecution {

    public void run(RegressionEnvironment env) {
        EventBeanPropertyResolutionCaseInsensitiveEngineDefault.tryCaseInsensitive(env, "BeanWithCaseInsensitive", "@name('s0') select THESTRING, INTPRIMITIVE from BeanWithCaseInsensitive where THESTRING='A'", "THESTRING", "INTPRIMITIVE");
        EventBeanPropertyResolutionCaseInsensitiveEngineDefault.tryCaseInsensitive(env, "BeanWithCaseInsensitive", "@name('s0') select ThEsTrInG, INTprimitIVE from BeanWithCaseInsensitive where THESTRing='A'", "ThEsTrInG", "INTprimitIVE");
    }
}
