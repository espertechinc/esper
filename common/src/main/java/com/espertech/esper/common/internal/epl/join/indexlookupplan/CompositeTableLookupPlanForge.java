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
package com.espertech.esper.common.internal.epl.join.indexlookupplan;

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyClassRef;
import com.espertech.esper.common.internal.compile.multikey.MultiKeyCodegen;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprForge;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryHashKeyedForge;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryRange;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryRangeForge;
import com.espertech.esper.common.internal.epl.join.queryplan.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Plan to perform an indexed table lookup.
 */
public class CompositeTableLookupPlanForge extends TableLookupPlanForge {
    private final List<QueryGraphValueEntryHashKeyedForge> hashKeys;
    private final Class[] hashCoercionTypes;
    private final List<QueryGraphValueEntryRangeForge> rangeKeyPairs;
    private final Class[] optRangeCoercionTypes;
    private final QueryPlanIndexForge indexSpecs;
    private final MultiKeyClassRef optionalEPLTableLookupMultiKey;

    public CompositeTableLookupPlanForge(int lookupStream, int indexedStream, boolean indexedStreamIsVDW, EventType[] typesPerStream, TableLookupIndexReqKey indexNum, List<QueryGraphValueEntryHashKeyedForge> hashKeys, Class[] hashCoercionTypes, List<QueryGraphValueEntryRangeForge> rangeKeyPairs, Class[] optRangeCoercionTypes, QueryPlanIndexForge indexSpecs, MultiKeyClassRef optionalEPLTableLookupMultiKey) {
        super(lookupStream, indexedStream, indexedStreamIsVDW, typesPerStream, new TableLookupIndexReqKey[]{indexNum});
        this.hashKeys = hashKeys;
        this.hashCoercionTypes = hashCoercionTypes;
        this.rangeKeyPairs = rangeKeyPairs;
        this.optRangeCoercionTypes = optRangeCoercionTypes;
        this.indexSpecs = indexSpecs;
        this.optionalEPLTableLookupMultiKey = optionalEPLTableLookupMultiKey;
    }

    public TableLookupKeyDesc getKeyDescriptor() {
        return new TableLookupKeyDesc(hashKeys, rangeKeyPairs);
    }

    public Class typeOfPlanFactory() {
        return CompositeTableLookupPlanFactory.class;
    }

    public Collection<CodegenExpression> additionalParams(CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenExpression hashGetter = constantNull();
        if (!hashKeys.isEmpty()) {
            QueryPlanIndexItemForge indexForge = indexSpecs.getItems().get(getIndexNum()[0]);
            ExprForge[] forges = QueryGraphValueEntryHashKeyedForge.getForges(hashKeys.toArray(new QueryGraphValueEntryHashKeyedForge[hashKeys.size()]));
            if (indexForge != null) {
                hashGetter = MultiKeyCodegen.codegenExprEvaluatorMayMultikey(forges, hashCoercionTypes, indexForge.getHashMultiKeyClasses(), method, classScope);
            } else {
                hashGetter = MultiKeyCodegen.codegenExprEvaluatorMayMultikey(forges, hashCoercionTypes, optionalEPLTableLookupMultiKey, method, classScope);
            }
        }

        CodegenMethod rangeGetters = method.makeChild(QueryGraphValueEntryRange[].class, this.getClass(), classScope);
        rangeGetters.getBlock().declareVar(QueryGraphValueEntryRange[].class, "rangeGetters", newArrayByLength(QueryGraphValueEntryRange.class, constant(rangeKeyPairs.size())));
        for (int i = 0; i < rangeKeyPairs.size(); i++) {
            Class optCoercionType = optRangeCoercionTypes == null ? null : optRangeCoercionTypes[i];
            rangeGetters.getBlock().assignArrayElement(ref("rangeGetters"), constant(i), rangeKeyPairs.get(i).make(optCoercionType, rangeGetters, symbols, classScope));
        }
        rangeGetters.getBlock().methodReturn(ref("rangeGetters"));

        return Arrays.asList(hashGetter, localMethod(rangeGetters));
    }

    public String toString() {
        return "CompositeTableLookupPlan " +
            super.toString() +
            " directKeys=" + QueryGraphValueEntryHashKeyedForge.toQueryPlan(hashKeys) +
            " rangeKeys=" + QueryGraphValueEntryRangeForge.toQueryPlan(rangeKeyPairs);
    }
}
