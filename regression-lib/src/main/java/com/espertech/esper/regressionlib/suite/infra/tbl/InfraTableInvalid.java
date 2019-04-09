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
package com.espertech.esper.regressionlib.suite.infra.tbl;

import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import java.util.ArrayList;
import java.util.Collection;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * NOTE: More table-related tests in "nwtable"
 */
public class InfraTableInvalid {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraInvalidAggMatchSingleFunc());
        execs.add(new InfraInvalidAggMatchMultiFunc());
        execs.add(new InfraInvalidAnnotations());
        execs.add(new InfraInvalid());
        return execs;
    }

    private static class InfraInvalidAggMatchSingleFunc implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // sum
            tryInvalidAggMatch(env, "var1", "sum(double)", false, "sum(intPrimitive)",
                "Incompatible aggregation function for table 'var1' column 'value', expecting 'sum(double)' and received 'sum(intPrimitive)': The required parameter type is java.lang.Double and provided is java.lang.Integer [");
            tryInvalidAggMatch(env, "var1", "sum(double)", false, "count(*)",
                "Incompatible aggregation function for table 'var1' column 'value', expecting 'sum(double)' and received 'count(*)': The table declares 'sum(double)' and provided is 'count(*)'");
            tryInvalidAggMatch(env, "var1", "sum(double)", false, "sum(doublePrimitive, theString='a')",
                "Incompatible aggregation function for table 'var1' column 'value', expecting 'sum(double)' and received 'sum(doublePrimitive,theString=\"a\")': The aggregation declares no filter expression and provided is a filter expression [");
            tryInvalidAggMatch(env, "var1", "sum(double, boolean)", false, "sum(doublePrimitive)",
                "Incompatible aggregation function for table 'var1' column 'value', expecting 'sum(double,boolean)' and received 'sum(doublePrimitive)': The aggregation declares a filter expression and provided is no filter expression [");

            // count
            tryInvalidAggMatch(env, "var1", "count(*)", false, "sum(intPrimitive)",
                "Incompatible aggregation function for table 'var1' column 'value', expecting 'count(*)' and received 'sum(intPrimitive)': The table declares 'count(*)' and provided is 'sum(intPrimitive)'");
            tryInvalidAggMatch(env, "var1", "count(*)", false, "count(distinct intPrimitive)",
                "Incompatible aggregation function for table 'var1' column 'value', expecting 'count(*)' and received 'count(distinct intPrimitive)': The aggregation declares no distinct and provided is a distinct [");
            tryInvalidAggMatch(env, "var1", "count(*)", false, "count(distinct intPrimitive, boolPrimitive)", null);
            tryInvalidAggMatch(env, "var1", "count(distinct int)", false, "count(distinct doublePrimitive)", null);
            tryInvalidAggMatch(env, "var1", "count(int)", false, "count(*)",
                "Incompatible aggregation function for table 'var1' column 'value', expecting 'count(int)' and received 'count(*)': The aggregation declares ignore nulls and provided is no ignore nulls [");

            // avg
            tryInvalidAggMatch(env, "var1", "avg(int)", false, "sum(intPrimitive)", null);
            tryInvalidAggMatch(env, "var1", "avg(int)", false, "avg(longPrimitive)", null);
            tryInvalidAggMatch(env, "var1", "avg(int)", false, "avg(intPrimitive, boolPrimitive)", null);
            tryInvalidAggMatch(env, "var1", "avg(int)", false, "avg(distinct intPrimitive)", null);

            // min-max
            tryInvalidAggMatch(env, "var1", "max(int)", false, "min(intPrimitive)",
                "Incompatible aggregation function for table 'var1' column 'value', expecting 'max(int)' and received 'min(intPrimitive)': The aggregation declares max and provided is min [");
            tryInvalidAggMatch(env, "var1", "min(int)", false, "avg(intPrimitive)", null);
            tryInvalidAggMatch(env, "var1", "min(int)", false, "min(doublePrimitive)", null);
            tryInvalidAggMatch(env, "var1", "min(int)", false, "fmin(intPrimitive, theString='a')",
                "Incompatible aggregation function for table 'var1' column 'value', expecting 'min(int)' and received 'min(intPrimitive,theString=\"a\")': The aggregation declares no filter expression and provided is a filter expression [");

            // stddev
            tryInvalidAggMatch(env, "var1", "stddev(int)", false, "avg(intPrimitive)", null);
            tryInvalidAggMatch(env, "var1", "stddev(int)", false, "stddev(doublePrimitive)", null);
            tryInvalidAggMatch(env, "var1", "stddev(int)", false, "stddev(intPrimitive, true)", null);

            // avedev
            tryInvalidAggMatch(env, "var1", "avedev(int)", false, "avg(intPrimitive)", null);
            tryInvalidAggMatch(env, "var1", "avedev(int)", false, "avedev(doublePrimitive)", null);
            tryInvalidAggMatch(env, "var1", "avedev(int)", false, "avedev(intPrimitive, true)", null);

            // median
            tryInvalidAggMatch(env, "var1", "median(int)", false, "avg(intPrimitive)", null);
            tryInvalidAggMatch(env, "var1", "median(int)", false, "median(doublePrimitive)", null);
            tryInvalidAggMatch(env, "var1", "median(int)", false, "median(intPrimitive, true)", null);

            // firstever
            tryInvalidAggMatch(env, "var1", "firstever(int)", false, "lastever(intPrimitive)", null);
            tryInvalidAggMatch(env, "var1", "firstever(int)", false, "firstever(doublePrimitive)", null);
            tryInvalidAggMatch(env, "var1", "firstever(int, boolean)", false, "firstever(intPrimitive)", null);

            // lastever
            tryInvalidAggMatch(env, "var1", "lastever(int)", false, "firstever(intPrimitive)", null);
            tryInvalidAggMatch(env, "var1", "lastever(int)", false, "lastever(doublePrimitive)", null);
            tryInvalidAggMatch(env, "var1", "lastever(int, boolean)", false, "lastever(intPrimitive)", null);

            // countever
            tryInvalidAggMatch(env, "var1", "lastever(int)", true, "countever(intPrimitive)", null);
            tryInvalidAggMatch(env, "var1", "lastever(int, boolean)", true, "countever(intPrimitive)", null);
            tryInvalidAggMatch(env, "var1", "lastever(int)", true, "countever(intPrimitive, true)", null);
            tryInvalidAggMatch(env, "var1", "countever(*)", true, "countever(intPrimitive)", null);

            // nth
            tryInvalidAggMatch(env, "var1", "nth(int, 10)", false, "avg(20)", null);
            tryInvalidAggMatch(env, "var1", "nth(int, 10)", false, "nth(intPrimitive, 11)",
                "Incompatible aggregation function for table 'var1' column 'value', expecting 'nth(int,10)' and received 'nth(intPrimitive,11)': The size is 10 and provided is 11 [");
            tryInvalidAggMatch(env, "var1", "nth(int, 10)", false, "nth(doublePrimitive, 10)", null);

            // rate
            tryInvalidAggMatch(env, "var1", "rate(20)", false, "avg(20)", null);
            tryInvalidAggMatch(env, "var1", "rate(20)", false, "rate(11)",
                "Incompatible aggregation function for table 'var1' column 'value', expecting 'rate(20)' and received 'rate(11)': The interval-time is 20000 and provided is 11000 [");

            // leaving
            tryInvalidAggMatch(env, "var1", "leaving()", false, "avg(intPrimitive)", null);

            // plug-in single-func
            tryInvalidAggMatch(env, "var1", "myaggsingle()", false, "leaving()",
                "Incompatible aggregation function for table 'var1' column 'value', expecting 'myaggsingle(*)' and received 'leaving(*)': The table declares 'myaggsingle(*)' and provided is 'leaving(*)'");
        }
    }

    private static class InfraInvalidAggMatchMultiFunc implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // Window and related
            //
            // window vs agg method
            tryInvalidAggMatch(env, "var1", "window(*) @type(SupportBean)", true, "avg(intPrimitive)",
                "Incompatible aggregation function for table 'var1' column 'value', expecting 'window(*)' and received 'avg(intPrimitive)': The table declares 'window(*)' and provided is 'avg(intPrimitive)'");
            // window vs sorted
            tryInvalidAggMatch(env, "var1", "window(*) @type(SupportBean)", true, "sorted(intPrimitive)",
                "Failed to validate select-clause expression 'sorted(intPrimitive)': When specifying into-table a sort expression cannot be provided [");
            // wrong type
            tryInvalidAggMatch(env, "var1", "window(*) @type(SupportBean_S0)", true, "window(*)",
                "Incompatible aggregation function for table 'var1' column 'value', expecting 'window(*)' and received 'window(*)': The required event type is 'SupportBean_S0' and provided is 'SupportBean' [");

            // sorted
            //
            tryInvalidAggMatch(env, "var1", "sorted(intPrimitive) @type(SupportBean)", true, "window(*)",
                "Incompatible aggregation function for table 'var1' column 'value', expecting 'sorted(intPrimitive)' and received 'window(*)': The table declares 'sorted(intPrimitive)' and provided is 'window(*)'");
            tryInvalidAggMatch(env, "var1", "sorted(id) @type(SupportBean_S0)", true, "sorted(intPrimitive)",
                "Failed to validate select-clause expression 'sorted(intPrimitive)': When specifying into-table a sort expression cannot be provided [");

            // plug-in
            //
            tryInvalidAggMatch(env, "var1", "se1() @type(SupportBean)", true, "window(*)",
                "Incompatible aggregation function for table 'var1' column 'value', expecting 'se1(*)' and received 'window(*)': The table declares 'se1(*)' and provided is 'window(*)'");
        }
    }

    private static class InfraInvalidAnnotations implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // unknown marker
            tryInvalidCompile(env, "create table v1 (abc window(*) @unknown)",
                "For column 'abc' unrecognized annotation 'unknown' [");

            // no type provided
            tryInvalidCompile(env, "create table v1 (abc window(*) @type)",
                "For column 'abc' no value provided for annotation 'type', expected a value [");

            // multiple value
            tryInvalidCompile(env, "create table v1 (abc window(*) @type(SupportBean) @type(SupportBean))",
                "For column 'abc' multiple annotations provided named 'type' [");

            // wrong value
            tryInvalidCompile(env, "create table v1 (abc window(*) @type(1))",
                "For column 'abc' string value expected for annotation 'type' [");

            // unknown type provided
            tryInvalidCompile(env, "create table v1 (abc window(*) @type(xx))",
                "For column 'abc' failed to find event type 'xx' [");
        }
    }

    private static class InfraInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            env.compileDeploy("create table aggvar_grouped_string (key string primary key, total count(*))", path);
            env.compileDeploy("create table aggvar_twogrouped (keyone string primary key, keytwo string primary key, total count(*))", path);
            env.compileDeploy("create table aggvar_grouped_int (key int primary key, total count(*))", path);
            env.compileDeploy("create table aggvar_ungrouped as (total count(*))", path);
            env.compileDeploy("create table aggvar_ungrouped_window as (win window(*) @type(SupportBean))", path);
            env.compileDeploy("create context MyContext initiated by SupportBean_S0 terminated by SupportBean_S1", path);
            env.compileDeploy("context MyContext create table aggvarctx (total count(*))", path);
            env.compileDeploy("create context MyOtherContext initiated by SupportBean_S0 terminated by SupportBean_S1", path);
            env.compileDeploy("create variable int myvariable", path);
            env.compileDeploy("create window MyNamedWindow#keepall as select * from SupportBean", path);
            env.compileDeploy("create schema SomeSchema(p0 string)", path);

            // invalid declaration
            //
            //
            // constant
            tryInvalidCompile(env, path, "create constant variable aggvar_ungrouped (total count(*))",
                "Incorrect syntax near '(' expecting an identifier but found an opening parenthesis '(' at line 1 column 42 [");
            // invalid type
            tryInvalidCompile(env, path, "create table aggvar_notright as (total sum(abc))",
                "Failed to resolve type 'abc': Could not load class by name 'abc', please check imports [");
            // invalid non-aggregation
            tryInvalidCompile(env, path, "create table aggvar_wrongtoo as (total singlerow(1))",
                "Expression 'singlerow(1)' is not an aggregation [");
            // can only declare "sorted()" or "window" aggregation function
            // this is to make sure future compatibility when optimizing queries
            tryInvalidCompile(env, path, "create table aggvar_invalid as (mywindow window(intPrimitive) @type(SupportBean))",
                "Failed to validate table-column expression 'window(intPrimitive)': For tables columns, the window aggregation function requires the 'window(*)' declaration [");
            tryInvalidCompile(env, path, "create table aggvar_invalid as (mywindow last(*)@type(SupportBean))", "skip");
            tryInvalidCompile(env, path, "create table aggvar_invalid as (mywindow window(sb.*)@type(SupportBean)", "skip");
            tryInvalidCompile(env, path, "create table aggvar_invalid as (mymax maxBy(intPrimitive) @type(SupportBean))",
                "Failed to validate table-column expression 'maxby(intPrimitive)': For tables columns, the aggregation function requires the 'sorted(*)' declaration [");
            // same column multiple times
            tryInvalidCompile(env, path, "create table aggvar_invalid as (mycount count(*),mycount count(*))",
                "Column 'mycount' is listed more than once [create table aggvar_invalid as (mycount count(*),mycount count(*))]");
            // already a variable of the same name
            tryInvalidCompile(env, path, "create table myvariable as (mycount count(*))",
                "A variable by name 'myvariable' has already been declared [");
            tryInvalidCompile(env, path, "create table aggvar_ungrouped as (total count(*))",
                "A table by name 'aggvar_ungrouped' has already been declared [");
            // invalid primary key use
            tryInvalidCompile(env, path, "create table abc as (total count(*) primary key)",
                "Column 'total' may not be tagged as primary key, an expression cannot become a primary key column [");
            tryInvalidCompile(env, path, "create table abc as (arr SupportBean primary key)",
                "Column 'arr' may not be tagged as primary key, received unexpected event type 'SupportBean' [");
            tryInvalidCompile(env, path, "create table abc as (mystr string prim key)",
                "Invalid keyword 'prim' encountered, expected 'primary key' [");
            tryInvalidCompile(env, path, "create table abc as (mystr string primary keys)",
                "Invalid keyword 'keys' encountered, expected 'primary key' [");
            tryInvalidCompile(env, path, "create table SomeSchema as (mystr string)",
                "An event type by name 'SomeSchema' has already been declared");

            // invalid-into
            //
            //
            // table-not-found
            tryInvalidCompile(env, path, "into table xxx select count(*) as total from SupportBean group by intPrimitive",
                "Invalid into-table clause: Failed to find table by name 'xxx' [");
            // group-by key type and count of group-by expressions
            tryInvalidCompile(env, path, "into table aggvar_grouped_string select count(*) as total from SupportBean group by intPrimitive",
                "Incompatible type returned by a group-by expression for use with table 'aggvar_grouped_string', the group-by expression 'intPrimitive' returns 'java.lang.Integer' but the table expects 'java.lang.String' [");
            tryInvalidCompile(env, path, "into table aggvar_grouped_string select count(*) as total from SupportBean group by theString, intPrimitive",
                "Incompatible number of group-by expressions for use with table 'aggvar_grouped_string', the table expects 1 group-by expressions and provided are 2 group-by expressions [");
            tryInvalidCompile(env, path, "into table aggvar_ungrouped select count(*) as total from SupportBean group by theString",
                "Incompatible number of group-by expressions for use with table 'aggvar_ungrouped', the table expects no group-by expressions and provided are 1 group-by expressions [");
            tryInvalidCompile(env, path, "into table aggvar_grouped_string select count(*) as total from SupportBean",
                "Incompatible number of group-by expressions for use with table 'aggvar_grouped_string', the table expects 1 group-by expressions and provided are no group-by expressions [");
            tryInvalidCompile(env, path, "into table aggvarctx select count(*) as total from SupportBean",
                "Table by name 'aggvarctx' has been declared for context 'MyContext' and can only be used within the same context [into table aggvarctx select count(*) as total from SupportBean]");
            tryInvalidCompile(env, path, "context MyOtherContext into table aggvarctx select count(*) as total from SupportBean",
                "Table by name 'aggvarctx' has been declared for context 'MyContext' and can only be used within the same context [context MyOtherContext into table aggvarctx select count(*) as total from SupportBean]");
            tryInvalidCompile(env, path, "into table aggvar_ungrouped select count(*) as total, aggvar_ungrouped from SupportBean",
                "Invalid use of table 'aggvar_ungrouped', aggregate-into requires write-only, the expression 'aggvar_ungrouped' is not allowed [into table aggvar_ungrouped select count(*) as total, aggvar_ungrouped from SupportBean]");
            // unidirectional join not supported
            tryInvalidCompile(env, path, "into table aggvar_ungrouped select count(*) as total from SupportBean unidirectional, SupportBean_S0#keepall",
                "Into-table does not allow unidirectional joins [");
            // requires aggregation
            tryInvalidCompile(env, path, "into table aggvar_ungrouped select * from SupportBean",
                "Into-table requires at least one aggregation function [");

            // invalid consumption
            //

            // invalid access keys type and count
            tryInvalidCompile(env, path, "select aggvar_ungrouped['a'].total from SupportBean",
                "Failed to validate select-clause expression 'aggvar_ungrouped[\"a\"].total': Incompatible number of key expressions for use with table 'aggvar_ungrouped', the table expects no key expressions and provided are 1 key expressions [select aggvar_ungrouped['a'].total from SupportBean]");
            tryInvalidCompile(env, path, "select aggvar_grouped_string.total from SupportBean",
                "Failed to validate select-clause expression 'aggvar_grouped_string.total': Failed to resolve property 'aggvar_grouped_string.total' to a stream or nested property in a stream [");
            tryInvalidCompile(env, path, "select aggvar_grouped_string[5].total from SupportBean",
                "Failed to validate select-clause expression 'aggvar_grouped_string[5].total': Incompatible type returned by a key expression for use with table 'aggvar_grouped_string', the key expression '5' returns 'java.lang.Integer' but the table expects 'java.lang.String' [select aggvar_grouped_string[5].total from SupportBean]");

            // top-level variable use without "keys" function
            tryInvalidCompile(env, path, "select aggvar_grouped_string.something() from SupportBean",
                "Invalid use of table 'aggvar_grouped_string', unrecognized use of function 'something', expected 'keys()'");
            tryInvalidCompile(env, path, "select dummy['a'] from SupportBean",
                "Failed to validate select-clause expression 'dummy[\"a\"]': Failed to resolve table name 'dummy' to a table");
            tryInvalidCompile(env, path, "select aggvarctx.dummy from SupportBean",
                "Failed to validate select-clause expression 'aggvarctx.dummy': A column 'dummy' could not be found for table 'aggvarctx' [select aggvarctx.dummy from SupportBean]");
            tryInvalidCompile(env, path, "select aggvarctx_ungrouped_window.win.dummy(123) from SupportBean",
                "Failed to validate select-clause expression 'aggvarctx_ungrouped_window.win.dumm...(41 chars)': Failed to resolve 'aggvarctx_ungrouped_window.win.dummy' to a property, single-row function, aggregation function, script, stream or class name [select aggvarctx_ungrouped_window.win.dummy(123) from SupportBean]");
            tryInvalidCompile(env, path, "context MyOtherContext select aggvarctx.total from SupportBean",
                "Failed to validate select-clause expression 'aggvarctx.total': Table by name 'aggvarctx' has been declared for context 'MyContext' and can only be used within the same context [context MyOtherContext select aggvarctx.total from SupportBean]");
            tryInvalidCompile(env, path, "context MyOtherContext select aggvarctx.total from SupportBean",
                "Failed to validate select-clause expression 'aggvarctx.total': Table by name 'aggvarctx' has been declared for context 'MyContext' and can only be used within the same context [context MyOtherContext select aggvarctx.total from SupportBean]");
            tryInvalidCompile(env, path, "select aggvar_grouped_int[0].a.b from SupportBean",
                "Invalid table expression 'aggvar_grouped_int[0].a.b [select aggvar_grouped_int[0].a.b from SupportBean]");

            // invalid use in non-contextual evaluation
            tryInvalidCompile(env, path, "select * from SupportBean#time(aggvar_ungrouped.total sec)",
                "Failed to validate data window declaration: Error in view 'time', Invalid parameter expression 0 for Time view: Failed to validate view parameter expression 'aggvar_ungrouped.total seconds': Invalid use of table access expression, expression 'aggvar_ungrouped' is not allowed here");
            // indexed property expression but not an aggregtion-type variable
            env.compileDeploy("create objectarray schema MyEvent(abc int[])");
            tryInvalidCompile(env, path, "select abc[5*5] from SupportBean",
                "Failed to validate select-clause expression 'abc[5*5]': Failed to resolve table name 'abc' to a table");
            // view use
            tryInvalidCompile(env, path, "select * from aggvar_grouped_string#time(30)",
                "Views are not supported with tables");
            tryInvalidCompile(env, path, "select (select * from aggvar_ungrouped#keepall) from SupportBean",
                "Views are not supported with tables [");
            // contained use
            tryInvalidCompile(env, path, "select * from aggvar_grouped_string[books]",
                "Contained-event expressions are not supported with tables");
            // join invalid
            tryInvalidCompile(env, path, "select aggvar_grouped_int[1].total.countMinSketchFrequency(theString) from SupportBean",
                "Failed to validate select-clause expression 'aggvar_grouped_int[1].total.countMi...(62 chars)': Failed to resolve method 'countMinSketchFrequency': Could not find enumeration method, date-time method or instance method named 'countMinSketchFrequency' in class 'java.lang.Long' with matching parameter number and expected parameter type(s) 'String' ");
            tryInvalidCompile(env, path, "select total.countMinSketchFrequency(theString) from aggvar_grouped_int, SupportBean unidirectional",
                "Failed to validate select-clause expression 'total.countMinSketchFrequency(theString)': Failed to resolve method 'countMinSketchFrequency': Could not find");
            // cannot be marked undirectional
            tryInvalidCompile(env, path, "select * from aggvar_grouped_int unidirectional, SupportBean",
                "Tables cannot be marked as unidirectional [");
            // cannot be marked with retain
            tryInvalidCompile(env, path, "select * from aggvar_grouped_int retain-union",
                "Tables cannot be marked with retain [");
            // cannot be used in on-action
            tryInvalidCompile(env, path, "on aggvar_ungrouped select * from aggvar_ungrouped",
                "Tables cannot be used in an on-action statement triggering stream [");
            // cannot be used in match-recognize
            tryInvalidCompile(env, path, "select * from aggvar_ungrouped " +
                    "match_recognize ( measures a.theString as a pattern (A) define A as true)",
                "Tables cannot be used with match-recognize [");
            // cannot be used in update-istream
            tryInvalidCompile(env, path, "update istream aggvar_grouped_string set key = 'a'",
                "Tables cannot be used in an update-istream statement [");
            // cannot be used in create-context
            tryInvalidCompile(env, path, "create context InvalidCtx as start aggvar_ungrouped end after 5 seconds",
                "Tables cannot be used in a context declaration [");
            // cannot be used in patterns
            tryInvalidCompile(env, path, "select * from pattern[aggvar_ungrouped]",
                "Tables cannot be used in pattern filter atoms [");
            // schema by the same name
            tryInvalidCompile(env, path, "create schema aggvar_ungrouped as SupportBean",
                "A table by name 'aggvar_ungrouped' already exists [");

            env.undeployAll();
        }
    }

    private static void tryInvalidAggMatch(RegressionEnvironment env, String name, String declared, boolean unbound, String provided, String messageOrNull) {
        RegressionPath path = new RegressionPath();
        env.compileDeploy("@name('create') create table " + name + "(value " + declared + ")", path);

        try {
            String epl = "into table " + name + " select " + provided + " as value from SupportBean" +
                (unbound ? "#time(1000)" : "");
            env.compileWCheckedEx(epl, path);
            fail();
        } catch (EPCompileException ex) {
            if (messageOrNull != null && messageOrNull.length() > 10) {
                if (!ex.getMessage().startsWith(messageOrNull)) {
                    ex.printStackTrace();
                    fail("\nExpected:" + messageOrNull + "\nReceived:" + ex.getMessage());
                }
            } else {
                assertTrue(ex.getMessage().contains("Incompatible aggregation function for table"));
            }
        }

        env.undeployModuleContaining("create");
    }

    public static String mySingleRowFunction(Integer value) {
        return null;
    }
}
