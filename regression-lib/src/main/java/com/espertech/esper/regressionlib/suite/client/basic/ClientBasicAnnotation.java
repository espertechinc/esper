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
package com.espertech.esper.regressionlib.suite.client.basic;

import com.espertech.esper.common.client.annotation.Name;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import java.lang.annotation.Annotation;

import static org.junit.Assert.assertEquals;

public class ClientBasicAnnotation implements RegressionExecution {
    public void run(RegressionEnvironment env) {
        String epl = "@name('abc') @Tag(name='a', value='b') @Priority(1) @Drop select * from SupportBean";
        env.compileDeployAddListenerMileZero(epl, "abc");

        Annotation[] annotations = env.statement("abc").getAnnotations();

        assertEquals(Name.class, annotations[0].annotationType());
        assertEquals("abc", ((Name) annotations[0]).value());

        env.undeployAll();
    }
}
