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
package com.espertech.esper.epl.join.exec.composite;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface CompositeAccessStrategy {
    public Set<EventBean> lookup(EventBean theEvent, Map parent, Set<EventBean> result, CompositeIndexQuery next, ExprEvaluatorContext context, ArrayList<Object> optionalKeyCollector, CompositeIndexQueryResultPostProcessor postProcessor);

    public Collection<EventBean> lookup(EventBean[] eventPerStream, Map parent, Collection<EventBean> result, CompositeIndexQuery next, ExprEvaluatorContext context, ArrayList<Object> optionalKeyCollector, CompositeIndexQueryResultPostProcessor postProcessor);
}
