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
package com.espertech.esper.common.internal.epl.agg.core;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenCtor;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenNamedMethods;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenTypedParam;

import java.util.List;

public interface AggregationServiceFactoryForgeWMethodGen extends AggregationServiceFactoryForge {
    AggregationCodegenRowLevelDesc getRowLevelDesc();

    void providerCodegen(CodegenMethod method, CodegenClassScope classScope, AggregationClassNames classNames);

    void rowCtorCodegen(AggregationRowCtorDesc rowCtorDesc);

    void rowWriteMethodCodegen(CodegenMethod method, int level);

    void rowReadMethodCodegen(CodegenMethod method, int level);

    void makeServiceCodegen(CodegenMethod method, CodegenClassScope classScope, AggregationClassNames classNames);

    void ctorCodegen(CodegenCtor ctor, List<CodegenTypedParam> explicitMembers, CodegenClassScope classScope, AggregationClassNames classNames);

    void getValueCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods);

    void getCollectionOfEventsCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods);

    void getCollectionScalarCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods);

    void getEventBeanCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods);

    void getRowCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods);

    void applyEnterCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods, AggregationClassNames classNames);

    void applyLeaveCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods, AggregationClassNames classNames);

    void stopMethodCodegen(AggregationServiceFactoryForgeWMethodGen forge, CodegenMethod method);

    void setRemovedCallbackCodegen(CodegenMethod method);

    void setCurrentAccessCodegen(CodegenMethod method, CodegenClassScope classScope, AggregationClassNames classNames);

    void clearResultsCodegen(CodegenMethod method, CodegenClassScope classScope);

    void acceptCodegen(CodegenMethod method, CodegenClassScope classScope);

    void getGroupKeysCodegen(CodegenMethod method, CodegenClassScope classScope);

    void getGroupKeyCodegen(CodegenMethod method, CodegenClassScope classScope);

    void acceptGroupDetailCodegen(CodegenMethod method, CodegenClassScope classScope);

    void isGroupedCodegen(CodegenMethod method, CodegenClassScope classScope);
}
