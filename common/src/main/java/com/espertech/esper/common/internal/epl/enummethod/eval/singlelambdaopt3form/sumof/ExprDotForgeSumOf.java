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
package com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.sumof;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionRef;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.epl.enummethod.dot.EnumMethodEnum;
import com.espertech.esper.common.internal.epl.enummethod.dot.ExprDotEvalParamLambda;
import com.espertech.esper.common.internal.epl.enummethod.eval.singlelambdaopt3form.base.*;
import com.espertech.esper.common.internal.rettype.EPType;
import com.espertech.esper.common.internal.rettype.EPTypeHelper;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.function.Function;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ExprDotForgeSumOf extends ExprDotForgeLambdaThreeForm {

    private ExprDotEvalSumMethodFactory aggMethodFactory;

    protected EPType initAndNoParamsReturnType(EventType inputEventType, Class collectionComponentType) {
        aggMethodFactory = getAggregatorFactory(collectionComponentType);
        return EPTypeHelper.singleValue(JavaClassHelper.getBoxedType(aggMethodFactory.getValueType()));
    }

    protected ThreeFormNoParamFactory.ForgeFunction noParamsForge(EnumMethodEnum enumMethod, EPType type, StatementCompileTimeServices services) {
        return streamCountIncoming -> new EnumSumScalarNoParams(streamCountIncoming, aggMethodFactory);
    }

    protected Function<ExprDotEvalParamLambda, EPType> initAndSingleParamReturnType(EventType inputEventType, Class collectionComponentType) {
        return lambda -> {
            aggMethodFactory = getAggregatorFactory(lambda.getBodyForge().getEvaluationType());
            Class returnType = JavaClassHelper.getBoxedType(aggMethodFactory.getValueType());
            return EPTypeHelper.singleValue(returnType);
        };
    }

    protected ThreeFormEventPlainFactory.ForgeFunction singleParamEventPlain(EnumMethodEnum enumMethod) {
        return (lambda, typeInfo, services) -> new EnumSumEvent(lambda, aggMethodFactory);
    }

    protected ThreeFormEventPlusFactory.ForgeFunction singleParamEventPlus(EnumMethodEnum enumMethod) {
        return (lambda, fieldType, numParameters, typeInfo, services) -> new EnumSumEventPlus(lambda, fieldType, numParameters, aggMethodFactory);
    }

    protected ThreeFormScalarFactory.ForgeFunction singleParamScalar(EnumMethodEnum enumMethod) {
        return (lambda, eventType, numParams, typeInfo, services) -> new EnumSumScalar(lambda, eventType, numParams, aggMethodFactory);
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
            block.incrementRef("cnt");
            block.assignCompound("sum", "+", value);
        }

        public void codegenEnterObjectTypedNonNull(CodegenBlock block, CodegenExpressionRef value) {
            block.incrementRef("cnt");
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
            block.incrementRef("cnt")
                    .assignRef("sum", exprDotMethod(ref("sum"), "add", value));
        }

        public void codegenEnterObjectTypedNonNull(CodegenBlock block, CodegenExpressionRef value) {
            block.incrementRef("cnt")
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
            block.incrementRef("cnt")
                    .assignRef("sum", exprDotMethod(ref("sum"), "add", value));
        }

        public void codegenEnterObjectTypedNonNull(CodegenBlock block, CodegenExpressionRef value) {
            block.incrementRef("cnt")
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
            block.incrementRef("cnt");
            block.assignCompound("sum", "+", value);
        }

        public void codegenEnterObjectTypedNonNull(CodegenBlock block, CodegenExpressionRef value) {
            block.incrementRef("cnt");
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
            block.incrementRef("cnt");
            block.assignCompound("sum", "+", value);
        }

        public void codegenEnterObjectTypedNonNull(CodegenBlock block, CodegenExpressionRef value) {
            block.incrementRef("cnt");
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
