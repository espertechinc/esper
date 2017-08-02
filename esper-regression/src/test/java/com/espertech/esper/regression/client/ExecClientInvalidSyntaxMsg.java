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
package com.espertech.esper.regression.client;

import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBeanReservedKeyword;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalidSyntax;

public class ExecClientInvalidSyntaxMsg implements RegressionExecution {
    public void run(EPServiceProvider epService) throws Exception {
        tryInvalidSyntax(epService, "insert into 7event select * from " + SupportBeanReservedKeyword.class.getName(),
                "Incorrect syntax near '7' at line 1 column 12");

        tryInvalidSyntax(epService, "select foo, create from " + SupportBeanReservedKeyword.class.getName(),
                "Incorrect syntax near 'create' (a reserved keyword) at line 1 column 12, please check the select clause");

        tryInvalidSyntax(epService, "select * from pattern [",
                "Unexpected end-of-input at line 1 column 23, please check the pattern expression within the from clause");

        tryInvalidSyntax(epService, "select * from A, into",
                "Incorrect syntax near 'into' (a reserved keyword) at line 1 column 17, please check the from clause");

        tryInvalidSyntax(epService, "select * from pattern[A -> B - C]",
                "Incorrect syntax near '-' expecting a right angle bracket ']' but found a minus '-' at line 1 column 29, please check the from clause");

        tryInvalidSyntax(epService, "insert into A (a",
                "Unexpected end-of-input at line 1 column 16 [insert into A (a]");

        tryInvalidSyntax(epService, "select case when 1>2 from A",
                "Incorrect syntax near 'from' (a reserved keyword) expecting 'then' but found 'from' at line 1 column 21, please check the case expression within the select clause [select case when 1>2 from A]");

        tryInvalidSyntax(epService, "select * from A full outer join B on A.field < B.field",
                "Incorrect syntax near '<' expecting an equals '=' but found a lesser then '<' at line 1 column 45, please check the outer join within the from clause [select * from A full outer join B on A.field < B.field]");

        tryInvalidSyntax(epService, "select a.b('aa\") from A",
                "Unexpected end-of-input at line 1 column 23, please check the select clause [select a.b('aa\") from A]");

        tryInvalidSyntax(epService, "select * from A, sql:mydb [\"",
                "Unexpected end-of-input at line 1 column 28, please check the relational data join within the from clause [select * from A, sql:mydb [\"]");

        tryInvalidSyntax(epService, "select * google",
                "Incorrect syntax near 'google' at line 1 column 9 [");

        tryInvalidSyntax(epService, "insert into into",
                "Incorrect syntax near 'into' (a reserved keyword) at line 1 column 12 [insert into into]");

        epService.getEPAdministrator().getConfiguration().addEventType(SupportBean.class);
        SupportMessageAssertUtil.tryInvalid(epService, "on SupportBean select 1",
                "Error starting statement: Required insert-into clause is not provided, the clause is required for split-stream syntax");
    }
}
