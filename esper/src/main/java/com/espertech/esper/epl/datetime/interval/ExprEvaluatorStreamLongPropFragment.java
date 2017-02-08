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
package com.espertech.esper.epl.datetime.interval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

public class ExprEvaluatorStreamLongPropFragment implements ExprEvaluator {

    private final int streamId;
    private final EventPropertyGetter getterFragment;
    private final EventPropertyGetter getterTimestamp;

    public ExprEvaluatorStreamLongPropFragment(int streamId, EventPropertyGetter getterFragment, EventPropertyGetter getterTimestamp) {
        this.streamId = streamId;
        this.getterFragment = getterFragment;
        this.getterTimestamp = getterTimestamp;
    }

    public Object evaluate(EventBean[] eventsPerStream, boolean isNewData, ExprEvaluatorContext context) {
        EventBean theEvent = eventsPerStream[streamId];
        if (theEvent == null) {
            return null;
        }
        Object event = getterFragment.getFragment(theEvent);
        if (!(event instanceof EventBean)) {
            return null;
        }
        return getterTimestamp.get((EventBean) event);
    }

    public Class getType() {
        return Long.class;
    }

}
