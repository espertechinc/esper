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
package com.espertech.esper.regressionlib.suite.pattern;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.runtime.client.EPRuntime;
import com.espertech.esper.runtime.client.EPStatement;
import com.espertech.esper.runtime.client.UpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PatternStartLoop implements RegressionExecution {
    /**
     * Starting this statement fires an event and the listener starts a new statement (same expression) again,
     * causing a loop. This listener limits to 10 - this is a smoke test.
     */
    public void run(RegressionEnvironment env) {
        String patternExpr = "@name('s0') select * from pattern [not SupportBean]";
        env.compileDeploy(patternExpr);
        env.statement("s0").addListener(new PatternUpdateListener(env));
        env.undeployAll();
        env.compileDeploy(patternExpr);
        env.undeployAll();
    }

    class PatternUpdateListener implements UpdateListener {
        private final RegressionEnvironment env;

        public PatternUpdateListener(RegressionEnvironment env) {
            this.env = env;
        }

        private int count = 0;

        public void update(EventBean[] newEvents, EventBean[] oldEvents, EPStatement statement, EPRuntime runtime) {
            log.warn(".update");

            if (count < 10) {
                count++;
                String patternExpr = "@name('ST" + count + "') select * from pattern[not SupportBean]";
                env.compileDeploy(patternExpr).addListener("ST" + count);
                env.undeployModuleContaining("ST" + count);
                env.compileDeploy(patternExpr).addListener("ST" + count);
            }
        }

        public int getCount() {
            return count;
        }
    }

    private final static Logger log = LoggerFactory.getLogger(PatternStartLoop.class);
}
