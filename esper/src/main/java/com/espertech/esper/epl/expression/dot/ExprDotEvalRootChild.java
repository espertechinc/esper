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
package com.espertech.esper.epl.expression.dot;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprEvaluatorEnumeration;
import com.espertech.esper.epl.expression.core.ExprValidationException;
import com.espertech.esper.epl.expression.dot.inner.*;
import com.espertech.esper.epl.rettype.*;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;
import com.espertech.esper.util.JavaClassHelper;

import java.util.Collection;

public class ExprDotEvalRootChild implements ExprEvaluator, ExprEvaluatorEnumeration {
    private final ExprDotNode dotNode;
    private final ExprDotEvalRootChildInnerEval innerEvaluator;
    private final ExprDotEval[] evalIteratorEventBean;
    private final ExprDotEval[] evalUnpacking;

    public ExprDotEvalRootChild(boolean hasEnumerationMethod, ExprDotNode dotNode, ExprEvaluator rootNodeEvaluator, ExprEvaluatorEnumeration rootLambdaEvaluator, EPType typeInfo, ExprDotEval[] evalIteratorEventBean, ExprDotEval[] evalUnpacking, boolean checkedUnpackEvent) {
        this.dotNode = dotNode;
        if (rootLambdaEvaluator != null) {
            if (typeInfo instanceof EventMultiValuedEPType) {
                innerEvaluator = new InnerEvaluatorEnumerableEventCollection(rootLambdaEvaluator, ((EventMultiValuedEPType) typeInfo).getComponent());
            } else if (typeInfo instanceof EventEPType) {
                innerEvaluator = new InnerEvaluatorEnumerableEventBean(rootLambdaEvaluator, ((EventEPType) typeInfo).getType());
            } else {
                innerEvaluator = new InnerEvaluatorEnumerableScalarCollection(rootLambdaEvaluator, ((ClassMultiValuedEPType) typeInfo).getComponent());
            }
        } else {
            if (checkedUnpackEvent) {
                innerEvaluator = new InnerEvaluatorScalarUnpackEvent(rootNodeEvaluator);
            } else {
                Class returnType = rootNodeEvaluator.getType();
                if (hasEnumerationMethod && returnType.isArray()) {
                    if (returnType.getComponentType().isPrimitive()) {
                        innerEvaluator = new InnerEvaluatorArrPrimitiveToColl(rootNodeEvaluator);
                    } else {
                        innerEvaluator = new InnerEvaluatorArrObjectToColl(rootNodeEvaluator);
                    }
                } else if (hasEnumerationMethod && JavaClassHelper.isImplementsInterface(returnType, Collection.class)) {
                    innerEvaluator = new InnerEvaluatorColl(rootNodeEvaluator);
                } else {
                    innerEvaluator = new InnerEvaluatorScalar(rootNodeEvaluator);
                }
            }
        }
        this.evalUnpacking = evalUnpacking;
        this.evalIteratorEventBean = evalIteratorEventBean;
    }

    public Class getType() {
        return EPTypeHelper.getNormalizedClass(evalUnpacking[evalUnpacking.length - 1].getTypeInfo());
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprDot(dotNode);
        }
        Object inner = innerEvaluator.evaluate(eventsPerStream, isNewData, context);
        if (inner != null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qExprDotChain(innerEvaluator.getTypeInfo(), inner, evalUnpacking);
            }
            inner = ExprDotNodeUtility.evaluateChain(evalUnpacking, inner, eventsPerStream, isNewData, context);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprDotChain();
            }
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprDot(inner);
        }
        return inner;
    }

    public Collection<EventBean> evaluateGetROCollectionEvents(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object inner = innerEvaluator.evaluateGetROCollectionEvents(eventsPerStream, isNewData, context);
        if (inner != null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qExprDotChain(innerEvaluator.getTypeInfo(), inner, evalUnpacking);
            }
            inner = ExprDotNodeUtility.evaluateChain(evalIteratorEventBean, inner, eventsPerStream, isNewData, context);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprDotChain();
            }
            if (inner instanceof Collection) {
                return (Collection<EventBean>) inner;
            }
        }
        return null;
    }

    public Collection evaluateGetROCollectionScalar(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        Object inner = innerEvaluator.evaluateGetROCollectionScalar(eventsPerStream, isNewData, context);
        if (inner != null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qExprDotChain(innerEvaluator.getTypeInfo(), inner, evalUnpacking);
            }
            inner = ExprDotNodeUtility.evaluateChain(evalIteratorEventBean, inner, eventsPerStream, isNewData, context);
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprDotChain();
            }
            if (inner instanceof Collection) {
                return (Collection) inner;
            }
        }
        return null;
    }

    public EventType getEventTypeCollection(EventAdapterService eventAdapterService, int statementId) throws ExprValidationException {
        return innerEvaluator.getEventTypeCollection();
    }

    public Class getComponentTypeCollection() throws ExprValidationException {
        return innerEvaluator.getComponentTypeCollection();
    }

    public EventType getEventTypeSingle(EventAdapterService eventAdapterService, int statementId) throws ExprValidationException {
        return null;
    }

    public EventBean evaluateGetEventBean(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        return null;
    }
}
