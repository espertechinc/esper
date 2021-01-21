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
package com.espertech.esper.regressionlib.suite.client.multitenancy;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClientMultitenancyIndex {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new ClientMultitenancyIndexTable());
        return execs;
    }

    public static class ClientMultitenancyIndexTable implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            RegressionPath path = new RegressionPath();
            String epl = "module com_test_app;\n" +
                    "@public @buseventtype create schema VWAPrice(symbol string);\n" +
                    "@public create table Basket(basket_id string primary key, symbol string primary key, weight double);\n" +
                    "@public create index BasketIndex on Basket(symbol);\n";
            env.compileDeploy(epl, path);
            env.compileExecuteFAFNoResult("insert into Basket select '1' as basket_id, 'A' as symbol, 1 as weight", path);
            env.compileExecuteFAFNoResult("insert into Basket select '2' as basket_id, 'B' as symbol, 2 as weight", path);

            epl = "@name('s0') select weight from Basket as bask, VWAPrice as v where bask.symbol = v.symbol;\n";
            env.compileDeploy(epl, path).addListener("s0");

            env.sendEventMap(Collections.singletonMap("symbol", "A"), "VWAPrice");
            env.assertEqualsNew("s0", "weight", 1.0);

            env.milestone(0);

            env.sendEventMap(Collections.singletonMap("symbol", "B"), "VWAPrice");
            env.assertEqualsNew("s0", "weight", 2.0);

            env.undeployAll();
        }
    }
}
