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
package com.espertech.esper.regression.event.map;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ExecEventMapInvalidType implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        Map<String, Object> invalid = ExecEventMap.makeMap(new Object[][]{{new SupportBean(), null}});
        tryInvalid(epService, invalid, SupportBean.class.getName() + " cannot be cast to");

        invalid = ExecEventMap.makeMap(new Object[][]{{"abc", new SupportBean()}});
        tryInvalid(epService, invalid, "Nestable type configuration encountered an unexpected property type of 'SupportBean' for property 'abc', expected java.lang.Class or java.util.Map or the name of a previously-declared Map or ObjectArray type");
    }

    private void tryInvalid(EPServiceProvider epService, Map<String, Object> config, String message) {
        try {
            epService.getEPAdministrator().getConfiguration().addEventType("NestedMap", config);
            fail();
        } catch (Exception ex) {
            // Comment-me-in: log.error(ex.getMessage(), ex);
            assertTrue("expected '" + message + "' but received '" + ex.getMessage(), ex.getMessage().contains(message));
        }
    }

    private final static Logger log = LoggerFactory.getLogger(TestSuiteEventMap.class);
}
