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
package com.espertech.esper.common.internal.epl.join.queryplan;

import com.espertech.esper.common.client.EventPropertyValueGetter;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenMakeable;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyCodegen;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.index.advanced.index.service.EventAdvancedIndexProvisionCompileTime;
import com.espertech.esper.common.internal.epl.join.lookup.IndexedPropDesc;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import com.espertech.esper.common.internal.event.core.EventTypeSPI;
import com.espertech.esper.common.internal.event.core.EventTypeUtility;
import com.espertech.esper.common.internal.serde.compiletime.resolve.DataInputOutputSerdeForge;
import com.espertech.esper.common.internal.util.CollectionUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Specifies an index to build as part of an overall query plan.
 */
public class QueryPlanIndexItemForge implements CodegenMakeable<SAIFFInitializeSymbol> {
    private final String[] hashProps;
    private Class[] hashTypes;
    private MultiKeyClassRef hashMultiKeyClasses;
    private final String[] rangeProps;
    private final Class[] rangeTypes;
    private DataInputOutputSerdeForge[] rangeSerdes;
    private final boolean unique;
    private final EventAdvancedIndexProvisionCompileTime advancedIndexProvisionDesc;
    private final EventType eventType;

    public QueryPlanIndexItemForge(String[] hashProps, Class[] hashTypes, String[] rangeProps, Class[] rangeTypes, boolean unique, EventAdvancedIndexProvisionCompileTime advancedIndexProvisionDesc, EventType eventType) {
        if (advancedIndexProvisionDesc == null) {
            if (unique && hashProps.length == 0) {
                throw new IllegalArgumentException("Invalid unique index planned without hash index props");
            }
            if (unique && rangeProps.length > 0) {
                throw new IllegalArgumentException("Invalid unique index planned that includes range props");
            }
        }
        if (hashProps == null || hashTypes == null || rangeProps == null || rangeTypes == null) {
            throw new IllegalArgumentException("Invalid null hash and range props");
        }
        if (hashProps.length != hashTypes.length) {
            throw new IllegalArgumentException("Mismatch size hash props and types");
        }
        if (rangeProps.length != rangeTypes.length) {
            throw new IllegalArgumentException("Mismatch size hash props and types");
        }
        this.hashProps = hashProps;
        this.hashTypes = hashTypes;
        this.rangeProps = rangeProps;
        this.rangeTypes = rangeTypes;
        this.unique = unique;
        this.advancedIndexProvisionDesc = advancedIndexProvisionDesc;
        this.eventType = eventType;
    }

    public QueryPlanIndexItemForge(List<IndexedPropDesc> hashProps, List<IndexedPropDesc> btreeProps, boolean unique, EventAdvancedIndexProvisionCompileTime advancedIndexProvisionDesc, EventType eventType) { // EventAdvancedIndexProvisionDesc
        this(getNames(hashProps), getTypes(hashProps), getNames(btreeProps), getTypes(btreeProps), unique, advancedIndexProvisionDesc, eventType);
    }

    public String[] getHashProps() {
        return hashProps;
    }

    public Class[] getHashTypes() {
        return hashTypes;
    }

    public String[] getRangeProps() {
        return rangeProps;
    }

    public Class[] getRangeTypes() {
        return rangeTypes;
    }

    public void setHashTypes(Class[] hashTypes) {
        this.hashTypes = hashTypes;
    }

    public void setHashMultiKeyClasses(MultiKeyClassRef hashMultiKeyClasses) {
        this.hashMultiKeyClasses = hashMultiKeyClasses;
    }

    public void setRangeSerdes(DataInputOutputSerdeForge[] rangeSerdes) {
        this.rangeSerdes = rangeSerdes;
    }

    public MultiKeyClassRef getHashMultiKeyClasses() {
        return hashMultiKeyClasses;
    }

    public boolean isUnique() {
        return unique;
    }

    public EventAdvancedIndexProvisionCompileTime getAdvancedIndexProvisionDesc() {
        return advancedIndexProvisionDesc;
    }

    @Override
    public String toString() {
        return "QueryPlanIndexItem{" +
            "unique=" + unique +
            ", hashProps=" + Arrays.asList(hashProps) +
            ", rangeProps=" + Arrays.asList(rangeProps) +
            ", hashTypes=" + Arrays.asList(hashTypes) +
            ", rangeTypes=" + Arrays.asList(rangeTypes) +
            ", advanced=" + (advancedIndexProvisionDesc == null ? null : advancedIndexProvisionDesc.getIndexDesc().getIndexTypeName()) +
            "}";
    }

