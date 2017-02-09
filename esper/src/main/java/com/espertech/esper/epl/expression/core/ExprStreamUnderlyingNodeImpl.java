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
package com.espertech.esper.epl.expression.core;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

import java.io.StringWriter;

/**
 * Represents an stream selector that returns the streams underlying event, or null if undefined.
 */
public class ExprStreamUnderlyingNodeImpl extends ExprNodeBase implements ExprEvaluator, ExprStreamUnderlyingNode {
    private final String streamName;
    private final boolean isWildcard;
    private int streamNum = -1;
    private Class type;
    private transient EventType eventType;
    private static final long serialVersionUID = 6611578192872250478L;

    public ExprStreamUnderlyingNodeImpl(String streamName, boolean isWildcard) {
        if ((streamName == null) && (!isWildcard)) {
            throw new IllegalArgumentException("Stream name is null");
        }
        this.streamName = streamName;
        this.isWildcard = isWildcard;
    }

    @Override
    public ExprEvaluator getExprEvaluator() {
        return this;
    }

    /**
     * Returns the stream name.
     *
     * @return stream name
     */
    public String getStreamName() {
        return streamName;
    }

    public Integer getStreamReferencedIfAny() {
        return getStreamId();
    }

    public String getRootPropertyNameIfAny() {
        return null;
    }

    public ExprNode validate(ExprValidationContext validationContext) throws ExprValidationException {
        if (streamName == null && isWildcard) {
            if (validationContext.getStreamTypeService().getStreamNames().length > 1) {
                throw new ExprValidationException("Wildcard must be stream wildcard if specifying multiple streams, use the 'streamname.*' syntax instead");
            }
            streamNum = 0;
        } else {
            streamNum = validationContext.getStreamTypeService().getStreamNumForStreamName(streamName);
        }

        if (streamNum == -1) {
            throw new ExprValidationException("Stream by name '" + streamName + "' could not be found among all streams");
        }

        eventType = validationContext.getStreamTypeService().getEventTypes()[streamNum];
        type = eventType.getUnderlyingType();
        return null;
    }

    public Class getType() {
        if (streamNum == -1) {
            throw new IllegalStateException("Stream underlying node has not been validated");
        }
        return type;
    }

    public boolean isConstantResult() {
        return false;
    }

    /**
     * Returns stream id supplying the property value.
     *
     * @return stream number
     */
    public int getStreamId() {
        if (streamNum == -1) {
            throw new IllegalStateException("Stream underlying node has not been validated");
        }
        return streamNum;
    }

    public String toString() {
        return "streamName=" + streamName +
                " streamNum=" + streamNum;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprStreamUnd(this);
        }
        EventBean theEvent = eventsPerStream[streamNum];
        if (theEvent == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprStreamUnd(null);
            }
            return null;
        }
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().aExprStreamUnd(theEvent.getUnderlying());
        }
        return theEvent.getUnderlying();
    }

    public void toPrecedenceFreeEPL(StringWriter writer) {
        writer.append(streamName);
        if (isWildcard) {
            writer.append(".*");
        }
    }

    public ExprPrecedenceEnum getPrecedence() {
        return ExprPrecedenceEnum.UNARY;
    }

    public EventType getEventType() {
        return eventType;
    }

    public boolean equalsNode(ExprNode node) {
        if (!(node instanceof ExprStreamUnderlyingNodeImpl)) {
            return false;
        }

        ExprStreamUnderlyingNodeImpl other = (ExprStreamUnderlyingNodeImpl) node;
        if (this.isWildcard != other.isWildcard) {
            return false;
        }
        if (this.isWildcard) {
            return true;
        }
        return this.streamName.equals(other.streamName);
    }
}
