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
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryHashKeyedForge;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryRangeForge;
import com.espertech.esper.common.internal.epl.join.queryplan.TableLookupIndexReqKey;
import com.espertech.esper.common.internal.epl.join.queryplan.TableLookupKeyDesc;
import com.espertech.esper.common.internal.epl.join.queryplan.TableLookupPlanForge;

import java.util.Collection;
import java.util.Collections;

/**
 * Plan to perform an indexed table lookup.
 */
public class SortedTableLookupPlanForge extends TableLookupPlanForge {
    private QueryGraphValueEntryRangeForge rangeKeyPair;
    private Class optionalCoercionType;

    public SortedTableLookupPlanForge(int lookupStream, int indexedStream, boolean indexedStreamIsVDW, EventType[] typesPerStream, TableLookupIndexReqKey indexNum, QueryGraphValueEntryRangeForge rangeKeyPair, Class optionalCoercionType) {
        super(lookupStream, indexedStream, indexedStreamIsVDW, typesPerStream, new TableLookupIndexReqKey[]{indexNum});
        this.rangeKeyPair = rangeKeyPair;
        this.optionalCoercionType = optionalCoercionType;
    }

    public QueryGraphValueEntryRangeForge getRangeKeyPair() {
        return rangeKeyPair;
    }

    public TableLookupKeyDesc getKeyDescriptor() {
        return new TableLookupKeyDesc(Collections.<QueryGraphValueEntryHashKeyedForge>emptyList(), Collections.singletonList(rangeKeyPair));
    }

    public String toString() {
        return "SortedTableLookupPlan " +
                super.toString() +
                " keyProperties=" + rangeKeyPair.toQueryPlan();
    }

    public Class typeOfPlanFactory() {
        return SortedTableLookupPlanFactory.class;
    }

    public Collection<CodegenExpression> additionalParams(CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        return Collections.singletonList(rangeKeyPair.make(optionalCoercionType, method, symbols, classScope));
    }
}
