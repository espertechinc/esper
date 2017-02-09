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
package com.espertech.esper.epl.expression.funcs;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.epl.expression.core.*;
import com.espertech.esper.event.vaevent.VariantEvent;
import com.espertech.esper.filter.FilterSpecLookupable;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.io.StringWriter;
import java.util.Map;

/**
 * Represents the TYPEOF(a) function is an expression tree.
 */
public class ExprTypeofNode extends ExprNodeBase implements ExprFilterOptimizableNode {
    private static final long serialVersionUID = -612634538694877204L;
    private transient ExprEvaluator evaluator;

    /**
     * Ctor.
     */
    public ExprTypeofNode() {
    }

    public ExprEvaluator getExprEvaluator() {
        return evaluator;
    }

    public Map<String, Object> getEventType() {
        return null;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if (this.getChildNodes().length != 1) {
            throw new ExprValidationException("Typeof node must have 1 child expression node supplying the expression to test");
        }

        if (this.getChildNodes()[0] instanceof ExprStreamUnderlyingNode) {
            ExprStreamUnderlyingNode stream = (ExprStreamUnderlyingNode) getChildNodes()[0];
            evaluator = new StreamEventTypeEval(stream.getStreamId());
            return null;
        }

        if (this.getChildNodes()[0] instanceof ExprIdentNode) {
            ExprIdentNode ident = (ExprIdentNode) getChildNodes()[0];
            int streamNum = validationContext.getStreamTypeService().getStreamNumForStreamName(ident.getFullUnresolvedName());
            if (streamNum != -1) {
                evaluator = new StreamEventTypeEval(streamNum);
                return null;
            }

            EventType eventType = validationContext.getStreamTypeService().getEventTypes()[ident.getStreamId()];
            if (eventType.getFragmentType(ident.getResolvedPropertyName()) != null) {
                evaluator = new FragmentTypeEval(ident.getStreamId(), eventType, ident.getResolvedPropertyName());
                return null;
            }
        }

        evaluator = new InnerEvaluator(this.getChildNodes()[0].getExprEvaluator());
        return null;
    }

    public boolean isConstantResult() {
        return false;
    }

    public Class getType() {
        return String.class;
    }

    public boolean getFilterLookupEligible() {
        return true;
    }

    public FilterSpecLookupable getFilterLookupable() {
        EventPropertyGetter getter = new EventPropertyGetter() {
            public Object get(EventBean eventBean) throws PropertyAccessException {
                return eventBean.getEventType().getName();
            }

            public boolean isExistsProperty(EventBean eventBean) {
                return true;
            }

            public Object getFragment(EventBean eventBean) throws PropertyAccessException {
                return null;
            }
        };
        return new FilterSpecLookupable(ExprNodeUtility.toExpressionStringMinPrecedenceSafe(this), getter, String.class, true);
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append("typeof(");
        this.getChildNodes()[0].toEPL(writer, ExprPrecedenceEnum.MINIMUM);
        writer.append(')');
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public boolean equalsNode(ExprNode node) {
        return node instanceof ExprTypeofNode;
    }

    public static class StreamEventTypeEval implements ExprEvaluator {
        private final int streamNum;

        public StreamEventTypeEval(int streamNum) {
            this.streamNum = streamNum;
        }

        @Override
        public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qExprTypeof();
            }
            EventBean theEvent = eventsPerStream[streamNum];
            if (theEvent == null) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aExprTypeof(null);
                }
                return null;
            }
            if (theEvent instanceof VariantEvent) {
                String typeName = ((VariantEvent) theEvent).getUnderlyingEventBean().getEventType().getName();
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aExprTypeof(typeName);
                }
                return typeName;
            }
            if (InstrumentationHelper.ENABLED) {
                String typeName = theEvent.getEventType().getName();
                InstrumentationHelper.get().aExprTypeof(typeName);
                return typeName;
            }
            return theEvent.getEventType().getName();
        }

        @Override
        public Class getType() {
            return String.class;
        }
    }

    public static class FragmentTypeEval implements ExprEvaluator {

        private final int streamId;
        private final EventPropertyGetter getter;
        private final String fragmentType;

        public FragmentTypeEval(int streamId, EventType eventType, String resolvedPropertyName) {
            this.streamId = streamId;
            getter = eventType.getGetter(resolvedPropertyName);
            fragmentType = eventType.getFragmentType(resolvedPropertyName).getFragmentType().getName();
        }

        @Override
        public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qExprTypeof();
            }
            EventBean theEvent = eventsPerStream[streamId];
            if (theEvent == null) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aExprTypeof(null);
                }
                return null;
            }
            Object fragment = getter.getFragment(theEvent);
            if (fragment == null) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aExprTypeof(null);
                }
                return null;
            }
            if (fragment instanceof EventBean) {
                EventBean bean = (EventBean) fragment;
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aExprTypeof(bean.getEventType().getName());
                }
                return bean.getEventType().getName();
            }
            if (fragment.getClass().isArray()) {
                String type = fragmentType + "[]";
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aExprTypeof(type);
                }
                return type;
            }
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprTypeof(null);
            }
            return null;
        }

        @Override
        public Class getType() {
            return String.class;
        }

    }

    private static class InnerEvaluator implements ExprEvaluator {
        private final ExprEvaluator evaluator;

        public InnerEvaluator(ExprEvaluator evaluator) {
            this.evaluator = evaluator;
        }

        @Override
        public Class getType() {
            return String.class;
        }

        @Override
        public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().qExprTypeof();
            }
            Object result = evaluator.evaluate(eventsPerStream, isNewData, context);
            if (result == null) {
                if (InstrumentationHelper.ENABLED) {
                    InstrumentationHelper.get().aExprTypeof(null);
                }
                return null;
            }
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprTypeof(result.getClass().getSimpleName());
            }
            return result.getClass().getSimpleName();
        }
    }
}
