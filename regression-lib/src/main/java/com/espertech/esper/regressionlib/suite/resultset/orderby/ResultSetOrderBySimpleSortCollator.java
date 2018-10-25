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
package com.espertech.esper.regressionlib.suite.resultset.orderby;

import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.common.internal.support.SupportBean;
import org.junit.Assert;

import java.text.Collator;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ResultSetOrderBySimpleSortCollator implements RegressionExecution {

    public void run(RegressionEnvironment env) {
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
        String stmtText = "@name('s0') select theString from SupportBean#keepall order by theString asc";
        env.compileDeploy(stmtText).addListener("s0");
        env.sendEventBean(new SupportBean(frenchForSin, 1));

        env.milestone(0);

        env.sendEventBean(new SupportBean(frenchForFruit, 1));
        EPAssertionUtil.assertPropsPerRow(env.statement("s0").iterator(), "theString".split(","), new Object[][]{{sortedFrench[0]}, {sortedFrench[1]}});
        env.undeployAll();

        // test sort view
        stmtText = "@name('s0') select irstream theString from SupportBean#sort(2, theString asc)";
        env.compileDeploy(stmtText).addListener("s0");

        env.sendEventBean(new SupportBean(frenchForSin, 1));

        env.milestone(1);

        env.sendEventBean(new SupportBean(frenchForFruit, 1));
        env.sendEventBean(new SupportBean("abc", 1));

        Assert.assertEquals(frenchForSin, env.listener("s0").getLastOldData()[0].get("theString"));
        Locale.setDefault(Locale.US);

        env.undeployAll();
    }
}
