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
package com.espertech.esper.filterspec;

import com.espertech.esper.collection.MultiKeyUntyped;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.util.JavaClassHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.*;

/**
 * This class represents a 'in' filter parameter in an {@link FilterSpecCompiled} filter specification.
 * <p>
 * The 'in' checks for a list of values.
 */
public final class FilterSpecParamIn extends FilterSpecParam {
    private final List<FilterSpecParamInValue> listOfValues;
    private MultiKeyUntyped inListConstantsOnly;
    private boolean hasCollMapOrArray;
    private InValueAdder[] adders;
    private static final long serialVersionUID = 1723225284589047752L;

    /**
     * Ctor.
     *
     * @param lookupable     is the event property or function
     * @param filterOperator is expected to be the IN-list operator
     * @param listofValues   is a list of constants and event property names
     * @throws IllegalArgumentException for illegal args
     */
    public FilterSpecParamIn(ExprFilterSpecLookupable lookupable,
                             FilterOperator filterOperator,
                             List<FilterSpecParamInValue> listofValues)
            throws IllegalArgumentException {
        super(lookupable, filterOperator);
        this.listOfValues = listofValues;

        for (FilterSpecParamInValue value : listofValues) {
            Class returnType = value.getReturnType();
            if (JavaClassHelper.isCollectionMapOrArray(returnType)) {
                hasCollMapOrArray = true;
                break;
            }
        }

        if (hasCollMapOrArray) {
            adders = new InValueAdder[listofValues.size()];
            for (int i = 0; i < listofValues.size(); i++) {
                Class returnType = listofValues.get(0).getReturnType();
                if (returnType == null) {
                    adders[i] = InValueAdderPlain.INSTANCE;
                } else if (returnType.isArray()) {
                    adders[i] = InValueAdderArray.INSTANCE;
                } else if (JavaClassHelper.isImplementsInterface(returnType, Map.class)) {
                    adders[i] = InValueAdderMap.INSTANCE;
                } else if (JavaClassHelper.isImplementsInterface(returnType, Collection.class)) {
                    adders[i] = InValueAdderColl.INSTANCE;
                } else {
                    adders[i] = InValueAdderPlain.INSTANCE;
                }
            }
        }

        boolean isAllConstants = true;
        for (FilterSpecParamInValue value : listofValues) {
            if (!value.constant()) {
                isAllConstants = false;
                break;
            }
        }

        if (isAllConstants) {
            inListConstantsOnly = getFilterValues(null, null);
        }

        if ((filterOperator != FilterOperator.IN_LIST_OF_VALUES) && ((filterOperator != FilterOperator.NOT_IN_LIST_OF_VALUES))) {
            throw new IllegalArgumentException("Illegal filter operator " + filterOperator + " supplied to " +
                    "in-values filter parameter");
        }
    }

    public final Object getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext, EngineImportService engineImportService, Annotation[] annotations) {
        // If the list of values consists of all-constants and no event properties, then use cached version
        if (inListConstantsOnly != null) {
            return inListConstantsOnly;
        }
        return getFilterValues(matchedEvents, exprEvaluatorContext);
    }

    /**
     * Returns the list of values we are asking to match.
     *
     * @return list of filter values
     */
    public List<FilterSpecParamInValue> getListOfValues() {
        return listOfValues;
    }

    public final String toString() {
        return super.toString() + "  in=(listOfValues=" + listOfValues.toString() + ')';
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof FilterSpecParamIn)) {
            return false;
        }

        FilterSpecParamIn other = (FilterSpecParamIn) obj;
        if (!super.equals(other)) {
            return false;
        }

        if (listOfValues.size() != other.listOfValues.size()) {
            return false;
        }

        if (!(Arrays.deepEquals(listOfValues.toArray(), other.listOfValues.toArray()))) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (listOfValues != null ? listOfValues.hashCode() : 0);
        return result;
    }

    private MultiKeyUntyped getFilterValues(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext) {
        if (!hasCollMapOrArray) {
            Object[] constants = new Object[listOfValues.size()];
            int count = 0;
            for (FilterSpecParamInValue valuePlaceholder : listOfValues) {
                constants[count++] = valuePlaceholder.getFilterValue(matchedEvents, exprEvaluatorContext);
            }
            return new MultiKeyUntyped(constants);
        }

        ArrayDeque<Object> constants = new ArrayDeque<>(listOfValues.size());
        int count = 0;
        for (FilterSpecParamInValue valuePlaceholder : listOfValues) {
            Object value = valuePlaceholder.getFilterValue(matchedEvents, exprEvaluatorContext);
            if (value != null) {
                adders[count].add(constants, value);
            }
            count++;
        }
        return new MultiKeyUntyped(constants.toArray());
    }

    private interface InValueAdder {
        void add(Collection<Object> constants, Object value);
    }

    private static class InValueAdderArray implements InValueAdder {
        private final static InValueAdderArray INSTANCE = new InValueAdderArray();

        private InValueAdderArray() {
        }

        public void add(Collection<Object> constants, Object value) {
            int len = Array.getLength(value);
            for (int i = 0; i < len; i++) {
                constants.add(Array.get(value, i));
            }
        }
    }

    private static class InValueAdderMap implements InValueAdder {
        private final static InValueAdderMap INSTANCE = new InValueAdderMap();

        private InValueAdderMap() {
        }

        public void add(Collection<Object> constants, Object value) {
            Map map = (Map) value;
            constants.addAll(map.keySet());
        }
    }

    private static class InValueAdderColl implements InValueAdder {
        private final static InValueAdderColl INSTANCE = new InValueAdderColl();

        private InValueAdderColl() {
        }

        public void add(Collection<Object> constants, Object value) {
            Collection coll = (Collection) value;
            constants.addAll(coll);
        }
    }

    private static class InValueAdderPlain implements InValueAdder {
        private final static InValueAdderPlain INSTANCE = new InValueAdderPlain();

        private InValueAdderPlain() {
        }

        public void add(Collection<Object> constants, Object value) {
            constants.add(value);
        }
    }
}
