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
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.metrics.instrumentation.InstrumentationHelper;

public class ExprIdentNodeEvaluatorImpl implements ExprIdentNodeEvaluator {
    private final int streamNum;
    private final EventPropertyGetter propertyGetter;
    private final Class propertyType;
    private final ExprIdentNode identNode;

    public ExprIdentNodeEvaluatorImpl(int streamNum, EventPropertyGetter propertyGetter, Class propertyType, ExprIdentNode identNode) {
        this.streamNum = streamNum;
        this.propertyGetter = propertyGetter;
        this.propertyType = propertyType;
        this.identNode = identNode;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext exprEvaluatorContext) {
        if (InstrumentationHelper.ENABLED) {
            InstrumentationHelper.get().qExprIdent(identNode.getFullUnresolvedName());
        }
        EventBean theEvent = eventsPerStream[streamNum];
        if (theEvent == null) {
            if (InstrumentationHelper.ENABLED) {
                InstrumentationHelper.get().aExprIdent(null);
            }
            return null;
        }
        if (InstrumentationHelper.ENABLED) {
            Object result = propertyGetter.get(theEvent);
            InstrumentationHelper.get().aExprIdent(result);
            return result;
        }

        return propertyGetter.get(theEvent);
    }

    public Class getType() {
        return propertyType;
    }

    public EventPropertyGetter getGetter() {
        return propertyGetter;
    }

    /**
     * Returns true if the property exists, or false if not.
     *
     * @param eventsPerStream each stream's events
     * @param isNewData       if the stream represents insert or remove stream
     * @return true if the property exists, false if not
     */
    public boolean evaluatePropertyExists(EventBean[] eventsPerStream, boolean isNewData) {
        EventBean theEvent = eventsPerStream[streamNum];
        if (theEvent == null) {
            return false;
        }
        return propertyGetter.isExistsProperty(theEvent);
    }

    public int getStreamNum() {
        return streamNum;
    }

    public boolean isContextEvaluated() {
        return false;
    }
}
