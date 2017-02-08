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
package com.espertech.esper.epl.named;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.collection.OneEventCollection;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.updatehelper.EventBeanUpdateHelper;

public class NamedWindowOnMergeActionUpd extends NamedWindowOnMergeAction {
    private final EventBeanUpdateHelper updateHelper;

    public NamedWindowOnMergeActionUpd(ExprEvaluator optionalFilter, EventBeanUpdateHelper updateHelper) {
        super(optionalFilter);
        this.updateHelper = updateHelper;
    }

    public void apply(EventBean matchingEvent, EventBean[] eventsPerStream, OneEventCollection newData, OneEventCollection oldData, ExprEvaluatorContext exprEvaluatorContext) {
        EventBean copy = updateHelper.updateWCopy(matchingEvent, eventsPerStream, exprEvaluatorContext);
        newData.add(copy);
        oldData.add(matchingEvent);
    }

    public String getName() {
        return "update";
    }
}
