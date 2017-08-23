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
package com.espertech.esper.epl.script.mvel;

import com.espertech.esper.client.EPException;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.util.JavaClassHelper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class MVELInvoker {

    private static Class mvelClass;
    private static Class parserContextClass;
    private static Class execStatementClass;
    private static Method executeExpressionMethod;

    public static boolean isMVELInClasspath(EngineImportService engineImportService) {
        if (mvelClass == null) {
            init(engineImportService);
        }
        return assertClasses();
    }

    public static void analysisCompile(String expression, Object parserContext, EngineImportService engineImportService) throws InvocationTargetException {
        assertClasspath(engineImportService);
        Method method;
        try {
            method = mvelClass.getMethod("analysisCompile", new Class[]{String.class, parserContextClass});
        } catch (Exception e) {
            throw new EPException("Failed to find MVEL method: " + e.getMessage(), e);
        }

        try {
            method.invoke(null, new Object[]{expression, parserContext});
        } catch (IllegalAccessException e) {
            throw new EPException("Failed to access MVEL method: " + e.getMessage(), e);
        }
    }

    public static Object compileExpression(String expression, Object parserContext) throws InvocationTargetException {
        Method method;
        try {
            method = mvelClass.getMethod("compileExpression", new Class[]{String.class, parserContextClass});
        } catch (Exception e) {
            throw new EPException("Failed to find MVEL method: " + e.getMessage(), e);
        }

        try {
            return method.invoke(null, new Object[]{expression, parserContext});
        } catch (IllegalAccessException e) {
            throw new EPException("Failed to access MVEL method: " + e.getMessage(), e);
        }
    }

    public static Object newParserContext(EngineImportService engineImportService) {
        assertClasspath(engineImportService);

        try {
            return parserContextClass.newInstance();
        } catch (Exception e) {
            throw new EPException("Failed to instantiate MVEL ParserContext: " + e.getMessage(), e);
        }
    }

    public static Map<String, Class> getParserContextInputs(Object parserContext) {
        Method method;
        try {
            method = parserContextClass.getMethod("getInputs", new Class[0]);
            return (Map<String, Class>) method.invoke(parserContext, null);
        } catch (Exception e) {
            throw new EPException("Failed to find MVEL method: " + e.getMessage(), e);
        }
    }

    public static Object executeExpression(Object executable, Map<String, Object> parameters) throws InvocationTargetException {
        try {
            return executeExpressionMethod.invoke(null, new Object[]{executable, parameters});
        } catch (IllegalAccessException e) {
            throw new EPException("Failed to find MVEL method: " + e.getMessage(), e);
        }
    }

    public static void setParserContextStrongTyping(Object parserContext) {
        Method method;
        try {
            method = parserContextClass.getMethod("setStrongTyping", boolean.class);
            method.invoke(parserContext, true);
        } catch (Exception e) {
            throw new EPException("Failed to find MVEL method: " + e.getMessage(), e);
        }
    }

    public static void setParserContextInputs(Object parserContext, Map<String, Class> mvelInputParamTypes) {
        Method method;
        try {
            method = parserContextClass.getMethod("setInputs", Map.class);
            method.invoke(parserContext, mvelInputParamTypes);
        } catch (Exception e) {
            throw new EPException("Failed to find MVEL method: " + e.getMessage(), e);
        }
    }

    public static Class getExecutableStatementKnownReturnType(Object compiled) {
        Method method;
        try {
            method = execStatementClass.getMethod("getKnownEgressType", null);
            return (Class) method.invoke(compiled, null);
        } catch (Exception e) {
            throw new EPException("Failed to find MVEL method: " + e.getMessage(), e);
        }
    }

    private static void init(EngineImportService engineImportService) {
        mvelClass = JavaClassHelper.getClassInClasspath("org.mvel2.MVEL", engineImportService.getClassForNameProvider());
        parserContextClass = JavaClassHelper.getClassInClasspath("org.mvel2.ParserContext", engineImportService.getClassForNameProvider());
        execStatementClass = JavaClassHelper.getClassInClasspath("org.mvel2.compiler.ExecutableStatement", engineImportService.getClassForNameProvider());
        if (mvelClass != null) {
            try {
                executeExpressionMethod = mvelClass.getMethod("executeExpression", new Class[]{Object.class, Map.class});
            } catch (NoSuchMethodException e) {
                throw new EPException("Failed to find MVEL method: " + e.getMessage(), e);
            }
        }
    }

    private static void assertClasspath(EngineImportService engineImportService) {
        if (mvelClass == null) {
            init(engineImportService);
        }
        if (!assertClasses()) {
            throw new IllegalStateException("Failed to find MVEL in classpath");
        }
    }

    private static boolean assertClasses() {
        return mvelClass != null && parserContextClass != null;
    }
}
