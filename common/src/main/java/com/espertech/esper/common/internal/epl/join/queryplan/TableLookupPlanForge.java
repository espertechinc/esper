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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenMakeable;
import com.espertech.esper.common.internal.bytecodemodel.util.CodegenMakeableUtil;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityCodegen;
import com.espertech.esper.common.internal.epl.expression.core.ExprNodeUtilityQuery;
import com.espertech.esper.common.internal.epl.join.querygraph.QueryGraphValueEntryRangeForge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Abstract specification on how to perform a table lookup.
 */
public abstract class TableLookupPlanForge implements CodegenMakeable<SAIFFInitializeSymbol> {
    protected final int lookupStream;
    protected final int indexedStream;
    protected boolean indexedStreamIsVDW;
    protected EventType[] typesPerStream;
    private TableLookupIndexReqKey[] indexNum;

    public abstract TableLookupKeyDesc getKeyDescriptor();

    public abstract Class typeOfPlanFactory();

    public abstract Collection<CodegenExpression> additionalParams(CodegenMethod method, SAIFFInitializeSymbol symbols, CodegenClassScope classScope);

    /**
     * Ctor.
     *
     * @param lookupStream       - stream number of stream that supplies event to be used to look up
     * @param indexedStream      - - stream number of stream that is being access via index/table
     * @param indexedStreamIsVDW - vdw indicators
     * @param typesPerStream     types
     * @param indexNum           - index to use for lookup
     */
    protected TableLookupPlanForge(int lookupStream, int indexedStream, boolean indexedStreamIsVDW, EventType[] typesPerStream, TableLookupIndexReqKey[] indexNum) {
        this.lookupStream = lookupStream;
        this.indexedStream = indexedStream;
        this.indexedStreamIsVDW = indexedStreamIsVDW;
        this.indexNum = indexNum;
        this.typesPerStream = typesPerStream;
    }

    /**
     * Returns the lookup stream.
     *
     * @return lookup stream
     */
    public int getLookupStream() {
        return lookupStream;
    }

    /**
     * Returns indexed stream.
     *
     * @return indexed stream
     */
    public int getIndexedStream() {
        return indexedStream;
    }

    /**
     * Returns index number to use for looking up in.
     *
     * @return index number
     */
    public TableLookupIndexReqKey[] getIndexNum() {
        return indexNum;
    }

    public String toString() {
        return "lookupStream=" + lookupStream +
                " indexedStream=" + indexedStream +
                " indexNum=" + Arrays.toString(indexNum);
    }

    public CodegenExpression make(CodegenMethodScope parent, SAIFFInitializeSymbol symbols, CodegenClassScope classScope) {
        CodegenMethod method = parent.makeChild(typeOfPlanFactory(), this.getClass(), classScope);
        List<CodegenExpression> params = new ArrayList<>(6);
        params.add(constant(lookupStream));
        params.add(constant(indexedStream));
        params.add(CodegenMakeableUtil.makeArray("reqIdxKeys", TableLookupIndexReqKey.class, indexNum, this.getClass(), method, symbols, classScope));
        params.addAll(additionalParams(method, symbols, classScope));
        method.getBlock()
                .declareVar(typeOfPlanFactory(), "plan", newInstance(typeOfPlanFactory(), params.toArray(new CodegenExpression[params.size()])));

        // inject additional information for virtual data windows
        if (indexedStreamIsVDW) {
            TableLookupKeyDesc keyDesc = getKeyDescriptor();
            ExprNode[] hashes = keyDesc.getHashExpressions();
            QueryGraphValueEntryRangeForge[] ranges = keyDesc.getRanges().toArray(new QueryGraphValueEntryRangeForge[keyDesc.getRanges().size()]);
            Class[] rangeResults = QueryGraphValueEntryRangeForge.getRangeResultTypes(ranges);
            method.getBlock()
                    .exprDotMethod(ref("plan"), "setVirtualDWHashEvals", ExprNodeUtilityCodegen.codegenEvaluators(hashes, method, this.getClass(), classScope))
                    .exprDotMethod(ref("plan"), "setVirtualDWHashTypes", constant(ExprNodeUtilityQuery.getExprResultTypes(hashes)))
                    .exprDotMethod(ref("plan"), "setVirtualDWRangeEvals", QueryGraphValueEntryRangeForge.makeArray(ranges, method, symbols, classScope))
                    .exprDotMethod(ref("plan"), "setVirtualDWRangeTypes", constant(rangeResults));
        }

        method.getBlock().methodReturn(ref("plan"));
        return localMethod(method);
    }
}
