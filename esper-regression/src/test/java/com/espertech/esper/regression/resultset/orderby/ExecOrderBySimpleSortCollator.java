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
package com.espertech.esper.regression.resultset.orderby;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.scopetest.EPAssertionUtil;
import com.espertech.esper.client.scopetest.SupportUpdateListener;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.execution.RegressionExecution;

import java.text.Collator;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExecOrderBySimpleSortCollator implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.getEngineDefaults().getLanguage().setSortUsingCollator(true);
    }

    public void run(EPServiceProvider epService) throws Exception {
        epService.getEPAdministrator().getConfiguration().addEventType("SupportBean", SupportBean.class.getName());
        String frenchForSin = "p\u00E9ch\u00E9";
        String frenchForFruit = "p\u00EAche";

        String[] sortedFrench = (frenchForFruit + "," + frenchForSin).split(",");

        assertEquals(1, frenchForFruit.compareTo(frenchForSin));
        assertEquals(-1, frenchForSin.compareTo(frenchForFruit));
        Locale.setDefault(Locale.FRENCH);
        assertEquals(1, frenchForFruit.compareTo(frenchForSin));
        assertEquals(-1, Collator.getInstance().compare(frenchForFruit, frenchForSin));
        assertEquals(-1, frenchForSin.compareTo(frenchForFruit));
        assertEquals(1, Collator.getInstance().compare(frenchForSin, frenchForFruit));
        assertFalse(frenchForSin.equals(frenchForFruit));

        /*
        Collections.sort(items);
        System.out.println("Sorted default" + items);

        Collections.sort(items, new Comparator<String>() {
            Collator collator = Collator.getInstance(Locale.FRANCE);
            public int compare(String o1, String o2)
            {
                return collator.compare(o1, o2);
            }
        });
        System.out.println("Sorted FR" + items);
        */

        // test order by
        String stmtText = "select theString from SupportBean#keepall order by theString asc";
        EPStatement stmtOne = epService.getEPAdministrator().createEPL(stmtText);
        epService.getEPRuntime().sendEvent(new SupportBean(frenchForSin, 1));
        epService.getEPRuntime().sendEvent(new SupportBean(frenchForFruit, 1));
        EPAssertionUtil.assertPropsPerRow(stmtOne.iterator(), "theString".split(","), new Object[][]{{sortedFrench[0]}, {sortedFrench[1]}});

        // test sort view
        SupportUpdateListener listener = new SupportUpdateListener();
        stmtText = "select irstream theString from SupportBean#sort(2, theString asc)";
        EPStatement stmtTwo = epService.getEPAdministrator().createEPL(stmtText);
        stmtTwo.addListener(listener);

        epService.getEPRuntime().sendEvent(new SupportBean(frenchForSin, 1));
        epService.getEPRuntime().sendEvent(new SupportBean(frenchForFruit, 1));
        epService.getEPRuntime().sendEvent(new SupportBean("abc", 1));

        assertEquals(frenchForSin, listener.getLastOldData()[0].get("theString"));
        Locale.setDefault(Locale.US);
    }
}
