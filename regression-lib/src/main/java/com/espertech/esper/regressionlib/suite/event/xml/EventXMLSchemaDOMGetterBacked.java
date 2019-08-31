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
package com.espertech.esper.regressionlib.suite.event.xml;

import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.framework.RegressionPath;

import java.util.ArrayList;
import java.util.List;

public class EventXMLSchemaDOMGetterBacked {

    public static List<RegressionExecution> executions() {
        List<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EventXMLSchemaDOMGetterBackedPreconfig());
        execs.add(new EventXMLSchemaDOMGetterBackedCreateSchema());
        return execs;
    }

    public static class EventXMLSchemaDOMGetterBackedPreconfig implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            EventXMLSchemaXPathBacked.runAssertion(env, false, "XMLSchemaConfigTwo", new RegressionPath());
        }
    }

    public static class EventXMLSchemaDOMGetterBackedCreateSchema implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String schemaUriSimpleSchema = Thread.currentThread().getContextClassLoader().getResource("regression/simpleSchema.xsd").toString();
            String epl = "@public @buseventtype " +
                "@XMLSchema(rootElementName='simpleEvent', schemaResource='" + schemaUriSimpleSchema + "', xpathPropertyExpr=false)" +
                "@XMLSchemaNamespacePrefix(prefix='ss', namespace='samples:schemas:simpleSchema')" +
                "@XMLSchemaField(name='customProp', xpath='count(/ss:simpleEvent/ss:nested3/ss:nested4)', type='number')" +
                "create xml schema MyEventCreateSchema()";
            RegressionPath path = new RegressionPath();
            env.compileDeploy(epl, path);
            EventXMLSchemaXPathBacked.runAssertion(env, false, "MyEventCreateSchema", path);
        }
    }
}
