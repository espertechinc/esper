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
package com.espertech.esper.regression.event.bean;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.ConfigurationEventTypeLegacy;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

public class ExecEventBeanPropertyResolutionCaseInsensitiveConfigureType implements RegressionExecution {
    public void configure(Configuration configuration) throws Exception {
        ConfigurationEventTypeLegacy legacyDef = new ConfigurationEventTypeLegacy();
        legacyDef.setPropertyResolutionStyle(Configuration.PropertyResolutionStyle.CASE_INSENSITIVE);
        configuration.addEventType("Bean", SupportBean.class.getName(), legacyDef);
    }

    public void run(EPServiceProvider epService) throws Exception {
        ExecEventBeanPropertyResolutionCaseInsensitiveEngineDefault.tryCaseInsensitive(epService, "select THESTRING, INTPRIMITIVE from Bean where THESTRING='A'", "THESTRING", "INTPRIMITIVE");
        ExecEventBeanPropertyResolutionCaseInsensitiveEngineDefault.tryCaseInsensitive(epService, "select ThEsTrInG, INTprimitIVE from Bean where THESTRing='A'", "ThEsTrInG", "INTprimitIVE");
    }
}
