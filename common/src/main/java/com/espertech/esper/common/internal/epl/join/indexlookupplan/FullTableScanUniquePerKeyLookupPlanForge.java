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
 * Plan for a full table scan.
 */
public class FullTableScanUniquePerKeyLookupPlanForge extends TableLookupPlanForge {
    public FullTableScanUniquePerKeyLookupPlanForge(int lookupStream, int indexedStream, boolean indexedStreamIsVDW, EventType[] typesPerStream, TableLookupIndexReqKey indexNum) {
        super(lookupStream, indexedStream, indexedStreamIsVDW, typesPerStream, new TableLookupIndexReqKey[]{indexNum});
    }

    public TableLookupKeyDesc getKeyDescriptor() {
        return new TableLookupKeyDesc(Collections.<QueryGraphValueEntryHashKeyedForge>emptyList(), Collections.<QueryGraphValueEntryRangeForge>emptyList());
    }

    public Class typeOfPlanFactory() {
        return FullTableScanUniquePerKeyLookupPlan.class;
    }

    public Collection<CodegenExpression> additionalParams(CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        return Collections.emptyList();
    }

    public String toString() {
        return "FullTableScanLookupPlan " +
                super.toString();
    }

}
