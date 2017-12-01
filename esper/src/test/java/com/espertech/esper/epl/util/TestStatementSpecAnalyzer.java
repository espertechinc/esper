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
package com.espertech.esper.epl.util;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.core.service.EPAdministratorSPI;
import com.espertech.esper.epl.spec.FilterSpecRaw;
import com.espertech.esper.epl.spec.StatementSpecRaw;
import junit.framework.TestCase;

import java.util.List;

public class TestStatementSpecAnalyzer extends TestCase {

    private EPServiceProvider engine;

    public void setUp() {
        Configuration config = new Configuration();
        config.getEngineDefaults().getThreading().setInternalTimerEnabled(false);
        config.getEngineDefaults().getByteCodeGeneration().setEnableExpression(false);
        engine = EPServiceProviderManager.getDefaultProvider(config);
    }

    public void testFilterWalker() throws Exception {
        engine.getEPAdministrator().createEPL("create schema A1 as (col1 string)");

        assertEquals(1, getFilters("select * from A1").size());
        assertEquals(2, getFilters("select * from A1#lastevent, A1#lastevent").size());
        assertEquals(2, getFilters("select (select col1 from A1#lastevent), col1 from A1#lastevent").size());
        assertEquals(2, getFilters("select * from pattern [A1 -> A1(col1='a')]").size());
    }

    private List<FilterSpecRaw> getFilters(String epl) throws Exception {
        EPAdministratorSPI spi = (EPAdministratorSPI) engine.getEPAdministrator();
        StatementSpecRaw raw = spi.compileEPLToRaw(epl);
        List<FilterSpecRaw> filters = StatementSpecRawAnalyzer.analyzeFilters(raw);
        return filters;
    }

}
