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
package com.espertech.esper.epl.join.table;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.join.exec.composite.CompositeIndexEnterRemove;
import com.espertech.esper.epl.join.exec.composite.CompositeIndexQueryResultPostProcessor;

import java.util.*;

/**
 * For use when the index comprises of either two or more ranges or a unique key in combination with a range.
 * Organizes into a TreeMap&lt;key, TreeMap&lt;key2, Set&lt;EventBean&gt;&gt;, for short. The top level can also be just Map&lt;MultiKeyUntyped, TreeMap...&gt;.
 * Expected at least either (A) one key and one range or (B) zero keys and 2 ranges.
 * <p>
 * An alternative implementatation could have been based on "TreeMap&lt;ComparableMultiKey, Set&lt;EventBean&gt;&gt;&gt;", however the following implication arrive
 * - not applicable for range-only lookups (since there the key can be the value itself
 * - not applicable for multiple nested range as ordering not nested
 * - each add/remove and lookup would also need to construct a key object.
 */
public class PropertyCompositeEventTableImpl extends PropertyCompositeEventTable {
    private final CompositeIndexEnterRemove chain;

    /**
     * Index table (sorted and/or keyed, always nested).
     */
    protected final Map<Object, Object> index;

    public PropertyCompositeEventTableImpl(Class[] optKeyCoercedTypes, Class[] optRangeCoercedTypes, EventTableOrganization organization, boolean isHashKeyed, CompositeIndexEnterRemove chain) {
        super(optKeyCoercedTypes, optRangeCoercedTypes, organization);
        this.chain = chain;
        if (isHashKeyed) {
            index = new HashMap<Object, Object>();
        } else {
            index = new TreeMap<Object, Object>();
        }
    }

    public Map<Object, Object> getIndex() {
        return index;
    }

    public void add(EventBean theEvent, ExprEvaluatorContext exprEvaluatorContext) {
        chain.enter(theEvent, index);
    }

    public void remove(EventBean theEvent, ExprEvaluatorContext exprEvaluatorContext) {
        chain.remove(theEvent, index);
    }

    public boolean isEmpty() {
        return index.isEmpty();
    }

    public Iterator<EventBean> iterator() {
        HashSet<EventBean> result = new LinkedHashSet<EventBean>();
        chain.getAll(result, index);
        return result.iterator();
    }

    public void clear() {
        index.clear();
    }

    public void destroy() {
        clear();
    }

    public int getNumKeys() {
        return index.size();
    }

    public Class getProviderClass() {
        return PropertyCompositeEventTable.class;
    }

    public CompositeIndexQueryResultPostProcessor getPostProcessor() {
        return null;
    }
}
