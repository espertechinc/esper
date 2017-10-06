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
package com.espertech.esper.epl.agg.service.common;

import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.codegen.core.CodegenCtor;
import com.espertech.esper.codegen.core.CodegenNamedMethods;
import com.espertech.esper.codegen.core.CodegenTypedParam;
import com.espertech.esper.core.service.StatementContext;
import com.espertech.esper.epl.agg.codegen.AggregationCodegenRowLevelDesc;

import java.util.List;

public interface AggregationServiceFactoryForge {
    AggregationServiceFactory getAggregationServiceFactory(StatementContext stmtContext, boolean isFireAndForget);
    AggregationCodegenRowLevelDesc getRowLevelDesc();
    void rowCtorCodegen(CodegenClassScope classScope, CodegenCtor rowCtor, List<CodegenTypedParam> rowMembers, CodegenNamedMethods namedMethods);
    void makeServiceCodegen(CodegenMethodNode method, CodegenClassScope classScope);
    void ctorCodegen(CodegenCtor ctor, List<CodegenTypedParam> explicitMembers, CodegenClassScope classScope);
    void getValueCodegen(CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods);
    void getCollectionOfEventsCodegen(CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods);
    void getCollectionScalarCodegen(CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods);
    void getEventBeanCodegen(CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods);
    void applyEnterCodegen(CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods);
    void applyLeaveCodegen(CodegenMethodNode method, CodegenClassScope classScope, CodegenNamedMethods namedMethods);
    void stopMethodCodegen(AggregationServiceFactoryForge forge, CodegenMethodNode method);
    void setRemovedCallbackCodegen(CodegenMethodNode method);
    void setCurrentAccessCodegen(CodegenMethodNode method, CodegenClassScope classScope);
    void clearResultsCodegen(CodegenMethodNode method, CodegenClassScope classScope);
    void acceptCodegen(CodegenMethodNode method, CodegenClassScope classScope);
    void getGroupKeysCodegen(CodegenMethodNode method, CodegenClassScope classScope);
    void getGroupKeyCodegen(CodegenMethodNode method, CodegenClassScope classScope);
    void acceptGroupDetailCodegen(CodegenMethodNode method, CodegenClassScope classScope);
    void isGroupedCodegen(CodegenMethodNode method, CodegenClassScope classScope);
}
