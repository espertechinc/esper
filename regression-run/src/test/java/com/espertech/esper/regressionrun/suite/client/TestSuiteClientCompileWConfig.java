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
package com.espertech.esper.regressionrun.suite.client;

import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompilerPlugInAggregationMultiFunction;
import com.espertech.esper.common.client.util.ClassForNameProvider;
import com.espertech.esper.common.client.util.ClassForNameProviderDefault;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil;
import com.espertech.esper.common.internal.support.SupportBean;
import com.espertech.esper.regressionlib.support.client.SupportSingleRowFunction;
import com.espertech.esper.regressionlib.support.extend.aggfunc.SupportConcatWCodegenAggregationFunctionForge;
import com.espertech.esper.regressionlib.support.extend.aggmultifunc.SupportAggMFMultiRTForge;
import com.espertech.esper.regressionrun.runner.SupportConfigFactory;
import junit.framework.TestCase;

import java.util.function.Consumer;

import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidConfigurationCompiler;

public class TestSuiteClientCompileWConfig extends TestCase {

    public void testClientCompileClassForNameProvider() {
        Configuration config = SupportConfigFactory.getConfiguration();
        config.getCommon().addEventType(SupportBean.class);
        config.getCommon().getTransientConfiguration().put(ClassForNameProvider.NAME, new MyClassForNameProvider());

        String epl = "select java.lang.System.exit(-1) from SupportBean";
        tryInvalidCompileWConfig(config, epl,
            "Failed to validate select-clause expression 'java.lang.System.exit(-1)': Failed to resolve 'java.lang.System.exit' to");

        config.getCommon().getTransientConfiguration().put(ClassForNameProvider.NAME, ClassForNameProviderDefault.INSTANCE);
    }

    public void testClientCompileInvalidSingleRowFunc() {
        tryInvalidPlugInSingleRow("a b", "MyClass", "some", "Failed compiler startup: Error configuring compiler: Invalid single-row name 'a b'");
        tryInvalidPlugInSingleRow("abc", "My Class", "other s", "Failed compiler startup: Error configuring compiler: Invalid class name for aggregation 'My Class'");

        Consumer<Configuration> configurer = config -> {
            config.getCompiler().addPlugInSingleRowFunction("concatstring", SupportSingleRowFunction.class.getName(), "xyz");
            config.getCompiler().addPlugInAggregationFunctionForge("concatstring", SupportConcatWCodegenAggregationFunctionForge.class.getName());
        };
        tryInvalidConfigurationCompiler(SupportConfigFactory.getConfiguration(), configurer, "Failed compiler startup: Error configuring compiler: Aggregation function by name 'concatstring' is already defined");

        configurer = config -> {
            config.getCompiler().addPlugInAggregationFunctionForge("teststring", SupportConcatWCodegenAggregationFunctionForge.class.getName());
            config.getCompiler().addPlugInSingleRowFunction("teststring", SupportSingleRowFunction.class.getName(), "xyz");
        };
        tryInvalidConfigurationCompiler(SupportConfigFactory.getConfiguration(), configurer, "Failed compiler startup: Error configuring compiler: Aggregation function by name 'teststring' is already defined");
    }

    public void testClientCompileInvalidConfigAggFuncs() {
        tryInvalidCompileConfigureAggFunc("a b", "MyClass", "Failed compiler startup: Error configuring compiler: Invalid aggregation function name 'a b'");
        tryInvalidCompileConfigureAggFunc("abc", "My Class", "Failed compiler startup: Error configuring compiler: Invalid class name for aggregation function forge 'My Class'");

        Consumer<Configuration> configurer = config -> {
            config.getCompiler().addPlugInAggregationFunctionForge("abc", SupportConcatWCodegenAggregationFunctionForge.class.getName());
            config.getCompiler().addPlugInAggregationFunctionForge("abc", SupportConcatWCodegenAggregationFunctionForge.class.getName());
        };
        tryInvalidConfigurationCompiler(SupportConfigFactory.getConfiguration(), configurer, "Failed compiler startup: Error configuring compiler: Aggregation function by name 'abc' is already defined");
    }

    public void testClientCompileInvalidConfigAggMultiFunc() {
        Consumer<Configuration> configurer;

        configurer = config -> {
            config.getCompiler().addPlugInAggregationFunctionForge("abc", SupportConcatWCodegenAggregationFunctionForge.class.getName());
            ConfigurationCompilerPlugInAggregationMultiFunction func = new ConfigurationCompilerPlugInAggregationMultiFunction("abc".split(","), SupportAggMFMultiRTForge.class.getName());
            config.getCompiler().addPlugInAggregationMultiFunction(func);
        };
        tryInvalidConfigurationCompiler(SupportConfigFactory.getConfiguration(), configurer, "Failed compiler startup: Error configuring compiler: Aggregation function by name 'abc' is already defined");

        configurer = config -> {
            ConfigurationCompilerPlugInAggregationMultiFunction funcOne = new ConfigurationCompilerPlugInAggregationMultiFunction("abc,def".split(","), SupportAggMFMultiRTForge.class.getName());
            config.getCompiler().addPlugInAggregationMultiFunction(funcOne);
            ConfigurationCompilerPlugInAggregationMultiFunction funcTwo = new ConfigurationCompilerPlugInAggregationMultiFunction("def,xyz".split(","), SupportAggMFMultiRTForge.class.getName());
            config.getCompiler().addPlugInAggregationMultiFunction(funcTwo);
        };
        tryInvalidConfigurationCompiler(SupportConfigFactory.getConfiguration(), configurer, "Failed compiler startup: Error configuring compiler: Aggregation multi-function by name 'def' is already defined");

        configurer = config -> {
            ConfigurationCompilerPlugInAggregationMultiFunction configTwo = new ConfigurationCompilerPlugInAggregationMultiFunction("thefunction2".split(","), "x y z");
            config.getCompiler().addPlugInAggregationMultiFunction(configTwo);
        };
        tryInvalidConfigurationCompiler(SupportConfigFactory.getConfiguration(), configurer, "Failed compiler startup: Error configuring compiler: Invalid class name for aggregation multi-function factory 'x y z'");
    }

    private void tryInvalidCompileConfigureAggFunc(String funcName, String className, String message) {
        Consumer<Configuration> configurer = config -> {
            config.getCompiler().addPlugInAggregationFunctionForge(funcName, className);
        };
        tryInvalidConfigurationCompiler(SupportConfigFactory.getConfiguration(), configurer, message);
    }

    private void tryInvalidPlugInSingleRow(String funcName, String className, String methodName, String expected) {
        Consumer<Configuration> configurer = config -> {
            config.getCompiler().addPlugInSingleRowFunction(funcName, className, methodName);
        };
        tryInvalidConfigurationCompiler(SupportConfigFactory.getConfiguration(), configurer, expected);
    }

    private static class MyClassForNameProvider implements ClassForNameProvider {
        public Class classForName(String className) throws ClassNotFoundException {
            if (className.equals("java.lang.System")) {
                throw new UnsupportedOperationException("Access to class '" + className + " is not permitted");
            }
            return ClassForNameProviderDefault.INSTANCE.classForName(className);
        }
    }

    private void tryInvalidCompileWConfig(Configuration config, String epl, String expected) {
        try {
            EPCompilerProvider.getCompiler().compile(epl, new CompilerArguments(config));
            fail();
        } catch (EPCompileException ex) {
            SupportMessageAssertUtil.assertMessage(ex.getMessage(), expected);
        }
    }
}
