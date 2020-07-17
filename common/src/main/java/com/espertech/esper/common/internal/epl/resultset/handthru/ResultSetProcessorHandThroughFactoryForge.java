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
package com.espertech.esper.common.internal.epl.resultset.handthru;

import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenInstanceAux;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenTypedParam;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessor;
import com.espertech.esper.common.internal.epl.resultset.core.ResultSetProcessorFactoryForge;

import java.util.List;

/**
 * Result set processor prototype for the hand-through case:
 * no aggregation functions used in the select clause, and no group-by, no having and ordering.
 */
public class ResultSetProcessorHandThroughFactoryForge implements ResultSetProcessorFactoryForge {
    private final boolean isSelectRStream;

    public ResultSetProcessorHandThroughFactoryForge(boolean selectRStream) {
        this.isSelectRStream = selectRStream;
    }

    public boolean isSelectRStream() {
        return isSelectRStream;
    }

    public EPTypeClass getInterfaceClass() {
        return ResultSetProcessor.EPTYPE;
    }

    public void instanceCodegen(CodegenInstanceAux instance, CodegenClassScope classScope, CodegenCtor factoryCtor, List<CodegenTypedParam> factoryMembers) {
        throw notImplemented();
    }

    public void processViewResultCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        throw notImplemented();
    }

    public void processJoinResultCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        throw notImplemented();
    }

    public void getIteratorViewCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        throw notImplemented();
    }

    public void getIteratorJoinCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        throw notImplemented();
    }

    public void processOutputLimitedViewCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        throw notImplemented();
    }

    public void processOutputLimitedJoinCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        throw notImplemented();
    }

    public void applyViewResultCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        throw notImplemented();
    }

    public void applyJoinResultCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        throw notImplemented();
    }

    public void continueOutputLimitedLastAllNonBufferedViewCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        throw notImplemented();
    }

    public void continueOutputLimitedLastAllNonBufferedJoinCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        throw notImplemented();
    }

    public void processOutputLimitedLastAllNonBufferedViewCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        throw notImplemented();
    }

    public void processOutputLimitedLastAllNonBufferedJoinCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        throw notImplemented();
    }

    public void acceptHelperVisitorCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        throw notImplemented();
    }

    public void stopMethodCodegen(CodegenClassScope classScope, CodegenMethod method, CodegenInstanceAux instance) {
        throw notImplemented();
    }

    public void clearMethodCodegen(CodegenClassScope classScope, CodegenMethod method) {
        throw notImplemented();
    }

    public String getInstrumentedQName() {
        return "ResultSetProcessSimple";
    }

    private UnsupportedOperationException notImplemented() {
        throw new UnsupportedOperationException("Implemented by " + ResultSetProcessorHandThroughImpl.class);
    }
}
