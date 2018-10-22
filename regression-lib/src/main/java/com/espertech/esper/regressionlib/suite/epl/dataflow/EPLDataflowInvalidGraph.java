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
package com.espertech.esper.regressionlib.suite.epl.dataflow;

import com.espertech.esper.common.client.dataflow.annotations.DataFlowOpParameter;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.*;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;
import com.espertech.esper.regressionlib.support.bean.SupportBean_B;

import java.util.ArrayList;
import java.util.Collection;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constantNull;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.newInstance;
import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static com.espertech.esper.regressionlib.support.dataflow.SupportDataFlowAssertionUtil.tryInvalidInstantiate;

public class EPLDataflowInvalidGraph {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLDataflowInvalidCompile());
        execs.add(new EPLDataflowInvalidInstantiate());
        return execs;
    }

    private static class EPLDataflowInvalidCompile implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;

            // invalid syntax
            tryInvalidCompile(env, "create dataflow MyGraph MySource -> select",
                "Incorrect syntax near 'select' (a reserved keyword) at line 1 column 36 [");

            tryInvalidCompile(env, "create dataflow MyGraph MySource -> myout",
                "Incorrect syntax near end-of-input expecting a left curly bracket '{' but found end-of-input at line 1 column 41 [");

            // duplicate data flow name
            epl = "create dataflow MyGraph Emitter -> outstream<?> {};\n" +
                "create dataflow MyGraph Emitter -> outstream<?> {};\n";
            tryInvalidCompile(env, epl, "A dataflow by name 'MyGraph' has already been declared [");

            // type not found
            tryInvalidCompile(env, "create dataflow MyGraph DefaultSupportSourceOp -> outstream<ABC> {}",
                "Failed to find event type 'ABC'");

            // invalid schema (need not test all variants, same as create-schema)
            tryInvalidCompile(env, "create dataflow MyGraph create schema DUMMY com.mycompany.DUMMY, " +
                    "DefaultSupportSourceOp -> outstream<?> {}",
                "Could not load class by name 'com.mycompany.DUMMY', please check imports");

            // can't find op
            tryInvalidCompile(env, "create dataflow MyGraph DummyOp {}",
                "Failed to resolve forge class for operator 'DummyOp': Could not load class by name 'DummyOpForge', please check imports");

            // op is some other class
            tryInvalidCompile(env, "create dataflow MyGraph Random {}",
                "Forge class for operator 'Random' does not implement interface 'DataFlowOperatorForge'");

            // input stream not found
            tryInvalidCompile(env, "create dataflow MyGraph DefaultSupportCaptureOp(nostream) {}",
                "Input stream 'nostream' consumed by operator 'DefaultSupportCaptureOp' could not be found");

            // failed op factory
            tryInvalidCompile(env, "create dataflow MyGraph MyInvalidOp {}",
                "Failed to obtain operator 'MyInvalidOp': Failed-Here");

            // inject properties: property not found
            tryInvalidCompile(env, "create dataflow MyGraph DefaultSupportCaptureOp {dummy: 1}",
                "Failed to find writable property 'dummy' for class");

            // inject properties: property invalid type
            tryInvalidCompile(env, "create dataflow MyGraph MyTestOp {theString: 1}",
                "Property 'theString' of class " + MyTestOp.class.getName() + " expects an java.lang.String but receives a value of type java.lang.Integer");

            // two incompatible input streams: different types
            epl = "create dataflow MyGraph " +
                "DefaultSupportSourceOp -> out1<SupportBean_A> {}\n" +
                "DefaultSupportSourceOp -> out2<SupportBean_B> {}\n" +
                "MyTestOp((out1, out2) as ABC) {}";
            tryInvalidCompile(env, epl,
                "For operator 'MyTestOp' stream 'out1' typed 'SupportBean_A' is not the same type as stream 'out2' typed 'SupportBean_B'");

            // two incompatible input streams: one is wildcard
            epl = "create dataflow MyGraph " +
                "DefaultSupportSourceOp -> out1<?> {}\n" +
                "DefaultSupportSourceOp -> out2<SupportBean_B> {}\n" +
                "MyTestOp((out1, out2) as ABC) {}";
            tryInvalidCompile(env, epl,
                "For operator 'MyTestOp' streams 'out1' and 'out2' have differing wildcard type information");

            // two incompatible input streams: underlying versus eventbean
            epl = "create dataflow MyGraph " +
                "DefaultSupportSourceOp -> out1<Eventbean<SupportBean_B>> {}\n" +
                "DefaultSupportSourceOp -> out2<SupportBean_B> {}\n" +
                "MyTestOp((out1, out2) as ABC) {}";
            tryInvalidCompile(env, epl,
                "For operator 'MyTestOp' streams 'out1' and 'out2' have differing underlying information");

            // output stream multiple type parameters
            epl = "create dataflow MyGraph " +
                "DefaultSupportSourceOp -> out1<SupportBean_A, SupportBean_B> {}";
            tryInvalidCompile(env, epl,
                "Failed to validate operator 'DefaultSupportSourceOp': Multiple output types for a single stream 'out1' are not supported [");

            // same output stream declared twice
            epl = "create dataflow MyGraph " +
                "DefaultSupportSourceOp -> out1<SupportBean_A>, out1<SupportBean_B> {}";
            tryInvalidCompile(env, epl,
                "For operator 'DefaultSupportSourceOp' stream 'out1' typed 'SupportBean_A' is not the same type as stream 'out1' typed 'SupportBean_B'");

            epl = "create dataflow MyGraph " +
                "DefaultSupportSourceOp -> out1<Eventbean<SupportBean_A>>, out1<Eventbean<SupportBean_B>> {}";
            tryInvalidCompile(env, epl,
                "For operator 'DefaultSupportSourceOp' stream 'out1' typed 'SupportBean_A' is not the same type as stream 'out1' typed 'SupportBean_B'");

            // two incompatible output streams: underlying versus eventbean
            epl = "create dataflow MyGraph " +
                "DefaultSupportSourceOp -> out1<SupportBean_A> {}\n" +
                "DefaultSupportSourceOp -> out1<SupportBean_B> {}\n" +
                "MyTestOp(out1) {}";
            tryInvalidCompile(env, epl,
                "For operator 'MyTestOp' stream 'out1' typed 'SupportBean_A' is not the same type as stream 'out1' typed 'SupportBean_B'");

            // same schema defined twice
            epl = "create dataflow MyGraph " +
                "create schema ABC (c0 string), create schema ABC (c1 string), " +
                "DefaultSupportSourceOp -> out1<SupportBean_A> {}";
            tryInvalidCompile(env, epl,
                "Schema name 'ABC' is declared more then once [");
        }
    }

    private static class EPLDataflowInvalidInstantiate implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            // incompatible on-input method
            String epl = "@name('flow') create dataflow MyGraph " +
                "DefaultSupportSourceOp -> out1<SupportBean_A> {}\n" +
                "MySBInputOp(out1) {}";
            tryInvalidInstantiate(env, "MyGraph", epl,
                "Failed to instantiate data flow 'MyGraph': Failed to find onInput method on for operator 'MySBInputOp#1(out1)' class " + MySBInputOp.class.getName() + ", expected an onInput method that takes any of {Object, Object[");
        }
    }

    public static class MyInvalidOpForge implements DataFlowOperatorForge {
        public DataFlowOpForgeInitializeResult initializeForge(DataFlowOpForgeInitializeContext context) throws ExprValidationException {
            throw new RuntimeException("Failed-Here");
        }

        public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
            return constantNull();
        }
    }

    public static class MyTestOp implements DataFlowOperatorForge {

        @DataFlowOpParameter
        private String theString;

        public DataFlowOpForgeInitializeResult initializeForge(DataFlowOpForgeInitializeContext context) throws ExprValidationException {
            return null;
        }

        public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
            return constantNull();
        }
    }

    public static class MySBInputOp implements DataFlowOperatorForge, DataFlowOperatorFactory, DataFlowOperator {

        public DataFlowOpForgeInitializeResult initializeForge(DataFlowOpForgeInitializeContext context) throws ExprValidationException {
            return null;
        }

        public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
            return newInstance(MySBInputOp.class);
        }

        public void initializeFactory(DataFlowOpFactoryInitializeContext context) {
        }

        public DataFlowOperator operator(DataFlowOpInitializeContext context) {
            return new MySBInputOp();
        }

        public void onInput(SupportBean_B b) {
        }
    }
}
