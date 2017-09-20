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
package com.espertech.esper.epl.core.resultset.core;

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.core.resultset.codegen.ResultSetProcessorCodegenInstance;

public interface ResultSetProcessorFactoryForge {
    ResultSetProcessorFactory getResultSetProcessorFactory(StatementContext stmtContext, boolean isFireAndForget);

    Class getInterfaceClass();

    void instanceCodegen(ResultSetProcessorCodegenInstance instance, CodegenClassScope classScope);

    void processViewResultCodegen(CodegenClassScope classScope, CodegenMethodNode method, ResultSetProcessorCodegenInstance instance);

    void processJoinResultCodegen(CodegenClassScope classScope, CodegenMethodNode method, ResultSetProcessorCodegenInstance instance);

    void getIteratorViewCodegen(CodegenClassScope classScope, CodegenMethodNode method, ResultSetProcessorCodegenInstance instance);

    void getIteratorJoinCodegen(CodegenClassScope classScope, CodegenMethodNode method, ResultSetProcessorCodegenInstance instance);

    void processOutputLimitedViewCodegen(CodegenClassScope classScope, CodegenMethodNode method, ResultSetProcessorCodegenInstance instance);

    void processOutputLimitedJoinCodegen(CodegenClassScope classScope, CodegenMethodNode method, ResultSetProcessorCodegenInstance instance);

    void applyViewResultCodegen(CodegenClassScope classScope, CodegenMethodNode method, ResultSetProcessorCodegenInstance instance);

    void applyJoinResultCodegen(CodegenClassScope classScope, CodegenMethodNode method, ResultSetProcessorCodegenInstance instance);

    void continueOutputLimitedLastAllNonBufferedViewCodegen(CodegenClassScope classScope, CodegenMethodNode method, ResultSetProcessorCodegenInstance instance);

    void continueOutputLimitedLastAllNonBufferedJoinCodegen(CodegenClassScope classScope, CodegenMethodNode method, ResultSetProcessorCodegenInstance instance);

    void processOutputLimitedLastAllNonBufferedViewCodegen(CodegenClassScope classScope, CodegenMethodNode method, ResultSetProcessorCodegenInstance instance);

    void processOutputLimitedLastAllNonBufferedJoinCodegen(CodegenClassScope classScope, CodegenMethodNode method, ResultSetProcessorCodegenInstance instance);

    void acceptHelperVisitorCodegen(CodegenClassScope classScope, CodegenMethodNode method, ResultSetProcessorCodegenInstance instance);

    void stopMethodCodegen(CodegenClassScope classScope, CodegenMethodNode method, ResultSetProcessorCodegenInstance instance);

    void clearMethodCodegen(CodegenClassScope classScope, CodegenMethodNode method);
}
