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

public interface CompositeIndexQuery {
    public void add(EventBean theEvent, Map value, Set<EventBean> result, CompositeIndexQueryResultPostProcessor postProcessor);

    public void add(EventBean[] eventsPerStream, Map value, Collection<EventBean> result, CompositeIndexQueryResultPostProcessor postProcessor);

    public Set<EventBean> get(EventBean theEvent, Map parent, ExprEvaluatorContext context, CompositeIndexQueryResultPostProcessor postProcessor);

    public Collection<EventBean> get(EventBean[] eventsPerStream, Map parent, ExprEvaluatorContext context, CompositeIndexQueryResultPostProcessor postProcessor);

    public Set<EventBean> getCollectKeys(EventBean theEvent, Map parent, ExprEvaluatorContext context, ArrayList<Object> keys, CompositeIndexQueryResultPostProcessor postProcessor);

    public Collection<EventBean> getCollectKeys(EventBean[] eventsPerStream, Map parent, ExprEvaluatorContext context, ArrayList<Object> keys, CompositeIndexQueryResultPostProcessor postProcessor);

    public void setNext(CompositeIndexQuery next);
}
