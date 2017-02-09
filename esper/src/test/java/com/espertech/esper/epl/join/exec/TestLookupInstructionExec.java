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
package com.espertech.esper.epl.join.exec;

import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.epl.join.exec.base.JoinExecTableLookupStrategy;
import com.espertech.esper.epl.join.exec.base.LookupInstructionExec;
import com.espertech.esper.supportunit.epl.join.SupportRepositoryImpl;
import com.espertech.esper.supportunit.epl.join.SupportTableLookupStrategy;
import junit.framework.TestCase;

public class TestLookupInstructionExec extends TestCase {
    private LookupInstructionExec exec;
    private SupportRepositoryImpl rep;
    private JoinExecTableLookupStrategy[] lookupStrategies;

    public void setUp() {
        lookupStrategies = new JoinExecTableLookupStrategy[4];
        for (int i = 0; i < lookupStrategies.length; i++) {
            lookupStrategies[i] = new SupportTableLookupStrategy(1);
        }

        exec = new LookupInstructionExec(0, "test",
                new int[]{1, 2, 3, 4}, lookupStrategies, new boolean[]{false, true, true, false, false});

        rep = new SupportRepositoryImpl();
    }

    public void testProcessAllResults() {
        boolean result = exec.process(rep, null);

        assertTrue(result);
        assertEquals(4, rep.getLookupResultsList().size());
        EPAssertionUtil.assertEqualsExactOrder(rep.getResultStreamList().toArray(), new Object[]{1, 2, 3, 4});
    }

    public void testProcessNoRequiredResults() {
        lookupStrategies[1] = new SupportTableLookupStrategy(0);

        boolean result = exec.process(rep, null);

        assertFalse(result);
        assertEquals(0, rep.getLookupResultsList().size());
    }

    public void testProcessPartialOptionalResults() {
        lookupStrategies[3] = new SupportTableLookupStrategy(0);

        boolean result = exec.process(rep, null);

        assertTrue(result);
        assertEquals(3, rep.getLookupResultsList().size());
    }
}
