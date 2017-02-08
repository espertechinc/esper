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

public class ExprIdentNodeEvaluatorContext implements ExprIdentNodeEvaluator {

    private final int streamNum;
    private final Class resultType;
    private final EventPropertyGetter getter;

    public ExprIdentNodeEvaluatorContext(int streamNum, Class resultType, EventPropertyGetter getter) {
        this.streamNum = streamNum;
        this.resultType = resultType;
        this.getter = getter;
    }

    public boolean evaluatePropertyExists(EventBean[] eventsPerStream, boolean isNewData) {
        return true;
    }

    public int getStreamNum() {
        return streamNum;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        if (context.getContextProperties() != null) {
            return getter.get(context.getContextProperties());
        }
        return null;
    }

    public Class getType() {
        return resultType;
    }

    public EventPropertyGetter getGetter() {
        return getter;
    }

    public boolean isContextEvaluated() {
        return true;
    }
}
