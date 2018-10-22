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
package com.espertech.esper.common.internal.view.core;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.context.util.AgentInstanceStopCallback;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluator;

import java.util.List;

public class ViewFactoryUtil {
    public static int evaluateSizeParam(String viewName, ExprEvaluator sizeEvaluator, AgentInstanceContext context) {
        Number size = (Number) sizeEvaluator.evaluate(null, true, context);
        if (!validateSize(size)) {
            throw new EPException(getSizeValidationMsg(viewName, size));
        }
        return size.intValue();
    }

    private static boolean validateSize(Number size) {
        return !(size == null || size.intValue() <= 0);
    }

    private static String getSizeValidationMsg(String viewName, Number size) {
        return viewName + " view requires a positive integer for size but received " + size;
    }

    public static ViewablePair materialize(ViewFactory[] factories, Viewable eventStreamParent, AgentInstanceViewFactoryChainContext viewFactoryChainContext, List<AgentInstanceStopCallback> stopCallbacks) {
        if (factories.length == 0) {
            return new ViewablePair(eventStreamParent, eventStreamParent);
        }

        Viewable current = eventStreamParent;
        Viewable topView = null;
        Viewable streamView = null;

        for (ViewFactory viewFactory : factories) {
            View view = viewFactory.makeView(viewFactoryChainContext);
            if (view instanceof AgentInstanceStopCallback) {
                stopCallbacks.add((AgentInstanceStopCallback) view);
            }
            current.setChild(view);
            view.setParent(current);
            if (topView == null) {
                topView = view;
            }
            streamView = view;
            current = view;
        }

        return new ViewablePair(topView, streamView);
    }
}
