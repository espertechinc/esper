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
package com.espertech.esper.regression.rowrecog;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.rowrecog.SupportRecogBean;

import static com.espertech.esper.supportregression.util.SupportMessageAssertUtil.tryInvalid;

public class ExecRowRecogInvalid implements RegressionExecution {

    public void configure(Configuration configuration) throws Exception {
        configuration.addEventType("MyEvent", SupportRecogBean.class);
    }

    public void run(EPServiceProvider epService) throws Exception {
        String text = "select * from MyEvent " +
                "match_recognize (" +
                " measures A as a_array" +
                " pattern (A+ B)" +
                " define" +
                " A as A.theString = B.theString)";
        tryInvalid(epService, text, "Error starting statement: Failed to validate condition expression for variable 'A': Failed to validate match-recognize define expression 'A.theString=B.theString': Failed to find a stream named 'B' (did you mean 'A'?) [select * from MyEvent match_recognize ( measures A as a_array pattern (A+ B) define A as A.theString = B.theString)]");

        // invalid after syntax
        text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                "  measures A.theString as a" +
                "  AFTER MATCH SKIP TO OTHER ROW " +
                "  pattern (A B*) " +
                "  define " +
                "    A as A.theString like 'A%'" +
                ")";
        tryInvalid(epService, text, "Match-recognize AFTER clause must be either AFTER MATCH SKIP TO LAST ROW or AFTER MATCH SKIP TO NEXT ROW or AFTER MATCH SKIP TO CURRENT ROW [select * from MyEvent#keepall match_recognize (  measures A.theString as a  AFTER MATCH SKIP TO OTHER ROW   pattern (A B*)   define     A as A.theString like 'A%')]");

        // property cannot resolve
        text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                "  measures A.theString as a, D.theString as x" +
                "  pattern (A B*) " +
                "  define " +
                "    A as A.theString like 'A%'" +
                ")";
        tryInvalid(epService, text, "Error starting statement: Failed to validate match-recognize measure expression 'D.theString': Failed to resolve property 'D.theString' to a stream or nested property in a stream [select * from MyEvent#keepall match_recognize (  measures A.theString as a, D.theString as x  pattern (A B*)   define     A as A.theString like 'A%')]");

        // property not named
        text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                "  measures A.theString, A.theString as xxx" +
                "  pattern (A B*) " +
                "  define " +
                "    A as A.theString like 'A%'" +
                ")";
        tryInvalid(epService, text, "Error starting statement: The measures clause requires that each expression utilizes the AS keyword to assign a column name [select * from MyEvent#keepall match_recognize (  measures A.theString, A.theString as xxx  pattern (A B*)   define     A as A.theString like 'A%')]");

        // grouped property not indexed
        text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                "  measures B.theString as b1" +
                "  pattern (A B*) " +
                "  define " +
                "    A as A.theString like 'A%'" +
                ")";
        tryInvalid(epService, text, "Error starting statement: Failed to validate match-recognize measure expression 'B.theString': Failed to resolve property 'B.theString' (property 'B' is an indexed property and requires an index or enumeration method to access values) [select * from MyEvent#keepall match_recognize (  measures B.theString as b1  pattern (A B*)   define     A as A.theString like 'A%')]");

        // define twice
        text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                "  measures A.theString as a" +
                "  pattern (A B*) " +
                "  define " +
                "    A as A.theString like 'A%'," +
                "    A as A.theString like 'A%'" +
                ")";
        tryInvalid(epService, text, "Error starting statement: Variable 'A' has already been defined [select * from MyEvent#keepall match_recognize (  measures A.theString as a  pattern (A B*)   define     A as A.theString like 'A%',    A as A.theString like 'A%')]");

        // define for not used variable
        text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                "  measures A.theString as a" +
                "  pattern (A B*) " +
                "  define " +
                "    X as X.theString like 'A%'" +
                ")";
        tryInvalid(epService, text, "Error starting statement: Variable 'X' does not occur in pattern [select * from MyEvent#keepall match_recognize (  measures A.theString as a  pattern (A B*)   define     X as X.theString like 'A%')]");

        // define mentions another variable
        text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                "  measures A.theString as a" +
                "  pattern (A B*) " +
                "  define " +
                "    A as B.theString like 'A%'" +
                ")";
        tryInvalid(epService, text, "Error starting statement: Failed to validate condition expression for variable 'A': Failed to validate match-recognize define expression 'B.theString like \"A%\"': Failed to find a stream named 'B' (did you mean 'A'?) [select * from MyEvent#keepall match_recognize (  measures A.theString as a  pattern (A B*)   define     A as B.theString like 'A%')]");

        // aggregation over multiple groups
        text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                "  measures sum(A.value+B.value) as mytotal" +
                "  pattern (A* B*) " +
                "  define " +
                "    A as A.theString like 'A%'" +
                ")";
        tryInvalid(epService, text, "Error starting statement: Aggregation functions in the measure-clause must only refer to properties of exactly one group variable returning multiple events [select * from MyEvent#keepall match_recognize (  measures sum(A.value+B.value) as mytotal  pattern (A* B*)   define     A as A.theString like 'A%')]");

        // aggregation over no groups
        text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                "  measures sum(A.value) as mytotal" +
                "  pattern (A B*) " +
                "  define " +
                "    A as A.theString like 'A%'" +
                ")";
        tryInvalid(epService, text, "Error starting statement: Aggregation functions in the measure-clause must refer to one or more properties of exactly one group variable returning multiple events [select * from MyEvent#keepall match_recognize (  measures sum(A.value) as mytotal  pattern (A B*)   define     A as A.theString like 'A%')]");

        // aggregation in define
        text = "select * from MyEvent#keepall " +
                "match_recognize (" +
                "  measures A.theString as astring" +
                "  pattern (A B) " +
                "  define " +
                "    A as sum(A.value + A.value) > 3000" +
                ")";
        tryInvalid(epService, text, "Error starting statement: Failed to validate condition expression for variable 'A': An aggregate function may not appear in a DEFINE clause [select * from MyEvent#keepall match_recognize (  measures A.theString as astring  pattern (A B)   define     A as sum(A.value + A.value) > 3000)]");

        // join disallowed
        text = "select * from MyEvent#keepall, MyEvent#keepall " +
                "match_recognize (" +
                "  measures A.value as aval" +
                "  pattern (A B*) " +
                "  define " +
                "    A as A.theString like 'A%'" +
                ")";
        tryInvalid(epService, text, "Error starting statement: Joins are not allowed when using match-recognize [select * from MyEvent#keepall, MyEvent#keepall match_recognize (  measures A.value as aval  pattern (A B*)   define     A as A.theString like 'A%')]");
    }
}