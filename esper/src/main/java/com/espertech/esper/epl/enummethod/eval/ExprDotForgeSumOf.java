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
package com.espertech.esper.epl.enummethod.eval;

import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.model.expression.CodegenExpressionRef;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.streamtype.StreamTypeService;
import com.espertech.esper.epl.enummethod.dot.ExprDotEvalParam;
import com.espertech.esper.epl.enummethod.dot.ExprDotEvalParamLambda;
import com.espertech.esper.epl.enummethod.dot.ExprDotForgeEnumMethodBase;
import com.espertech.esper.epl.expression.dot.ExprDotNodeUtility;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.arr.ObjectArrayEventType;
import com.espertech.esper.util.JavaClassHelper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class ExprDotForgeSumOf extends ExprDotForgeEnumMethodBase {

    public EventType[] getAddStreamTypes(String enumMethodUsedName, List<String> goesToNames, EventType inputEventType, Class collectionComponentType, List<ExprDotEvalParam> bodiesAndParameters, EventAdapterService eventAdapterService) {
        return ExprDotNodeUtility.getSingleLambdaParamEventType(enumMethodUsedName, goesToNames, inputEventType, collectionComponentType, eventAdapterService);
    }

    public EnumForge getEnumForge(EngineImportService engineImportService, EventAdapterService eventAdapterService, StreamTypeService streamTypeService, int statementId, String enumMethodUsedName, List<ExprDotEvalParam> bodiesAndParameters, EventType inputEventType, Class collectionComponentType, int numStreamsIncoming, boolean disablePropertyExpressionEventCollCache) {

        if (bodiesAndParameters.isEmpty()) {
            ExprDotEvalSumMethodFactory aggMethodFactory = getAggregatorFactory(collectionComponentType);
            super.setTypeInfo(EPTypeHelper.singleValue(JavaClassHelper.getBoxedType(aggMethodFactory.getValueType())));
            return new EnumSumScalarForge(numStreamsIncoming, aggMethodFactory);
        }

        ExprDotEvalParamLambda first = (ExprDotEvalParamLambda) bodiesAndParameters.get(0);
        ExprDotEvalSumMethodFactory aggMethodFactory = getAggregatorFactory(first.getBodyForge().getEvaluationType());
        Class returnType = JavaClassHelper.getBoxedType(aggMethodFactory.getValueType());
        super.setTypeInfo(EPTypeHelper.singleValue(returnType));
        if (inputEventType == null) {
            return new EnumSumScalarLambdaForge(first.getBodyForge(), first.getStreamCountIncoming(), aggMethodFactory,
                    (ObjectArrayEventType) first.getGoesToTypes()[0]);
        }
        return new EnumSumEventsForge(first.getBodyForge(), first.getStreamCountIncoming(), aggMethodFactory);
    }

    private static ExprDotEvalSumMethodFactory getAggregatorFactory(Class evalType) {
        if (JavaClassHelper.isFloatingPointClass(evalType)) {
            return ExprDotEvalSumMethodFactoryDouble.INSTANCE;
        } else if (evalType == BigDecimal.class) {
            return ExprDotEvalSumMethodFactoryBigDecimal.INSTANCE;
        } else if (evalType == BigInteger.class) {
            return ExprDotEvalSumMethodFactoryBigInteger.INSTANCE;
        } else if (JavaClassHelper.getBoxedType(evalType) == Long.class) {
            return ExprDotEvalSumMethodFactoryLong.INSTANCE;
        } else {
            return ExprDotEvalSumMethodFactoryInteger.INSTANCE;
        }
    }

    private static class ExprDotEvalSumMethodFactoryDouble implements ExprDotEvalSumMethodFactory {

        private final static ExprDotEvalSumMethodFactoryDouble INSTANCE = new ExprDotEvalSumMethodFactoryDouble();

        private ExprDotEvalSumMethodFactoryDouble() {
        }

        public ExprDotEvalSumMethod getSumAggregator() {
            return new ExprDotEvalSumMethodDouble();
        }

        public Class getValueType() {
            return Double.class;
        }

        public void codegenDeclare(CodegenBlock block) {
            block.declareVar(double.class, "sum", constant(0));
            block.declareVar(long.class, "cnt", constant(0));
        }

        public void codegenEnterNumberTypedNonNull(CodegenBlock block, CodegenExpressionRef value) {
            block.increment("cnt");
            block.assignCompound("sum", "+", value);
        }

        public void codegenEnterObjectTypedNonNull(CodegenBlock block, CodegenExpressionRef value) {
            block.increment("cnt");
            block.assignCompound("sum", "+", cast(Double.class, value));
        }

        public void codegenReturn(CodegenBlock block) {
            codegenReturnSumOrNull(block);
        }
    }

    private static class ExprDotEvalSumMethodDouble implements ExprDotEvalSumMethod {
        protected double sum;
        protected long cnt;

        public void enter(Object object) {
            if (object == null) {
                return;
            }
            cnt++;
            sum += (Double) object;
        }

        public Object getValue() {
            if (cnt == 0) {
                return null;
            }
            return sum;
        }
    }

    private static class ExprDotEvalSumMethodFactoryBigDecimal implements ExprDotEvalSumMethodFactory {

        private final static ExprDotEvalSumMethodFactoryBigDecimal INSTANCE = new ExprDotEvalSumMethodFactoryBigDecimal();

        private ExprDotEvalSumMethodFactoryBigDecimal() {
        }

        public ExprDotEvalSumMethod getSumAggregator() {
            return new ExprDotEvalSumMethodBigDecimal();
        }

        public Class getValueType() {
            return BigDecimal.class;
        }

        public void codegenDeclare(CodegenBlock block) {
            block.declareVar(BigDecimal.class, "sum", newInstance(BigDecimal.class, constant(0d)));
            block.declareVar(long.class, "cnt", constant(0));
        }

        public void codegenEnterNumberTypedNonNull(CodegenBlock block, CodegenExpressionRef value) {
            block.increment("cnt")
                    .assignRef("sum", exprDotMethod(ref("sum"), "add", value));
        }

        public void codegenEnterObjectTypedNonNull(CodegenBlock block, CodegenExpressionRef value) {
            block.increment("cnt")
                    .assignRef("sum", exprDotMethod(ref("sum"), "add", cast(BigDecimal.class, value)));
        }

        public void codegenReturn(CodegenBlock block) {
            codegenReturnSumOrNull(block);
        }
    }

    private static class ExprDotEvalSumMethodBigDecimal implements ExprDotEvalSumMethod {
        protected BigDecimal sum;
        protected long cnt;

        public ExprDotEvalSumMethodBigDecimal() {
            sum = new BigDecimal(0.0);
        }

        public void enter(Object object) {
            if (object == null) {
                return;
            }
            cnt++;
            sum = sum.add((BigDecimal) object);
        }

        public Object getValue() {
            if (cnt == 0) {
                return null;
            }
            return sum;
        }
    }

    private static class ExprDotEvalSumMethodFactoryBigInteger implements ExprDotEvalSumMethodFactory {

        private final static ExprDotEvalSumMethodFactoryBigInteger INSTANCE = new ExprDotEvalSumMethodFactoryBigInteger();

        private ExprDotEvalSumMethodFactoryBigInteger() {
        }

        public ExprDotEvalSumMethod getSumAggregator() {
            return new ExprDotEvalSumMethodBigInteger();
        }

        public Class getValueType() {
            return BigInteger.class;
        }

        public void codegenDeclare(CodegenBlock block) {
            block.declareVar(BigInteger.class, "sum", staticMethod(BigInteger.class, "valueOf", constant(0)));
            block.declareVar(long.class, "cnt", constant(0));
        }

        public void codegenEnterNumberTypedNonNull(CodegenBlock block, CodegenExpressionRef value) {
            block.increment("cnt")
                    .assignRef("sum", exprDotMethod(ref("sum"), "add", value));
        }

        public void codegenEnterObjectTypedNonNull(CodegenBlock block, CodegenExpressionRef value) {
            block.increment("cnt")
                    .assignRef("sum", exprDotMethod(ref("sum"), "add", cast(BigInteger.class, value)));
        }

        public void codegenReturn(CodegenBlock block) {
            codegenReturnSumOrNull(block);
        }
    }

    private static class ExprDotEvalSumMethodBigInteger implements ExprDotEvalSumMethod {
        protected BigInteger sum;
        protected long cnt;

        public ExprDotEvalSumMethodBigInteger() {
            sum = BigInteger.valueOf(0);
        }

        public void enter(Object object) {
            if (object == null) {
                return;
            }
            cnt++;
            sum = sum.add((BigInteger) object);
        }

        public Object getValue() {
            if (cnt == 0) {
                return null;
            }
            return sum;
        }
    }

    private static class ExprDotEvalSumMethodFactoryLong implements ExprDotEvalSumMethodFactory {

        private final static ExprDotEvalSumMethodFactoryLong INSTANCE = new ExprDotEvalSumMethodFactoryLong();

        private ExprDotEvalSumMethodFactoryLong() {
        }

        public ExprDotEvalSumMethod getSumAggregator() {
            return new ExprDotEvalSumMethodLong();
        }

        public Class getValueType() {
            return Long.class;
        }

        public void codegenDeclare(CodegenBlock block) {
            block.declareVar(long.class, "sum", constant(0));
            block.declareVar(long.class, "cnt", constant(0));
        }

        public void codegenEnterNumberTypedNonNull(CodegenBlock block, CodegenExpressionRef value) {
            block.increment("cnt");
            block.assignCompound("sum", "+", value);
        }

        public void codegenEnterObjectTypedNonNull(CodegenBlock block, CodegenExpressionRef value) {
            block.increment("cnt");
            block.assignCompound("sum", "+", cast(Long.class, value));
        }

        public void codegenReturn(CodegenBlock block) {
            codegenReturnSumOrNull(block);
        }
    }

    private static class ExprDotEvalSumMethodLong implements ExprDotEvalSumMethod {
        protected long sum;
        protected long cnt;

        public void enter(Object object) {
            if (object == null) {
                return;
            }
            cnt++;
            sum += (Long) object;
        }

        public Object getValue() {
            if (cnt == 0) {
                return null;
            }
            return sum;
        }
    }

    private static class ExprDotEvalSumMethodFactoryInteger implements ExprDotEvalSumMethodFactory {

        private final static ExprDotEvalSumMethodFactoryInteger INSTANCE = new ExprDotEvalSumMethodFactoryInteger();

        private ExprDotEvalSumMethodFactoryInteger() {
        }

        public ExprDotEvalSumMethod getSumAggregator() {
            return new ExprDotEvalSumMethodInteger();
        }

        public Class getValueType() {
            return Integer.class;
        }

        public void codegenDeclare(CodegenBlock block) {
            block.declareVar(int.class, "sum", constant(0));
            block.declareVar(long.class, "cnt", constant(0));
        }

        public void codegenEnterNumberTypedNonNull(CodegenBlock block, CodegenExpressionRef value) {
            block.increment("cnt");
            block.assignCompound("sum", "+", value);
        }

        public void codegenEnterObjectTypedNonNull(CodegenBlock block, CodegenExpressionRef value) {
            block.increment("cnt");
            block.assignCompound("sum", "+", cast(Integer.class, value));
        }

        public void codegenReturn(CodegenBlock block) {
            codegenReturnSumOrNull(block);
        }
    }

    private static class ExprDotEvalSumMethodInteger implements ExprDotEvalSumMethod {
        protected int sum;
        protected long cnt;

        public void enter(Object object) {
            if (object == null) {
                return;
            }
            cnt++;
            sum += (Integer) object;
        }

        public Object getValue() {
            if (cnt == 0) {
                return null;
            }
            return sum;
        }
    }

    private static void codegenReturnSumOrNull(CodegenBlock block) {
        block.ifCondition(equalsIdentity(ref("cnt"), constant(0)))
                .blockReturn(constantNull())
                .methodReturn(ref("sum"));
    }
}