    public boolean equalsCompareSortedProps(QueryPlanIndexItemForge other) {
        if (unique != other.unique) {
            return false;
        }
        String[] otherIndexProps = CollectionUtil.copySortArray(other.getHashProps());
        String[] thisIndexProps = CollectionUtil.copySortArray(this.getHashProps());
        String[] otherRangeProps = CollectionUtil.copySortArray(other.getRangeProps());
        String[] thisRangeProps = CollectionUtil.copySortArray(this.getRangeProps());
        boolean compared = CollectionUtil.compare(otherIndexProps, thisIndexProps) && CollectionUtil.compare(otherRangeProps, thisRangeProps);
        return compared && advancedIndexProvisionDesc == null && other.advancedIndexProvisionDesc == null;
    }

    public List<IndexedPropDesc> getHashPropsAsList() {
        return asList(hashProps, hashTypes);
    }

    public List<IndexedPropDesc> getBtreePropsAsList() {
        return asList(rangeProps, rangeTypes);
    }

    private List<IndexedPropDesc> asList(String[] props, Class[] types) {
        if (props == null || props.length == 0) {
            return Collections.emptyList();
        }
        List<IndexedPropDesc> list = new ArrayList<>(props.length);
        for (int i = 0; i < props.length; i++) {
            list.add(new IndexedPropDesc(props[i], types[i]));
        }
        return list;
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        return make(parent, classScope);
    }

    public CodegenExpression make(CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(QueryPlanIndexItem.class, this.getClass(), classScope);

        EventPropertyGetterSPI[] propertyGetters = EventTypeUtility.getGetters(eventType, hashProps);
        Class[] propertyTypes = EventTypeUtility.getPropertyTypes(eventType, hashProps);

        CodegenExpression valueGetter = MultiKeyCodegen.codegenGetterMayMultiKey(eventType, propertyGetters, propertyTypes, hashTypes, hashMultiKeyClasses, method, classScope);

        CodegenExpression rangeGetters;
        if (rangeProps.length == 0) {
            rangeGetters = newArrayByLength(EventPropertyValueGetter.class, constant(0));
        } else {
            CodegenMethod makeMethod = parent.makeChild(EventPropertyValueGetter[].class, this.getClass(), classScope);
            makeMethod.getBlock().declareVar(EventPropertyValueGetter[].class, "getters", newArrayByLength(EventPropertyValueGetter.class, constant(rangeProps.length)));
            for (int i = 0; i < rangeProps.length; i++) {
                EventPropertyGetterSPI getter = ((EventTypeSPI) eventType).getGetterSPI(rangeProps[i]);
                Class getterType = eventType.getPropertyType(rangeProps[i]);
                Class coercionType = rangeTypes == null ? null : rangeTypes[i];
                CodegenExpression eval = EventTypeUtility.codegenGetterWCoerce(getter, getterType, coercionType, method, this.getClass(), classScope);
                makeMethod.getBlock().assignArrayElement(ref("getters"), constant(i), eval);
            }
            makeMethod.getBlock().methodReturn(ref("getters"));
            rangeGetters = localMethod(makeMethod);
        }

        CodegenExpression multiKeyTransform = MultiKeyCodegen.codegenMultiKeyFromArrayTransform(hashMultiKeyClasses, method, classScope);

        method.getBlock().methodReturn(newInstance(QueryPlanIndexItem.class,
            constant(hashProps), constant(hashTypes), valueGetter, multiKeyTransform, hashMultiKeyClasses == null ? constantNull() : hashMultiKeyClasses.getExprMKSerde(method, classScope),
            constant(rangeProps), constant(rangeTypes), rangeGetters, DataInputOutputSerdeForge.codegenArray(rangeSerdes, method, classScope, null),
            constant(unique),
            advancedIndexProvisionDesc == null ? constantNull() : advancedIndexProvisionDesc.codegenMake(method, classScope)));
        return localMethod(method);
    }

    private static String[] getNames(List<IndexedPropDesc> props) {
        String[] names = new String[props.size()];
        for (int i = 0; i < props.size(); i++) {
            names[i] = props.get(i).getIndexPropName();
        }
        return names;
    }

    private static Class[] getTypes(List<IndexedPropDesc> props) {
        Class[] types = new Class[props.size()];
        for (int i = 0; i < props.size(); i++) {
            types[i] = props.get(i).getCoercionType();
        }
        return types;
    }

    public QueryPlanIndexItem toRuntime() {
        if (advancedIndexProvisionDesc == null) {
            return null;
        }
        return new QueryPlanIndexItem(
            hashProps, hashTypes, null, null, null,
            rangeProps, rangeTypes, null, null,
            unique,
            advancedIndexProvisionDesc.toRuntime());
    }
}
