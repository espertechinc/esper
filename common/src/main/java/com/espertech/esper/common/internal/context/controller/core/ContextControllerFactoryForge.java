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
package com.espertech.esper.common.internal.context.controller.core;

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.compile.ContextMetaData;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.fabric.FabricCharge;

import java.util.LinkedHashMap;

public interface ContextControllerFactoryForge {
    ContextControllerFactoryEnv getFactoryEnv();

    void validateGetContextProps(LinkedHashMap<String, Object> props, String contextName, int controllerLevel, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) throws ExprValidationException;

    void planStateSettings(ContextMetaData detail, FabricCharge fabricCharge, int controllerLevel, String nestedContextName, StatementRawInfo statementRawInfo, StatementCompileTimeServices services);

    CodegenMethod makeCodegen(CodegenClassScope classScope, CodegenMethodScope parent, SAIFFInitializeSymbol symbols);

    ContextControllerPortableInfo getValidationInfo();

    <T> T accept(ContextControllerFactoryForgeVisitor<T> visitor);
}
