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
import com.espertech.esper.epl.core.EngineImportService;
import com.espertech.esper.epl.core.StreamTypeService;
import com.espertech.esper.epl.enummethod.dot.ExprDotEvalEnumMethodBase;
import com.espertech.esper.epl.enummethod.dot.ExprDotEvalParam;
import com.espertech.esper.epl.enummethod.dot.ExprDotEvalParamLambda;
import com.espertech.esper.epl.expression.dot.ExprDotNodeUtility;
import com.espertech.esper.epl.rettype.EPTypeHelper;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.arr.ObjectArrayEventType;
import com.espertech.esper.util.JavaClassHelper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

public class ExprDotEvalSumOf extends ExprDotEvalEnumMethodBase {

    public EventType[] getAddStreamTypes(String enumMethodUsedName, List<String> goesToNames, EventType inputEventType, Class collectionComponentType, List<ExprDotEvalParam> bodiesAndParameters, EventAdapterService eventAdapterService) {
        return ExprDotNodeUtility.getSingleLambdaParamEventType(enumMethodUsedName, goesToNames, inputEventType, collectionComponentType, eventAdapterService);
    }

    public EnumEval getEnumEval(EngineImportService engineImportService, EventAdapterService eventAdapterService, StreamTypeService streamTypeService, int statementId, String enumMethodUsedName, List<ExprDotEvalParam> bodiesAndParameters, EventType inputEventType, Class collectionComponentType, int numStreamsIncoming, boolean disablePropertyExpressionEventCollCache) {

        if (bodiesAndParameters.isEmpty()) {
            ExprDotEvalSumMethodFactory aggMethodFactory = getAggregatorFactory(collectionComponentType);
            super.setTypeInfo(EPTypeHelper.singleValue(JavaClassHelper.getBoxedType(aggMethodFactory.getValueType())));
            return new EnumEvalSumScalar(numStreamsIncoming, aggMethodFactory);
        }

        ExprDotEvalParamLambda first = (ExprDotEvalParamLambda) bodiesAndParameters.get(0);
        ExprDotEvalSumMethodFactory aggMethodFactory = getAggregatorFactory(first.getBodyEvaluator().getType());
        Class returnType = JavaClassHelper.getBoxedType(aggMethodFactory.getValueType());
        super.setTypeInfo(EPTypeHelper.singleValue(returnType));
        if (inputEventType == null) {
            return new EnumEvalSumScalarLambda(first.getBodyEvaluator(), first.getStreamCountIncoming(), aggMethodFactory,
                    (ObjectArrayEventType) first.getGoesToTypes()[0]);
        }
        return new EnumEvalSumEvents(first.getBodyEvaluator(), first.getStreamCountIncoming(), aggMethodFactory);
    }

    private static ExprDotEvalSumMethodFactory getAggregatorFactory(Class evalType) {
        if (JavaClassHelper.isFloatingPointClass(evalType)) {
            return new ExprDotEvalSumMethodFactory() {
                public ExprDotEvalSumMethod getSumAggregator() {
                    return new ExprDotEvalSumMethodDouble();
                }

                public Class getValueType() {
                    return Double.class;
                }
            };
        } else if (evalType == BigDecimal.class) {
            return new ExprDotEvalSumMethodFactory() {
                public ExprDotEvalSumMethod getSumAggregator() {
                    return new ExprDotEvalSumMethodBigDecimal();
                }

                public Class getValueType() {
                    return BigDecimal.class;
                }
            };
        } else if (evalType == BigInteger.class) {
            return new ExprDotEvalSumMethodFactory() {
                public ExprDotEvalSumMethod getSumAggregator() {
                    return new ExprDotEvalSumMethodBigInteger();
                }

                public Class getValueType() {
                    return BigInteger.class;
                }
            };
        } else if (JavaClassHelper.getBoxedType(evalType) == Long.class) {
            return new ExprDotEvalSumMethodFactory() {
                public ExprDotEvalSumMethod getSumAggregator() {
                    return new ExprDotEvalSumMethodLong();
                }

                public Class getValueType() {
                    return Long.class;
                }
            };
        } else {
            return new ExprDotEvalSumMethodFactory() {
                public ExprDotEvalSumMethod getSumAggregator() {
                    return new ExprDotEvalSumMethodInteger();
                }

                public Class getValueType() {
                    return Integer.class;
                }
            };
        }
    }

    private static class ExprDotEvalSumMethodDouble implements ExprDotEvalSumMethod {
        protected double sum;
        protected long numDataPoints;

        public void enter(Object object) {
            if (object == null) {
                return;
            }
            numDataPoints++;
            sum += (Double) object;
        }

        public Object getValue() {
            if (numDataPoints == 0) {
                return null;
            }
            return sum;
        }
    }

    private static class ExprDotEvalSumMethodBigDecimal implements ExprDotEvalSumMethod {
        protected BigDecimal sum;
        protected long numDataPoints;

        public ExprDotEvalSumMethodBigDecimal() {
            sum = new BigDecimal(0.0);
        }

        public void enter(Object object) {
            if (object == null) {
                return;
            }
            numDataPoints++;
            sum = sum.add((BigDecimal) object);
        }

        public Object getValue() {
            if (numDataPoints == 0) {
                return null;
            }
            return sum;
        }
    }

    private static class ExprDotEvalSumMethodBigInteger implements ExprDotEvalSumMethod {
        protected BigInteger sum;
        protected long numDataPoints;

        public ExprDotEvalSumMethodBigInteger() {
            sum = BigInteger.valueOf(0);
        }

        public void enter(Object object) {
            if (object == null) {
                return;
            }
            numDataPoints++;
            sum = sum.add((BigInteger) object);
        }

        public Object getValue() {
            if (numDataPoints == 0) {
                return null;
            }
            return sum;
        }
    }

    private static class ExprDotEvalSumMethodLong implements ExprDotEvalSumMethod {
        protected long sum;
        protected long numDataPoints;

        public void enter(Object object) {
            if (object == null) {
                return;
            }
            numDataPoints++;
            sum += (Long) object;
        }

        public Object getValue() {
            if (numDataPoints == 0) {
                return null;
            }
            return sum;
        }
    }

    private static class ExprDotEvalSumMethodInteger implements ExprDotEvalSumMethod {
        protected int sum;
        protected long numDataPoints;

        public void enter(Object object) {
            if (object == null) {
                return;
            }
            numDataPoints++;
            sum += (Integer) object;
        }

        public Object getValue() {
            if (numDataPoints == 0) {
                return null;
            }
            return sum;
        }
    }
}
