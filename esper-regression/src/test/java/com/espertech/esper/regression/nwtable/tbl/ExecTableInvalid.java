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
package com.espertech.esper.regression.nwtable.tbl;

import com.espertech.esper.client.*;
import com.espertech.esper.client.hook.*;
import com.espertech.esper.epl.agg.aggregator.AggregationMethod;
import com.espertech.esper.epl.agg.service.common.AggregationValidationContext;
import com.espertech.esper.supportregression.bean.SupportBean;
import com.espertech.esper.supportregression.bean.SupportBean_S0;
import com.espertech.esper.supportregression.bean.SupportBean_S1;
import com.espertech.esper.supportregression.client.SupportAggMFFactory;
import com.espertech.esper.supportregression.client.SupportAggMFFunc;
import com.espertech.esper.supportregression.execution.RegressionExecution;
import com.espertech.esper.supportregression.util.SupportMessageAssertUtil;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ExecTableInvalid implements RegressionExecution {

    public void run(EPServiceProvider epService) throws Exception {
        for (Class clazz : new Class[]{SupportBean.class, SupportBean_S0.class, SupportBean_S1.class}) {
            epService.getEPAdministrator().getConfiguration().addEventType(clazz);
        }

        runAssertionInvalidAggMatchSingleFunc(epService);
        runAssertionInvalidAggMatchMultiFunc(epService);
        runAssertionInvalidAnnotations(epService);
        runAssertionInvalid(epService);
    }

    private void runAssertionInvalidAggMatchSingleFunc(EPServiceProvider epService) {
        // sum
        tryInvalidAggMatch(epService, "var1", "sum(double)", false, "sum(intPrimitive)",
                "Error starting statement: Incompatible aggregation function for table 'var1' column 'value', expecting 'sum(double)' and received 'sum(intPrimitive)': The required parameter type is java.lang.Double and provided is java.lang.Integer [");
        tryInvalidAggMatch(epService, "var1", "sum(double)", false, "count(*)",
                "Error starting statement: Incompatible aggregation function for table 'var1' column 'value', expecting 'sum(double)' and received 'count(*)': Not a 'sum' aggregation [");
        tryInvalidAggMatch(epService, "var1", "sum(double)", false, "sum(doublePrimitive, theString='a')",
                "Error starting statement: Incompatible aggregation function for table 'var1' column 'value', expecting 'sum(double)' and received 'sum(doublePrimitive,theString=\"a\")': The aggregation declares no filter expression and provided is a filter expression [");
        tryInvalidAggMatch(epService, "var1", "sum(double, boolean)", false, "sum(doublePrimitive)",
                "Error starting statement: Incompatible aggregation function for table 'var1' column 'value', expecting 'sum(double,boolean)' and received 'sum(doublePrimitive)': The aggregation declares a filter expression and provided is no filter expression [");

        // count
        tryInvalidAggMatch(epService, "var1", "count(*)", false, "sum(intPrimitive)",
                "Error starting statement: Incompatible aggregation function for table 'var1' column 'value', expecting 'count(*)' and received 'sum(intPrimitive)': Not a 'count' aggregation [");
        tryInvalidAggMatch(epService, "var1", "count(*)", false, "count(distinct intPrimitive)",
                "Error starting statement: Incompatible aggregation function for table 'var1' column 'value', expecting 'count(*)' and received 'count(distinct intPrimitive)': The aggregation declares no distinct and provided is a distinct [");
        tryInvalidAggMatch(epService, "var1", "count(*)", false, "count(distinct intPrimitive, boolPrimitive)", null);
        tryInvalidAggMatch(epService, "var1", "count(distinct int)", false, "count(distinct doublePrimitive)", null);
        tryInvalidAggMatch(epService, "var1", "count(int)", false, "count(*)",
                "Error starting statement: Incompatible aggregation function for table 'var1' column 'value', expecting 'count(int)' and received 'count(*)': The aggregation declares ignore nulls and provided is no ignore nulls [");

        // avg
        tryInvalidAggMatch(epService, "var1", "avg(int)", false, "sum(intPrimitive)", null);
        tryInvalidAggMatch(epService, "var1", "avg(int)", false, "avg(longPrimitive)", null);
        tryInvalidAggMatch(epService, "var1", "avg(int)", false, "avg(intPrimitive, boolPrimitive)", null);
        tryInvalidAggMatch(epService, "var1", "avg(int)", false, "avg(distinct intPrimitive)", null);

        // min-max
        tryInvalidAggMatch(epService, "var1", "max(int)", false, "min(intPrimitive)",
                "Error starting statement: Incompatible aggregation function for table 'var1' column 'value', expecting 'max(int)' and received 'min(intPrimitive)': The aggregation declares max and provided is min [");
        tryInvalidAggMatch(epService, "var1", "min(int)", false, "avg(intPrimitive)", null);
        tryInvalidAggMatch(epService, "var1", "min(int)", false, "min(doublePrimitive)", null);
        tryInvalidAggMatch(epService, "var1", "min(int)", false, "fmin(intPrimitive, theString='a')",
                "Error starting statement: Incompatible aggregation function for table 'var1' column 'value', expecting 'min(int)' and received 'min(intPrimitive,theString=\"a\")': The aggregation declares no filter expression and provided is a filter expression [");

        // stddev
        tryInvalidAggMatch(epService, "var1", "stddev(int)", false, "avg(intPrimitive)", null);
        tryInvalidAggMatch(epService, "var1", "stddev(int)", false, "stddev(doublePrimitive)", null);
        tryInvalidAggMatch(epService, "var1", "stddev(int)", false, "stddev(intPrimitive, true)", null);

        // avedev
        tryInvalidAggMatch(epService, "var1", "avedev(int)", false, "avg(intPrimitive)", null);
        tryInvalidAggMatch(epService, "var1", "avedev(int)", false, "avedev(doublePrimitive)", null);
        tryInvalidAggMatch(epService, "var1", "avedev(int)", false, "avedev(intPrimitive, true)", null);

        // median
        tryInvalidAggMatch(epService, "var1", "median(int)", false, "avg(intPrimitive)", null);
        tryInvalidAggMatch(epService, "var1", "median(int)", false, "median(doublePrimitive)", null);
        tryInvalidAggMatch(epService, "var1", "median(int)", false, "median(intPrimitive, true)", null);

        // firstever
        tryInvalidAggMatch(epService, "var1", "firstever(int)", false, "lastever(intPrimitive)", null);
        tryInvalidAggMatch(epService, "var1", "firstever(int)", false, "firstever(doublePrimitive)", null);
        tryInvalidAggMatch(epService, "var1", "firstever(int, boolean)", false, "firstever(intPrimitive)", null);

        // lastever
        tryInvalidAggMatch(epService, "var1", "lastever(int)", false, "firstever(intPrimitive)", null);
        tryInvalidAggMatch(epService, "var1", "lastever(int)", false, "lastever(doublePrimitive)", null);
        tryInvalidAggMatch(epService, "var1", "lastever(int, boolean)", false, "lastever(intPrimitive)", null);

        // countever
        tryInvalidAggMatch(epService, "var1", "lastever(int)", true, "countever(intPrimitive)", null);
        tryInvalidAggMatch(epService, "var1", "lastever(int, boolean)", true, "countever(intPrimitive)", null);
        tryInvalidAggMatch(epService, "var1", "lastever(int)", true, "countever(intPrimitive, true)", null);
        tryInvalidAggMatch(epService, "var1", "countever(*)", true, "countever(intPrimitive)", null);

        // nth
        tryInvalidAggMatch(epService, "var1", "nth(int, 10)", false, "avg(20)", null);
        tryInvalidAggMatch(epService, "var1", "nth(int, 10)", false, "nth(intPrimitive, 11)",
                "Error starting statement: Incompatible aggregation function for table 'var1' column 'value', expecting 'nth(int,10)' and received 'nth(intPrimitive,11)': The size is 10 and provided is 11 [");
        tryInvalidAggMatch(epService, "var1", "nth(int, 10)", false, "nth(doublePrimitive, 10)", null);

        // rate
        tryInvalidAggMatch(epService, "var1", "rate(20)", false, "avg(20)", null);
        tryInvalidAggMatch(epService, "var1", "rate(20)", false, "rate(11)",
                "Error starting statement: Incompatible aggregation function for table 'var1' column 'value', expecting 'rate(20)' and received 'rate(11)': The size is 20000 and provided is 11000 [");

        // leaving
        tryInvalidAggMatch(epService, "var1", "leaving()", false, "avg(intPrimitive)", null);

        // plug-in single-func
        epService.getEPAdministrator().getConfiguration().addPlugInAggregationFunctionFactory("myaggsingle", MyAggregationFunctionFactory.class.getName());
        tryInvalidAggMatch(epService, "var1", "myaggsingle()", false, "leaving()",
                "Error starting statement: Incompatible aggregation function for table 'var1' column 'value', expecting 'myaggsingle(*)' and received 'leaving(*)': Not a 'myaggsingle' aggregation [");
    }

    private void runAssertionInvalidAggMatchMultiFunc(EPServiceProvider epService) {
        // Window and related
        //

        // window vs agg method
        tryInvalidAggMatch(epService, "var1", "window(*) @type(SupportBean)", true, "avg(intPrimitive)",
                "Error starting statement: Incompatible aggregation function for table 'var1' column 'value', expecting 'window(*)' and received 'avg(intPrimitive)': Not a 'window' aggregation [");
        // window vs sorted
        tryInvalidAggMatch(epService, "var1", "window(*) @type(SupportBean)", true, "sorted(intPrimitive)",
                "Error starting statement: Failed to validate select-clause expression 'sorted(intPrimitive)': When specifying into-table a sort expression cannot be provided [");
        // wrong type
        tryInvalidAggMatch(epService, "var1", "window(*) @type(SupportBean_S0)", true, "window(*)",
                "Error starting statement: Incompatible aggregation function for table 'var1' column 'value', expecting 'window(*)' and received 'window(*)': The required event type is 'SupportBean_S0' and provided is 'SupportBean' [");

        // sorted
        //
        tryInvalidAggMatch(epService, "var1", "sorted(intPrimitive) @type(SupportBean)", true, "window(*)",
                "Error starting statement: Incompatible aggregation function for table 'var1' column 'value', expecting 'sorted(intPrimitive)' and received 'window(*)': Not a 'sorted' aggregation [");
        tryInvalidAggMatch(epService, "var1", "sorted(id) @type(SupportBean_S0)", true, "sorted(intPrimitive)",
                "Error starting statement: Failed to validate select-clause expression 'sorted(intPrimitive)': When specifying into-table a sort expression cannot be provided [");

        // plug-in
        //
        ConfigurationPlugInAggregationMultiFunction config = new ConfigurationPlugInAggregationMultiFunction(SupportAggMFFunc.getFunctionNames(), SupportAggMFFactory.class.getName());
        epService.getEPAdministrator().getConfiguration().addPlugInAggregationMultiFunction(config);
        tryInvalidAggMatch(epService, "var1", "se1() @type(SupportBean)", true, "window(*)",
                "Error starting statement: Incompatible aggregation function for table 'var1' column 'value', expecting 'se1(*)' and received 'window(*)': Not a 'se1' aggregation [");
    }

    private void tryInvalidAggMatch(EPServiceProvider epService, String name, String declared, boolean unbound, String provided, String messageOrNull) {
        EPStatement stmtDeclare = epService.getEPAdministrator().createEPL("create table " + name + "(value " + declared + ")");

        try {
            String epl = "into table " + name + " select " + provided + " as value from SupportBean" +
                    (unbound ? "#time(1000)" : "");
            epService.getEPAdministrator().createEPL(epl);
            fail();
        } catch (EPStatementException ex) {
            if (messageOrNull != null && messageOrNull.length() > 10) {
                if (!ex.getMessage().startsWith(messageOrNull)) {
                    fail("\nExpected:" + messageOrNull + "\nReceived:" + ex.getMessage());
                }
            } else {
                assertTrue(ex.getMessage().contains("Incompatible aggregation function for table"));
            }
        }

        stmtDeclare.destroy();
        epService.getEPAdministrator().getConfiguration().removeEventType("table_" + name + "__internal", false);
        epService.getEPAdministrator().getConfiguration().removeEventType("table_" + name + "__public", false);
    }

    private void runAssertionInvalidAnnotations(EPServiceProvider epService) {
        // unknown marker
        SupportMessageAssertUtil.tryInvalid(epService, "create table v1 (abc window(*) @unknown)",
                "Error starting statement: For column 'abc' unrecognized annotation 'unknown' [");

        // no type provided
        SupportMessageAssertUtil.tryInvalid(epService, "create table v1 (abc window(*) @type)",
                "Error starting statement: For column 'abc' no value provided for annotation 'type', expected a value [");

        // multiple value
        SupportMessageAssertUtil.tryInvalid(epService, "create table v1 (abc window(*) @type(SupportBean) @type(SupportBean))",
                "Error starting statement: For column 'abc' multiple annotations provided named 'type' [");

        // wrong value
        SupportMessageAssertUtil.tryInvalid(epService, "create table v1 (abc window(*) @type(1))",
                "Error starting statement: For column 'abc' string value expected for annotation 'type' [");

        // unknown type provided
        SupportMessageAssertUtil.tryInvalid(epService, "create table v1 (abc window(*) @type(xx))",
                "Error starting statement: For column 'abc' failed to find event type 'xx' [");
    }

    private void runAssertionInvalid(EPServiceProvider epService) {
        epService.getEPAdministrator().getConfiguration().addPlugInSingleRowFunction("singlerow", this.getClass().getName(), "mySingleRowFunction");
        epService.getEPAdministrator().createEPL("create table aggvar_grouped_string (key string primary key, total count(*))");
        epService.getEPAdministrator().createEPL("create table aggvar_twogrouped (keyone string primary key, keytwo string primary key, total count(*))");
        epService.getEPAdministrator().createEPL("create table aggvar_grouped_int (key int primary key, total count(*))");
        epService.getEPAdministrator().createEPL("create table aggvar_ungrouped as (total count(*))");
        epService.getEPAdministrator().createEPL("create table aggvar_ungrouped_window as (win window(*) @type(SupportBean))");
        epService.getEPAdministrator().createEPL("create context MyContext initiated by SupportBean_S0 terminated by SupportBean_S1");
        epService.getEPAdministrator().createEPL("context MyContext create table aggvarctx (total count(*))");
        epService.getEPAdministrator().createEPL("create context MyOtherContext initiated by SupportBean_S0 terminated by SupportBean_S1");
        epService.getEPAdministrator().createEPL("create variable int myvariable");
        epService.getEPAdministrator().createEPL("create window MyNamedWindow#keepall as select * from SupportBean");
        epService.getEPAdministrator().createEPL("create schema SomeSchema(p0 string)");

        // invalid declaration
        //
        //
        // constant
        SupportMessageAssertUtil.tryInvalid(epService, "create constant variable aggvar_ungrouped (total count(*))",
                "Incorrect syntax near '(' expecting an identifier but found an opening parenthesis '(' at line 1 column 42 [");
        // invalid type
        SupportMessageAssertUtil.tryInvalid(epService, "create table aggvar_notright as (total sum(abc))",
                "Error starting statement: Failed to resolve type 'abc': Could not load class by name 'abc', please check imports [");
        // invalid non-aggregation
        SupportMessageAssertUtil.tryInvalid(epService, "create table aggvar_wrongtoo as (total singlerow(1))",
                "Error starting statement: Expression 'singlerow(1)' is not an aggregation [");
        // can only declare "sorted()" or "window" aggregation function
        // this is to make sure future compatibility when optimizing queries
        SupportMessageAssertUtil.tryInvalid(epService, "create table aggvar_invalid as (mywindow window(intPrimitive) @type(SupportBean))",
                "Error starting statement: Failed to validate table-column expression 'window(intPrimitive)': For tables columns, the window aggregation function requires the 'window(*)' declaration [");
        SupportMessageAssertUtil.tryInvalid(epService, "create table aggvar_invalid as (mywindow last(*)@type(SupportBean))", "skip");
        SupportMessageAssertUtil.tryInvalid(epService, "create table aggvar_invalid as (mywindow window(sb.*)@type(SupportBean)", "skip");
        SupportMessageAssertUtil.tryInvalid(epService, "create table aggvar_invalid as (mymax maxBy(intPrimitive) @type(SupportBean))",
                "Error starting statement: Failed to validate table-column expression 'maxby(intPrimitive)': For tables columns, the aggregation function requires the 'sorted(*)' declaration [");
        // same column multiple times
        SupportMessageAssertUtil.tryInvalid(epService, "create table aggvar_invalid as (mycount count(*),mycount count(*))",
                "Error starting statement: Column 'mycount' is listed more than once [create table aggvar_invalid as (mycount count(*),mycount count(*))]");
        // already a variable of the same name
        SupportMessageAssertUtil.tryInvalid(epService, "create table myvariable as (mycount count(*))",
                "Error starting statement: Variable by name 'myvariable' has already been created [");
        SupportMessageAssertUtil.tryInvalid(epService, "create table aggvar_ungrouped as (total count(*))",
                "Error starting statement: Table by name 'aggvar_ungrouped' has already been created [");
        // invalid primary key use
        SupportMessageAssertUtil.tryInvalid(epService, "create table abc as (total count(*) primary key)",
                "Error starting statement: Column 'total' may not be tagged as primary key, an expression cannot become a primary key column [");
        SupportMessageAssertUtil.tryInvalid(epService, "create table abc as (arr int[] primary key)",
                "Error starting statement: Column 'arr' may not be tagged as primary key, an array-typed column cannot become a primary key column [");
        SupportMessageAssertUtil.tryInvalid(epService, "create table abc as (arr SupportBean primary key)",
                "Error starting statement: Column 'arr' may not be tagged as primary key, received unexpected event type 'SupportBean' [");
        SupportMessageAssertUtil.tryInvalid(epService, "create table abc as (mystr string prim key)",
                "Invalid keyword 'prim' encountered, expected 'primary key' [");
        SupportMessageAssertUtil.tryInvalid(epService, "create table abc as (mystr string primary keys)",
                "Invalid keyword 'keys' encountered, expected 'primary key' [");
        SupportMessageAssertUtil.tryInvalid(epService, "create table SomeSchema as (mystr string)",
                "Error starting statement: An event type or schema by name 'SomeSchema' already exists [");

        // invalid-into
        //
        //
        // table-not-found
        SupportMessageAssertUtil.tryInvalid(epService, "into table xxx select count(*) as total from SupportBean group by intPrimitive",
                "Error starting statement: Invalid into-table clause: Failed to find table by name 'xxx' [");
        // group-by key type and count of group-by expressions
        SupportMessageAssertUtil.tryInvalid(epService, "into table aggvar_grouped_string select count(*) as total from SupportBean group by intPrimitive",
                "Error starting statement: Incompatible type returned by a group-by expression for use with table 'aggvar_grouped_string', the group-by expression 'intPrimitive' returns 'java.lang.Integer' but the table expects 'java.lang.String' [");
        SupportMessageAssertUtil.tryInvalid(epService, "into table aggvar_grouped_string select count(*) as total from SupportBean group by theString, intPrimitive",
                "Error starting statement: Incompatible number of group-by expressions for use with table 'aggvar_grouped_string', the table expects 1 group-by expressions and provided are 2 group-by expressions [");
        SupportMessageAssertUtil.tryInvalid(epService, "into table aggvar_ungrouped select count(*) as total from SupportBean group by theString",
                "Error starting statement: Incompatible number of group-by expressions for use with table 'aggvar_ungrouped', the table expects no group-by expressions and provided are 1 group-by expressions [");
        SupportMessageAssertUtil.tryInvalid(epService, "into table aggvar_grouped_string select count(*) as total from SupportBean",
                "Error starting statement: Incompatible number of group-by expressions for use with table 'aggvar_grouped_string', the table expects 1 group-by expressions and provided are no group-by expressions [");
        SupportMessageAssertUtil.tryInvalid(epService, "into table aggvarctx select count(*) as total from SupportBean",
                "Error starting statement: Table by name 'aggvarctx' has been declared for context 'MyContext' and can only be used within the same context [into table aggvarctx select count(*) as total from SupportBean]");
        SupportMessageAssertUtil.tryInvalid(epService, "context MyOtherContext into table aggvarctx select count(*) as total from SupportBean",
                "Error starting statement: Table by name 'aggvarctx' has been declared for context 'MyContext' and can only be used within the same context [context MyOtherContext into table aggvarctx select count(*) as total from SupportBean]");
        SupportMessageAssertUtil.tryInvalid(epService, "into table aggvar_ungrouped select count(*) as total, aggvar_ungrouped from SupportBean",
                "Error starting statement: Invalid use of table 'aggvar_ungrouped', aggregate-into requires write-only, the expression 'aggvar_ungrouped' is not allowed [into table aggvar_ungrouped select count(*) as total, aggvar_ungrouped from SupportBean]");
        // unidirectional join not supported
        SupportMessageAssertUtil.tryInvalid(epService, "into table aggvar_ungrouped select count(*) as total from SupportBean unidirectional, SupportBean_S0#keepall",
                "Error starting statement: Into-table does not allow unidirectional joins [");
        // requires aggregation
        SupportMessageAssertUtil.tryInvalid(epService, "into table aggvar_ungrouped select * from SupportBean",
                "Error starting statement: Into-table requires at least one aggregation function [");

        // invalid consumption
        //

        // invalid access keys type and count
        SupportMessageAssertUtil.tryInvalid(epService, "select aggvar_ungrouped['a'].total from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'aggvar_ungrouped[\"a\"].total': Incompatible number of key expressions for use with table 'aggvar_ungrouped', the table expects no key expressions and provided are 1 key expressions [select aggvar_ungrouped['a'].total from SupportBean]");
        SupportMessageAssertUtil.tryInvalid(epService, "select aggvar_grouped_string.total from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'aggvar_grouped_string.total': Failed to resolve property 'aggvar_grouped_string.total' to a stream or nested property in a stream [");
        SupportMessageAssertUtil.tryInvalid(epService, "select aggvar_grouped_string[5].total from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'aggvar_grouped_string[5].total': Incompatible type returned by a key expression for use with table 'aggvar_grouped_string', the key expression '5' returns 'java.lang.Integer' but the table expects 'java.lang.String' [select aggvar_grouped_string[5].total from SupportBean]");
        // top-level variable use without "keys" function
        SupportMessageAssertUtil.tryInvalid(epService, "select aggvar_grouped_string.something() from SupportBean",
                "Invalid use of variable 'aggvar_grouped_string', unrecognized use of function 'something', expected 'keys()' [");
        SupportMessageAssertUtil.tryInvalid(epService, "select dummy['a'] from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'dummy[\"a\"]': A table 'dummy' could not be found [select dummy['a'] from SupportBean]");
        SupportMessageAssertUtil.tryInvalid(epService, "select aggvarctx.dummy from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'aggvarctx.dummy': A column 'dummy' could not be found for table 'aggvarctx' [select aggvarctx.dummy from SupportBean]");
        SupportMessageAssertUtil.tryInvalid(epService, "select aggvarctx_ungrouped_window.win.dummy(123) from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'aggvarctx_ungrouped_window.win.dumm...(41 chars)': Failed to resolve 'aggvarctx_ungrouped_window.win.dummy' to a property, single-row function, aggregation function, script, stream or class name [select aggvarctx_ungrouped_window.win.dummy(123) from SupportBean]");
        SupportMessageAssertUtil.tryInvalid(epService, "context MyOtherContext select aggvarctx.total from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'aggvarctx.total': Table by name 'aggvarctx' has been declared for context 'MyContext' and can only be used within the same context [context MyOtherContext select aggvarctx.total from SupportBean]");
        SupportMessageAssertUtil.tryInvalid(epService, "context MyOtherContext select aggvarctx.total from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'aggvarctx.total': Table by name 'aggvarctx' has been declared for context 'MyContext' and can only be used within the same context [context MyOtherContext select aggvarctx.total from SupportBean]");
        SupportMessageAssertUtil.tryInvalid(epService, "select aggvar_grouped_int[0].a.b from SupportBean",
                "Invalid table expression 'aggvar_grouped_int[0].a.b [select aggvar_grouped_int[0].a.b from SupportBean]");
        // invalid use in non-contextual evaluation
        SupportMessageAssertUtil.tryInvalid(epService, "select * from SupportBean#time(aggvar_ungrouped.total sec)",
                "Error starting statement: Error in view 'time', Invalid parameter expression 0 for Time view: Failed to validate view parameter expression 'aggvar_ungrouped.total seconds': Invalid use of table access expression, expression 'aggvar_ungrouped' is not allowed here [select * from SupportBean#time(aggvar_ungrouped.total sec)]");
        // indexed property expression but not an aggregtion-type variable
        epService.getEPAdministrator().createEPL("create objectarray schema MyEvent(abc int[])");
        SupportMessageAssertUtil.tryInvalid(epService, "select abc[5*5] from MyEvent",
                "Error starting statement: Failed to validate select-clause expression 'abc[5*5]': A table 'abc' could not be found [select abc[5*5] from MyEvent]");
        // view use
        SupportMessageAssertUtil.tryInvalid(epService, "select * from aggvar_grouped_string#time(30)",
                "Views are not supported with tables");
        SupportMessageAssertUtil.tryInvalid(epService, "select (select * from aggvar_ungrouped#keepall) from SupportBean",
                "Views are not supported with tables [");
        // contained use
        SupportMessageAssertUtil.tryInvalid(epService, "select * from aggvar_grouped_string[books]",
                "Contained-event expressions are not supported with tables");
        // join invalid
        SupportMessageAssertUtil.tryInvalid(epService, "select aggvar_grouped_int[1].total.countMinSketchFrequency(theString) from SupportBean",
                "Error starting statement: Failed to validate select-clause expression 'aggvar_grouped_int[1].total.countMi...(62 chars)': Invalid combination of aggregation state and aggregation accessor [");
        SupportMessageAssertUtil.tryInvalid(epService, "select total.countMinSketchFrequency(theString) from aggvar_grouped_int, SupportBean unidirectional",
                "Error starting statement: Failed to validate select-clause expression 'total.countMinSketchFrequency(theString)': Failed to validate method-chain expression 'total.countMinSketchFrequency(theString)': Invalid combination of aggregation state and aggregation accessor [");
        // cannot be marked undirectional
        SupportMessageAssertUtil.tryInvalid(epService, "select * from aggvar_grouped_int unidirectional, SupportBean",
                "Error starting statement: Tables cannot be marked as unidirectional [");
        // cannot be marked with retain
        SupportMessageAssertUtil.tryInvalid(epService, "select * from aggvar_grouped_int retain-union",
                "Error starting statement: Tables cannot be marked with retain [");
        // cannot be used in on-action
        SupportMessageAssertUtil.tryInvalid(epService, "on aggvar_ungrouped select * from aggvar_ungrouped",
                "Error starting statement: Tables cannot be used in an on-action statement triggering stream [");
        // cannot be used in match-recognize
        SupportMessageAssertUtil.tryInvalid(epService, "select * from aggvar_ungrouped " +
                        "match_recognize ( measures a.theString as a pattern (A) define A as true)",
                "Error starting statement: Tables cannot be used with match-recognize [");
        // cannot be used in update-istream
        SupportMessageAssertUtil.tryInvalid(epService, "update istream aggvar_grouped_string set key = 'a'",
                "Error starting statement: Tables cannot be used in an update-istream statement [");
        // cannot be used in create-context
        SupportMessageAssertUtil.tryInvalid(epService, "create context InvalidCtx as start aggvar_ungrouped end after 5 seconds",
                "Error starting statement: Tables cannot be used in a context declaration [");
        // cannot be used in patterns
        SupportMessageAssertUtil.tryInvalid(epService, "select * from pattern[aggvar_ungrouped]",
                "Tables cannot be used in pattern filter atoms [");
        // schema by the same name
        SupportMessageAssertUtil.tryInvalid(epService, "create schema aggvar_ungrouped as " + SupportBean.class.getName(),
                "Error starting statement: A table by name 'aggvar_ungrouped' already exists [");
        try {
            epService.getEPAdministrator().getConfiguration().addEventType("aggvar_ungrouped", "p0".split(","), new Object[]{int.class});
            fail();
        } catch (EPException ex) {
            SupportMessageAssertUtil.assertMessage(ex, "A table by name 'aggvar_ungrouped' already exists");
        }
    }

    public static String mySingleRowFunction(Integer value) {
        return null;
    }

    public static class MyAggregationFunctionFactory implements AggregationFunctionFactory {

        public void setFunctionName(String functionName) {
        }

        public void validate(AggregationValidationContext validationContext) {

        }

        public AggregationMethod newAggregator() {
            return null;
        }

        public Class getValueType() {
            return null;
        }
    }
}
