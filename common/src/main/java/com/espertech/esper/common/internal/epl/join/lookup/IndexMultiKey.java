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
package com.espertech.esper.common.internal.epl.join.lookup;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.epl.lookup.AdvancedIndexIndexMultiKeyPart;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class IndexMultiKey {

    private final boolean unique;
    private final IndexedPropDesc[] hashIndexedProps;
    private final IndexedPropDesc[] rangeIndexedProps;
    private final AdvancedIndexIndexMultiKeyPart advancedIndexDesc;

    public IndexMultiKey(boolean unique, List<IndexedPropDesc> hashIndexedProps, List<IndexedPropDesc> rangeIndexedProps, AdvancedIndexIndexMultiKeyPart advancedIndexDesc) {
        this.unique = unique;
        this.hashIndexedProps = hashIndexedProps.toArray(new IndexedPropDesc[hashIndexedProps.size()]);
        this.rangeIndexedProps = rangeIndexedProps.toArray(new IndexedPropDesc[rangeIndexedProps.size()]);
        this.advancedIndexDesc = advancedIndexDesc;
    }

    public IndexMultiKey(boolean unique, IndexedPropDesc[] hashIndexedProps, IndexedPropDesc[] rangeIndexedProps, AdvancedIndexIndexMultiKeyPart advancedIndexDesc) {
        this.unique = unique;
        this.hashIndexedProps = hashIndexedProps;
        this.rangeIndexedProps = rangeIndexedProps;
        this.advancedIndexDesc = advancedIndexDesc;
    }

    public CodegenExpression make(CodegenMethodScope parent, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(IndexMultiKey.class, this.getClass(), classScope);
        CodegenExpression hashes = IndexedPropDesc.makeArray(hashIndexedProps);
        CodegenExpression ranges = IndexedPropDesc.makeArray(rangeIndexedProps);
        CodegenExpression advanced = advancedIndexDesc == null ? constantNull() : advancedIndexDesc.codegenMake(parent, classScope);
        method.getBlock().methodReturn(newInstance(IndexMultiKey.class, constant(unique), hashes, ranges, advanced));
        return localMethod(method);
    }

    public boolean isUnique() {
        return unique;
    }

    public IndexedPropDesc[] getHashIndexedProps() {
        return hashIndexedProps;
    }

    public IndexedPropDesc[] getRangeIndexedProps() {
        return rangeIndexedProps;
    }

    public AdvancedIndexIndexMultiKeyPart getAdvancedIndexDesc() {
        return advancedIndexDesc;
    }

    public String toQueryPlan() {
        StringWriter writer = new StringWriter();
        writer.append(unique ? "unique " : "non-unique ");
        writer.append("hash={");
        IndexedPropDesc.toQueryPlan(writer, hashIndexedProps);
        writer.append("} btree={");
        IndexedPropDesc.toQueryPlan(writer, rangeIndexedProps);
        writer.append("} advanced={");
        writer.append(advancedIndexDesc == null ? "" : advancedIndexDesc.toQueryPlan());
        writer.append("}");
        return writer.toString();
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IndexMultiKey that = (IndexMultiKey) o;

        if (unique != that.unique) return false;
        if (!Arrays.equals(hashIndexedProps, that.hashIndexedProps)) return false;
        if (!Arrays.equals(rangeIndexedProps, that.rangeIndexedProps)) return false;
        if (advancedIndexDesc == null) {
            return that.advancedIndexDesc == null;
        } else {
            return that.advancedIndexDesc != null && advancedIndexDesc.equalsAdvancedIndex(that.advancedIndexDesc);
        }
    }

    public int hashCode() {
        int result = Arrays.hashCode(hashIndexedProps);
        result = 31 * result + Arrays.hashCode(rangeIndexedProps);
        return result;
    }
}
