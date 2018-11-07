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
import com.espertech.esper.common.client.scopetest.EPAssertionUtil;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpForgeInitializeContext;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOpForgeInitializeResult;
import com.espertech.esper.common.internal.epl.dataflow.interfaces.DataFlowOperatorForge;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.regressionlib.framework.RegressionEnvironment;
import com.espertech.esper.regressionlib.framework.RegressionExecution;

import java.util.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.constantNull;
import static com.espertech.esper.regressionlib.framework.SupportMessageAssertUtil.tryInvalidCompile;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

// Further relevant tests in JSONUtil/PopulateUtil
public class EPLDataflowCustomProperties {

    public static Collection<RegressionExecution> executions() {
        ArrayList<RegressionExecution> execs = new ArrayList<>();
        execs.add(new EPLDataflowInvalid());
        execs.add(new EPLDataflowCustomProps());
        execs.add(new EPLDataflowNestedProps());
        execs.add(new EPLDataflowCatchAllProps());
        return execs;
    }

    private static class EPLDataflowInvalid implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            String epl;

            epl = "create dataflow MyGraph ABC { field: { a: a}}";
            tryInvalidCompile(env, epl, "Incorrect syntax near 'a' at line 1 column 42 [");

            epl = "create dataflow MyGraph ABC { field: { a:1x b:2 }}";
            tryInvalidCompile(env, epl, "Incorrect syntax near 'x' at line 1 column 42 [");
        }
    }

    private static class EPLDataflowCatchAllProps implements RegressionExecution {
        public void run(RegressionEnvironment env) {
            MyOperatorFourForge.getOperators().clear();
            env.compile("@name('flow') create dataflow MyGraph MyOperatorFourForge {" +
                "    myTestParameter: 'abc' \n" +
                "}");
            assertEquals(1, MyOperatorFourForge.getOperators().size());
            MyOperatorFourForge instance = MyOperatorFourForge.getOperators().get(0);

            ExprNode node = instance.getAllProperties().get("myTestParameter");
            assertEquals("abc", node.getForge().getExprEvaluator().evaluate(null, true, null));
        }
    }

    private static class EPLDataflowNestedProps implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            MyOperatorThreeForge.getOperators().clear();
            env.compile("@name('flow') create dataflow MyGraph MyOperatorThree {" +
                "    settings: {\n" +
                "      class: 'EPLDataflowCustomProperties$MyOperatorThreeSettingsABC',\n" +
                "      parameterOne : 'ValueOne'" +
                "    }\n" +
                "}");
            assertEquals(1, MyOperatorThreeForge.getOperators().size());
            MyOperatorThreeForge instance = MyOperatorThreeForge.getOperators().get(0);

            MyOperatorThreeSettingsABC abc = (MyOperatorThreeSettingsABC) instance.getSettings();
            assertEquals("ValueOne", abc.parameterOne.getForge().getExprEvaluator().evaluate(null, true, null));
        }
    }

    private static class EPLDataflowCustomProps implements RegressionExecution {
        public void run(RegressionEnvironment env) {

            // test simple properties
            MyOperatorOneForge.getOperators().clear();
            env.compile("@name('flow') create dataflow MyGraph MyOperatorOne {" +
                "  theString = 'a'," +
                "  theInt: 1," +
                "  theBool: true," +
                "  theLongOne: 1L," +
                "  theLongTwo: 2," +
                "  theLongThree: null," +
                "  theDoubleOne: 1d," +
                "  theDoubleTwo: 2," +
                "  theFloatOne: 1f," +
                "  theFloatTwo: 2," +
                "  theStringWithSetter: 'b'," +
                "  theSystemProperty: systemProperties('log4j.configuration')" +
                "}");
            assertEquals(1, MyOperatorOneForge.getOperators().size());
            MyOperatorOneForge instanceOne = MyOperatorOneForge.getOperators().get(0);

            assertEquals("a", instanceOne.getTheString());
            assertEquals(null, instanceOne.getTheNotSetString());
            assertEquals(1, instanceOne.getTheInt());
            assertEquals(true, instanceOne.isTheBool());
            assertEquals(1L, (long) instanceOne.getTheLongOne());
            assertEquals(2, instanceOne.getTheLongTwo());
            assertEquals(null, instanceOne.getTheLongThree());
            assertEquals(1.0, instanceOne.getTheDoubleOne());
            assertEquals(2.0, instanceOne.getTheDoubleTwo());
            assertEquals(1f, instanceOne.getTheFloatOne());
            assertEquals(2f, instanceOne.getTheFloatTwo());
            assertEquals(">b<", instanceOne.getTheStringWithSetter());

            // test array etc. properties
            MyOperatorTwoForge.getOperators().clear();
            env.compile("@name('flow') create dataflow MyGraph MyOperatorTwo {\n" +
                "  theStringArray: ['a', \"b\"],\n" +
                "  theIntArray: [1, 2, 3],\n" +
                "  theObjectArray: ['a', 1],\n" +
                "  theMap: {\n" +
                "    a : 10,\n" +
                "    b : 'xyz'\n" +
                "  },\n" +
                "  theInnerOp: {\n" +
                "    fieldOne: 'x',\n" +
                "    fieldTwo: 2\n" +
                "  },\n" +
                "  theInnerOpInterface: {\n" +
                "    class: '" + MyOperatorTwoInterfaceImplTwo.class.getName() + "'\n" +
                "  },\n" +  // NOTE the last comma here, it's acceptable
                "}");
            assertEquals(1, MyOperatorTwoForge.getOperators().size());
            MyOperatorTwoForge instanceTwo = MyOperatorTwoForge.getOperators().get(0);

            EPAssertionUtil.assertEqualsExactOrder(new String[]{"a", "b"}, instanceTwo.getTheStringArray());
            EPAssertionUtil.assertEqualsExactOrder(new int[]{1, 2, 3}, instanceTwo.getTheIntArray());
            EPAssertionUtil.assertEqualsExactOrder(new Object[]{"a", 1}, instanceTwo.getTheObjectArray());
            EPAssertionUtil.assertPropsMap(instanceTwo.getTheMap(), "a,b".split(","), new Object[]{10, "xyz"});
            assertEquals("x", instanceTwo.getTheInnerOp().fieldOne);
            assertEquals(2, instanceTwo.getTheInnerOp().fieldTwo);
            assertTrue(instanceTwo.getTheInnerOpInterface() instanceof MyOperatorTwoInterfaceImplTwo);
        }
    }

    public static class MyOperatorOneForge implements DataFlowOperatorForge {

        private static List<MyOperatorOneForge> operators = new ArrayList<MyOperatorOneForge>();

        public static List<MyOperatorOneForge> getOperators() {
            return operators;
        }

        public MyOperatorOneForge() {
            operators.add(this);
        }

        @DataFlowOpParameter
        private String theString;
        @DataFlowOpParameter
        private String theNotSetString;
        @DataFlowOpParameter
        private int theInt;
        @DataFlowOpParameter
        private boolean theBool;
        @DataFlowOpParameter
        private Long theLongOne;
        @DataFlowOpParameter
        private long theLongTwo;
        @DataFlowOpParameter
        private Long theLongThree;
        @DataFlowOpParameter
        private double theDoubleOne;
        @DataFlowOpParameter
        private Double theDoubleTwo;
        @DataFlowOpParameter
        private float theFloatOne;
        @DataFlowOpParameter
        private Float theFloatTwo;
        @DataFlowOpParameter
        private String theSystemProperty;

        private String theStringWithSetter;

        public String getTheString() {
            return theString;
        }

        public String getTheNotSetString() {
            return theNotSetString;
        }

        public int getTheInt() {
            return theInt;
        }

        public boolean isTheBool() {
            return theBool;
        }

        public Long getTheLongOne() {
            return theLongOne;
        }

        public long getTheLongTwo() {
            return theLongTwo;
        }

        public Long getTheLongThree() {
            return theLongThree;
        }

        public float getTheFloatOne() {
            return theFloatOne;
        }

        public Float getTheFloatTwo() {
            return theFloatTwo;
        }

        public double getTheDoubleOne() {
            return theDoubleOne;
        }

        public Double getTheDoubleTwo() {
            return theDoubleTwo;
        }

        public String getTheStringWithSetter() {
            return theStringWithSetter;
        }

        public void setTheStringWithSetter(String theStringWithSetter) {
            this.theStringWithSetter = ">" + theStringWithSetter + "<";
        }

        public String getTheSystemProperty() {
            return theSystemProperty;
        }

        public DataFlowOpForgeInitializeResult initializeForge(DataFlowOpForgeInitializeContext context) throws ExprValidationException {
            return null;
        }

        public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
            return constantNull();
        }
    }

    public static class MyOperatorTwoForge implements DataFlowOperatorForge {

        private static List<MyOperatorTwoForge> operators = new ArrayList<MyOperatorTwoForge>();

        public static List<MyOperatorTwoForge> getOperators() {
            return operators;
        }

        public MyOperatorTwoForge() {
            operators.add(this);
        }

        private String[] theStringArray;
        private int[] theIntArray;
        private Object[] theObjectArray;
        private Map<String, Object> theMap;
        private MyOperatorTwoInner theInnerOp;
        private MyOperatorTwoInterface theInnerOpInterface;

        public String[] getTheStringArray() {
            return theStringArray;
        }

        public void setTheStringArray(String[] theStringArray) {
            this.theStringArray = theStringArray;
        }

        public int[] getTheIntArray() {
            return theIntArray;
        }

        public void setTheIntArray(int[] theIntArray) {
            this.theIntArray = theIntArray;
        }

        public Object[] getTheObjectArray() {
            return theObjectArray;
        }

        public void setTheObjectArray(Object[] theObjectArray) {
            this.theObjectArray = theObjectArray;
        }

        public Map<String, Object> getTheMap() {
            return theMap;
        }

        public void setTheMap(Map<String, Object> theMap) {
            this.theMap = theMap;
        }

        public MyOperatorTwoInner getTheInnerOp() {
            return theInnerOp;
        }

        public void setTheInnerOp(MyOperatorTwoInner theInnerOp) {
            this.theInnerOp = theInnerOp;
        }

        public MyOperatorTwoInterface getTheInnerOpInterface() {
            return theInnerOpInterface;
        }

        public void setTheInnerOpInterface(MyOperatorTwoInterface theInnerOpInterface) {
            this.theInnerOpInterface = theInnerOpInterface;
        }

        public DataFlowOpForgeInitializeResult initializeForge(DataFlowOpForgeInitializeContext context) throws ExprValidationException {
            return null;
        }

        public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
            return constantNull();
        }
    }

    public static class MyOperatorTwoInner {
        @DataFlowOpParameter
        private String fieldOne;
        @DataFlowOpParameter
        private int fieldTwo;
    }

    public static interface MyOperatorTwoInterface {
    }

    public static class MyOperatorTwoInterfaceImplOne implements MyOperatorTwoInterface {
    }

    public static class MyOperatorTwoInterfaceImplTwo implements MyOperatorTwoInterface {
    }

    public static class MyOperatorThreeForge implements DataFlowOperatorForge {

        @DataFlowOpParameter
        private MyOperatorThreeSettings settings;

        private static List<MyOperatorThreeForge> operators = new ArrayList<>();

        public static List<MyOperatorThreeForge> getOperators() {
            return operators;
        }

        public MyOperatorThreeForge() {
            operators.add(this);
        }

        public DataFlowOpForgeInitializeResult initializeForge(DataFlowOpForgeInitializeContext context) throws ExprValidationException {
            return null;
        }

        public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
            return constantNull();
        }

        public MyOperatorThreeSettings getSettings() {
            return settings;
        }
    }

    public interface MyOperatorThreeSettings {
    }

    public static class MyOperatorThreeSettingsABC implements MyOperatorThreeSettings {
        @DataFlowOpParameter
        private ExprNode parameterOne;

        public MyOperatorThreeSettingsABC() {
        }
    }

    public static class MyOperatorFourForge implements DataFlowOperatorForge {
        private Map<String, ExprNode> allProperties = new LinkedHashMap<>();

        @DataFlowOpParameter(all = true)
        public void setProperty(String name, ExprNode value) {
            allProperties.put(name, value);
        }

        public Map<String, ExprNode> getAllProperties() {
            return allProperties;
        }

        private static List<MyOperatorFourForge> operators = new ArrayList<>();

        public static List<MyOperatorFourForge> getOperators() {
            return operators;
        }

        public MyOperatorFourForge() {
            operators.add(this);
        }

        public DataFlowOpForgeInitializeResult initializeForge(DataFlowOpForgeInitializeContext context) throws ExprValidationException {
            return null;
        }

        public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
            return constantNull();
        }
    }
}
