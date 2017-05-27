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
package com.espertech.esper.regression.pattern;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecPatternStartLoop implements RegressionExecution {
    /**
     * Starting this statement fires an event and the listener starts a new statement (same expression) again,
     * causing a loop. This listener limits to 10 - this is a smoke test.
     */
    public void run(EPServiceProvider epService) throws Exception {
        String patternExpr = "not " + SupportBean.class.getName();
        EPStatement patternStmt = epService.getEPAdministrator().createPattern(patternExpr);
        patternStmt.addListener(new PatternUpdateListener(epService));
        patternStmt.stop();
        patternStmt.start();
    }

    class PatternUpdateListener implements UpdateListener {
        private final EPServiceProvider epService;

        public PatternUpdateListener(EPServiceProvider epService) {
            this.epService = epService;
        }

        private int count = 0;

        public void update(EventBean[] newEvents, EventBean[] oldEvents) {
            log.warn(".update");

            if (count < 10) {
                count++;
                String patternExpr = "not " + SupportBean.class.getName();
                EPStatement patternStmt = epService.getEPAdministrator().createPattern(patternExpr);
                patternStmt.addListener(this);
                patternStmt.stop();
                patternStmt.start();
            }
        }

        public int getCount() {
            return count;
        }
    }

    private final static Logger log = LoggerFactory.getLogger(ExecPatternStartLoop.class);
}
