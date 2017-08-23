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
package com.espertech.esper.epl.core.engineimport;

import com.espertech.esper.client.ConfigurationPlugInAggregationFunction;
import com.espertech.esper.core.support.SupportEngineImportServiceFactory;
import com.espertech.esper.epl.core.engineimport.EngineImportException;
import com.espertech.esper.epl.core.engineimport.EngineImportServiceImpl;
import com.espertech.esper.epl.core.engineimport.EngineImportUndefinedException;
import com.espertech.esper.supportunit.epl.SupportPluginAggregationMethodOneFactory;
import junit.framework.TestCase;

import java.lang.reflect.Method;

public class TestEngineImportServiceImpl extends TestCase {
    EngineImportServiceImpl engineImportService;

    public void setUp() {
        this.engineImportService = SupportEngineImportServiceFactory.make();
    }

    public void testResolveMethodNoArgTypes() throws Exception {
        Method method = engineImportService.resolveMethodOverloadChecked("java.lang.Math", "cbrt");
        assertEquals(Math.class.getMethod("cbrt", new Class[]{double.class}), method);

        try {
            engineImportService.resolveMethodOverloadChecked("java.lang.Math", "abs");
            fail();
        } catch (EngineImportException ex) {
            assertEquals("Method by name 'abs' is overloaded in class 'java.lang.Math' and overloaded methods do not return the same type", ex.getMessage());
        }
    }

    public void testAddAggregation() throws EngineImportException {
        engineImportService.addAggregation("abc", new ConfigurationPlugInAggregationFunction("abc", "abcdef.G"));
        engineImportService.addAggregation("abcDefGhk", new ConfigurationPlugInAggregationFunction("abcDefGhk", "ab"));
        engineImportService.addAggregation("a", new ConfigurationPlugInAggregationFunction("a", "Yh"));

        tryInvalidAddAggregation("g h", "");
        tryInvalidAddAggregation("gh", "j j");
        tryInvalidAddAggregation("abc", "hhh");
    }

    public void testResolveAggregationMethod() throws Exception {
        engineImportService.addAggregation("abc", new ConfigurationPlugInAggregationFunction("abc", SupportPluginAggregationMethodOneFactory.class.getName()));
        assertTrue(engineImportService.resolveAggregationFactory("abc") instanceof SupportPluginAggregationMethodOneFactory);
    }

    public void testInvalidResolveAggregation(String funcName) throws Exception {
        try {
            engineImportService.resolveAggregationFactory("abc");
        } catch (EngineImportUndefinedException ex) {
            // expected
        }

        engineImportService.addAggregation("abc", new ConfigurationPlugInAggregationFunction("abc", "abcdef.G"));
        try {
            engineImportService.resolveAggregationFactory("abc");
        } catch (EngineImportException ex) {
            // expected
        }
    }

    public void testResolveClass() throws Exception {
        String className = "java.lang.Math";
        Class expected = Math.class;
        assertEquals(expected, engineImportService.resolveClassInternal(className, false, false));

        engineImportService.addImport("java.lang.Math");
        assertEquals(expected, engineImportService.resolveClassInternal(className, false, false));

        engineImportService.addImport("java.lang.*");
        className = "String";
        expected = String.class;
        assertEquals(expected, engineImportService.resolveClassInternal(className, false, false));
    }

    public void testResolveClassInvalid() {
        String className = "Math";
        try {
            engineImportService.resolveClassInternal(className, false, false);
            fail();
        } catch (ClassNotFoundException e) {
            // Expected
        }
    }

    public void testAddImport() throws EngineImportException {
        engineImportService.addImport("java.lang.Math");
        assertEquals(1, engineImportService.getImports().length);
        assertEquals("java.lang.Math", engineImportService.getImports()[0]);

        engineImportService.addImport("java.lang.*");
        assertEquals(2, engineImportService.getImports().length);
        assertEquals("java.lang.Math", engineImportService.getImports()[0]);
        assertEquals("java.lang.*", engineImportService.getImports()[1]);
    }

    public void testAddImportInvalid() {
        try {
            engineImportService.addImport("java.lang.*.*");
            fail();
        } catch (EngineImportException e) {
            // Expected
        }

        try {
            engineImportService.addImport("java.lang..Math");
            fail();
        } catch (EngineImportException e) {
            // Expected
        }
    }

    private void tryInvalidAddAggregation(String funcName, String className) {
        try {
            engineImportService.addAggregation(funcName, new ConfigurationPlugInAggregationFunction(funcName, className));
        } catch (EngineImportException ex) {
            // expected
        }
    }
}
