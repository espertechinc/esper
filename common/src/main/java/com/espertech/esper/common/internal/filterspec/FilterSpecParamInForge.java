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
package com.espertech.esper.common.internal.filterspec;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.common.client.util.HashableMultiKey;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbolWEventType;
import com.espertech.esper.common.internal.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupableForge;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceRuntime;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.*;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * This class represents a 'in' filter parameter in an {@link FilterSpecActivatable} filter specification.
 * <p>
 * The 'in' checks for a list of values.
 */
public final class FilterSpecParamInForge extends FilterSpecParamForge {
    private final List<FilterSpecParamInValueForge> listOfValues;
    private Object[] inListConstantsOnly;
    private boolean hasCollMapOrArray;
    private FilterSpecParamInAdder[] adders;

    /**
     * Ctor.
     *
     * @param lookupable     is the event property or function
     * @param filterOperator is expected to be the IN-list operator
     * @param listofValues   is a list of constants and event property names
     * @throws IllegalArgumentException for illegal args
     */
    public FilterSpecParamInForge(ExprFilterSpecLookupableForge lookupable,
                                  FilterOperator filterOperator,
                                  List<FilterSpecParamInValueForge> listofValues)
            throws IllegalArgumentException {
        super(lookupable, filterOperator);
        this.listOfValues = listofValues;

        for (FilterSpecParamInValueForge value : listofValues) {
            Class returnType = value.getReturnType();
            if (JavaClassHelper.isCollectionMapOrArray(returnType)) {
                hasCollMapOrArray = true;
                break;
            }
        }

        if (hasCollMapOrArray) {
            adders = new FilterSpecParamInAdder[listofValues.size()];
            for (int i = 0; i < listofValues.size(); i++) {
                Class returnType = listofValues.get(i).getReturnType();
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
        for (FilterSpecParamInValueForge value : listofValues) {
            if (!value.isConstant()) {
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

    public final Object getFilterValue(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext, ClasspathImportServiceRuntime classpathImportService, Annotation[] annotations) {
        // If the list of values consists of all-constants and no event properties, then use cached version
        if (inListConstantsOnly != null) {
            return inListConstantsOnly;
        }
        return getFilterValues(matchedEvents, exprEvaluatorContext);
    }

    public final String toString() {
        return super.toString() + "  in=(listOfValues=" + listOfValues.toString() + ')';
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof FilterSpecParamInForge)) {
            return false;
        }

        FilterSpecParamInForge other = (FilterSpecParamInForge) obj;
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

    public CodegenMethod makeCodegen(CodegenClassScope classScope, CodegenMethodScope parent, SAIFFInitializeSymbolWEventType symbols) {
        CodegenMethod method = parent.makeChild(FilterSpecParam.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(ExprFilterSpecLookupable.class, "lookupable", localMethod(lookupable.makeCodegen(method, symbols, classScope)))
                .declareVar(FilterOperator.class, "op", enumValue(FilterOperator.class, filterOperator.name()));

        CodegenExpressionNewAnonymousClass param = newAnonymousClass(method.getBlock(), FilterSpecParam.class, Arrays.asList(ref("lookupable"), ref("op")));
        CodegenMethod getFilterValue = CodegenMethod.makeParentNode(Object.class, this.getClass(), classScope).addParam(FilterSpecParam.GET_FILTER_VALUE_FP);
        param.addMethod("getFilterValue", getFilterValue);
        if (inListConstantsOnly != null) {
            getFilterValue.getBlock().methodReturn(newInstance(HashableMultiKey.class, constant(inListConstantsOnly)));
        } else if (!hasCollMapOrArray) {
            getFilterValue.getBlock().declareVar(Object[].class, "values", newArrayByLength(Object.class, constant(listOfValues.size())));
            for (int i = 0; i < listOfValues.size(); i++) {
                FilterSpecParamInValueForge forge = listOfValues.get(i);
                getFilterValue.getBlock().assignArrayElement(ref("values"), constant(i), forge.makeCodegen(classScope, method));
            }
            getFilterValue.getBlock().methodReturn(newInstance(HashableMultiKey.class, ref("values")));
        } else {
            getFilterValue.getBlock().declareVar(ArrayDeque.class, "values", newInstance(ArrayDeque.class, constant(listOfValues.size())));
            for (int i = 0; i < listOfValues.size(); i++) {
                String valueName = "value" + i;
                String adderName = "adder" + i;
                getFilterValue.getBlock()
                        .declareVar(Object.class, valueName, listOfValues.get(i).makeCodegen(classScope, parent))
                        .ifRefNotNull(valueName)
                        .declareVar(adders[i].getClass(), adderName, enumValue(adders[i].getClass(), "INSTANCE"))
                        .exprDotMethod(ref(adderName), "add", ref("values"), ref(valueName))
                        .blockEnd();
            }
            getFilterValue.getBlock().methodReturn(newInstance(HashableMultiKey.class, exprDotMethod(ref("values"), "toArray")));
        }

        method.getBlock().methodReturn(param);
        return method;
    }

    private Object[] getFilterValues(MatchedEventMap matchedEvents, ExprEvaluatorContext exprEvaluatorContext) {
        if (!hasCollMapOrArray) {
            Object[] constants = new Object[listOfValues.size()];
            int count = 0;
            for (FilterSpecParamInValueForge valuePlaceholder : listOfValues) {
                constants[count++] = valuePlaceholder.getFilterValue(matchedEvents, exprEvaluatorContext);
            }
            return constants;
        }

        ArrayDeque<Object> constants = new ArrayDeque<>(listOfValues.size());
        int count = 0;
        for (FilterSpecParamInValueForge valuePlaceholder : listOfValues) {
            Object value = valuePlaceholder.getFilterValue(matchedEvents, exprEvaluatorContext);
            if (value != null) {
                adders[count].add(constants, value);
            }
            count++;
        }
        return constants.toArray();
    }

    public static class InValueAdderArray implements FilterSpecParamInAdder {
        public final static InValueAdderArray INSTANCE = new InValueAdderArray();

        private InValueAdderArray() {
        }

        public void add(Collection<Object> constants, Object value) {
            int len = Array.getLength(value);
            for (int i = 0; i < len; i++) {
                constants.add(Array.get(value, i));
            }
        }
    }

    public static class InValueAdderMap implements FilterSpecParamInAdder {
        public final static InValueAdderMap INSTANCE = new InValueAdderMap();

        private InValueAdderMap() {
        }

        public void add(Collection<Object> constants, Object value) {
            Map map = (Map) value;
            constants.addAll(map.keySet());
        }
    }

    public static class InValueAdderColl implements FilterSpecParamInAdder {
        public final static InValueAdderColl INSTANCE = new InValueAdderColl();

        private InValueAdderColl() {
        }

        public void add(Collection<Object> constants, Object value) {
            Collection coll = (Collection) value;
            constants.addAll(coll);
        }
    }

    public static class InValueAdderPlain implements FilterSpecParamInAdder {
        public final static InValueAdderPlain INSTANCE = new InValueAdderPlain();

        private InValueAdderPlain() {
        }

        public void add(Collection<Object> constants, Object value) {
            constants.add(value);
        }
    }
}
