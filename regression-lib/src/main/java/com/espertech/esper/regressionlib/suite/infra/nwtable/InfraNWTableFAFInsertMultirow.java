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
package com.espertech.esper.regressionlib.suite.infra.nwtable;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.common.internal.collection.Pair;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;
import com.espertech.esper.regressionlib.support.util.IndexBackingTableInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

public class InfraNWTableFAFInsertMultirow implements IndexBackingTableInfo {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new InfraInsertMultirow(true));
        execs.add(new InfraInsertMultirow(false));
        execs.add(new InfraInsertMultirowRollback(true));
        execs.add(new InfraInsertMultirowRollback(false));
        execs.add(new InfraInsertMultirowInvalid());
        return execs;
    }

    private static class InfraInsertMultirowRollback implements RegressionExecution {
        private final boolean namedWindow;

        public InfraInsertMultirowRollback(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String[] fields = "k,v".split(",");
            String epl = "@name('infra') @public ";
            if (namedWindow) {
                epl += "create window MyInfra#keepall as (k string, v int);\n" +
                        "create unique index Idx on MyInfra (k);\n";
            } else {
                epl += "create table MyInfra(k string primary key, v int);\n";
            }
            env.compileDeploy(epl, path);

            String query = "insert into MyInfra values ('a', 0), ('b', 10), ('b', 11), ('c', 20)";
            try {
                env.compileExecuteFAF(query, path);
                fail();
            } catch (EPException ex) {
                String indexName = namedWindow ? "Idx" : "MyInfra";
                String expected = "Unique index violation, index 'IDXNAME' is a unique index and key 'b' already exists".replace("IDXNAME", indexName);
                assertEquals(expected, ex.getMessage());
            }
            env.assertPropsPerRowIterator("infra", fields, new Object[0][]);

            env.undeployAll();
        }

        public String name() {
            return this.getClass().getSimpleName() + "{" +
                    "namedWindow=" + namedWindow +
                    '}';
        }
    }

    private static class InfraInsertMultirow implements RegressionExecution {
        private final boolean namedWindow;

        public InfraInsertMultirow(boolean namedWindow) {
            this.namedWindow = namedWindow;
        }

        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String[] fields = "k,v".split(",");
            String epl = "@name('infra') @public ";
            if (namedWindow) {
                epl += "create window MyInfra#keepall as (k string, v int);\n";
            } else {
                epl += "create table MyInfra(k string primary key, v int);\n";
            }
            epl += "@public create window LastSupportBean#lastevent as SupportBean;\n" +
                    "on SupportBean merge LastSupportBean insert select *\n";
            env.compileDeploy(epl, path);

            String query = "insert into MyInfra values ('a', 1), ('b', 2)";
            env.compileExecuteFAF(query, path);
            env.assertPropsPerRowIterator("infra", fields, new Object[][] {{"a", 1}, {"b", 2}});

            // test SODA
            query = "insert into MyInfra values (\"c\", 3), (\"d\", 4)";
            EPStatementObjectModel model = env.eplToModel(query);
            assertEquals(query, model.toEPL());
            env.compileExecuteFAF(model, path);
            env.assertPropsPerRowIterator("infra", fields, new Object[][] {{"a", 1}, {"b", 2}, {"c", 3}, {"d", 4}});

            // test subquery
            env.compileExecuteFAF("delete from MyInfra", path);
            env.sendEventBean(new SupportBean("x", 50));
            env.compileExecuteFAF("insert into MyInfra values ('a', 1), " +
                    "((select theString from LastSupportBean), (select intPrimitive from LastSupportBean))", path);
            env.assertPropsPerRowIterator("infra", fields, new Object[][] {{"a", 1}, {"x", 50}});

            // test 1000 rows
            env.compileExecuteFAF("delete from MyInfra", path);
            Pair<String, Object[][]> queryAndResult = buildQuery(1000);
            env.compileExecuteFAF(queryAndResult.getFirst(), path);
            env.assertPropsPerRowIteratorAnyOrder("infra", fields, queryAndResult.getSecond());

            env.undeployAll();
        }

        public String name() {
            return this.getClass().getSimpleName() + "{" +
                    "namedWindow=" + namedWindow +
                    '}';
        }
    }

    private static class InfraInsertMultirowInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "@name('window') @public create window MyInfra#keepall as (k string, v int);\n";
            env.compileDeploy(epl, path);

            env.tryInvalidCompileFAF(path, "insert into MyInfra (k, v) values ('a', 1), ('b')",
                    "Failed to validate multi-row insert at row 2 of 2: Number of supplied values in the select or values clause does not match insert-into clause");

            String queryMaxRows = buildQuery(1001).getFirst();
            env.tryInvalidCompileFAF(path, queryMaxRows,
                    "Insert-into number-of-rows exceeds the maximum of 1000 rows as the query provides 1001 rows");

            env.undeployAll();
        }
    }

    private static Pair<String, Object[][]> buildQuery(int size) {
        StringBuilder buf = new StringBuilder();
        buf.append("insert into MyInfra values ");
        String delimiter = "";
        List<Object[]> expected = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            buf.append(delimiter).append("('$1', $2)".replace("$1", "E" + i).replace("$2", Integer.toString(i)));
            delimiter = ",";
            expected.add(new Object[] {"E" + i, i});
        }
        return new Pair<>(buf.toString(), expected.toArray(new Object[0][]));
    }
}
