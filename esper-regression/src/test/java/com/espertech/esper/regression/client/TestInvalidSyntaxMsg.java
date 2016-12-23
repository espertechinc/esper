/*
 * *************************************************************************************
 *  Copyright (C) 2006-2015 EsperTech, Inc. All rights reserved.                       *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 * *************************************************************************************
 */

package com.espertech.esper.regression.client;

import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;
import junit.framework.TestCase;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatementSyntaxException;
import com.espertech.esper.supportregression.bean.SupportBeanReservedKeyword;
import com.espertech.esper.supportregression.client.SupportConfigFactory;

public class TestInvalidSyntaxMsg extends TestCase
{
    private EPServiceProvider epService;

    public void setUp()
    {
        epService = EPServiceProviderManager.getDefaultProvider(SupportConfigFactory.getConfiguration());
        epService.initialize();
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.startTest(epService, this.getClass(), getName());}
    }

    public void tearDown() {
        if (InstrumentationHelper.ENABLED) { InstrumentationHelper.endTest();}
    }

    public void testInvalidSyntax()
    {
        tryCompile("insert into 7event select * from " + SupportBeanReservedKeyword.class.getName(),
                   "Incorrect syntax near '7' at line 1 column 12");

        tryCompile("select foo, create from " + SupportBeanReservedKeyword.class.getName(),
                   "Incorrect syntax near 'create' (a reserved keyword) at line 1 column 12, please check the select clause");

        tryCompile("select * from pattern [",
                   "Unexpected end-of-input at line 1 column 23, please check the pattern expression within the from clause");

        tryCompile("select * from A, into",
                   "Incorrect syntax near 'into' (a reserved keyword) at line 1 column 17, please check the from clause");

        tryCompile("select * from pattern[A -> B - C]",
                   "Incorrect syntax near '-' at line 1 column 29, please check the pattern expression within the from clause");

        tryCompile("insert into A (a",
                   "Unexpected end-of-input at line 1 column 16 [insert into A (a]");

        tryCompile("select case when 1>2 from A",
                   "Incorrect syntax near 'from' (a reserved keyword) expecting 'then' but found 'from' at line 1 column 21, please check the case expression within the select clause [select case when 1>2 from A]");

        tryCompile("select * from A full outer join B on A.field < B.field",
                   "Incorrect syntax near '<' expecting an equals '=' but found a lesser then '<' at line 1 column 45, please check the outer join within the from clause [select * from A full outer join B on A.field < B.field]");

        tryCompile("select a.b('aa\") from A",
                   "Unexpected end-of-input at line 1 column 23, please check the select clause [select a.b('aa\") from A]");

        tryCompile("select * from A, sql:mydb [\"",
                   "Unexpected end-of-input at line 1 column 28, please check the relational data join within the from clause [select * from A, sql:mydb [\"]");

        tryCompile("select * google",
                   "Incorrect syntax near 'google' at line 1 column 9 [");

        tryCompile("insert into into",
                   "Incorrect syntax near 'into' (a reserved keyword) at line 1 column 12 [insert into into]");

        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        SupportMessageAssertUtil.tryInvalid(epService, "on SupportBean select 1",
                "Error starting statement: Required insert-into clause is not provided, the clause is required for split-stream syntax");
    }

    private void tryCompile(String expression, String expectedMsg)
    {
        try
        {
            epService.getEPAdministrator().createEPL(expression);
            fail();
        }
        catch (EPStatementSyntaxException ex)
        {
            SupportMessageAssertUtil.assertMessage(ex, expectedMsg);
        }
    }

}
