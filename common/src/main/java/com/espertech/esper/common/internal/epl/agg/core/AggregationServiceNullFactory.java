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
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.common.internal.context.util.AgentInstanceContext;
import com.espertech.esper.common.internal.settings.ClasspathImportServiceRuntime;

import java.util.List;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * A null object implementation of the AggregationService
 * interface.
 */
public class AggregationServiceNullFactory implements AggregationServiceFactory, AggregationServiceFactoryForgeWMethodGen {

    public final static AggregationServiceNullFactory INSTANCE = new AggregationServiceNullFactory();

    private AggregationServiceNullFactory() {
    }

    public AggregationService makeService(AgentInstanceContext agentInstanceContext, ClasspathImportServiceRuntime classpathImportService, boolean isSubquery, Integer subqueryNumber, int[] groupId) {
        return AggregationServiceNull.INSTANCE;
    }

    public void providerCodegen(CodegenMethod method, CodegenClassScope classScope, AggregationClassNames classNames) {
        method.getBlock().methodReturn(CodegenExpressionBuilder.newInstance(classNames.getServiceFactory(), ref("this")));
    }

    public AggregationCodegenRowLevelDesc getRowLevelDesc() {
        return AggregationCodegenRowLevelDesc.EMPTY;
    }

    public void makeServiceCodegen(CodegenMethod method, CodegenClassScope classScope, AggregationClassNames classNames) {
        method.getBlock().methodReturn(publicConstValue(AggregationServiceNull.class, "INSTANCE"));
    }

    public void ctorCodegen(CodegenCtor ctor, List<CodegenTypedParam> explicitMembers, CodegenClassScope classScope, AggregationClassNames classNames) {
    }

    public void getValueCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        method.getBlock().methodReturn(constantNull());
    }

    public void getEventBeanCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        method.getBlock().methodReturn(constantNull());
    }

    public void getCollectionOfEventsCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        method.getBlock().methodReturn(constantNull());
    }

    public void applyEnterCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods, AggregationClassNames classNames) {
    }

    public void applyLeaveCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods, AggregationClassNames classNames) {
    }

    public void stopMethodCodegen(AggregationServiceFactoryForgeWMethodGen forge, CodegenMethod method) {
    }

    public void rowCtorCodegen(AggregationRowCtorDesc rowCtorDesc) {
    }

    public void setRemovedCallbackCodegen(CodegenMethod method) {
    }

    public void setCurrentAccessCodegen(CodegenMethod method, CodegenClassScope classScope, AggregationClassNames classNames) {
    }

    public void clearResultsCodegen(CodegenMethod method, CodegenClassScope classScope) {
    }

    public void getCollectionScalarCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        method.getBlock().methodReturn(constantNull());
    }

    public void acceptCodegen(CodegenMethod method, CodegenClassScope classScope) {
    }

    public void getGroupKeysCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().methodReturn(constantNull());
    }

    public void getGroupKeyCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().methodReturn(constantNull());
    }

    public void acceptGroupDetailCodegen(CodegenMethod method, CodegenClassScope classScope) {
    }

    public void isGroupedCodegen(CodegenMethod method, CodegenClassScope classScope) {
        method.getBlock().methodReturn(constantFalse());
    }

    public void rowWriteMethodCodegen(CodegenMethod method, int level) {
    }

    public void rowReadMethodCodegen(CodegenMethod method, int level) {
    }

    public void getRowCodegen(CodegenMethod method, CodegenClassScope classScope, CodegenNamedMethods namedMethods) {
        method.getBlock().methodThrowUnsupported();
    }
}
