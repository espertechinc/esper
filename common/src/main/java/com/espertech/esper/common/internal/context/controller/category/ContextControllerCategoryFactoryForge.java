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
package com.espertech.esper.common.internal.context.controller.category;

import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.client.util.StateMgmtSetting;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.compile.stage1.spec.ContextSpecCategory;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.compile.ContextMetaData;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerFactoryEnv;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerFactoryForgeVisitor;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerForgeBase;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerPortableInfo;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.context.util.ContextPropertyEventType;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.fabric.FabricCharge;

import java.util.LinkedHashMap;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class ContextControllerCategoryFactoryForge extends ContextControllerForgeBase {

    private final ContextSpecCategory detail;
    private StateMgmtSetting stateMgmtSettings;

    public ContextControllerCategoryFactoryForge(ContextControllerFactoryEnv ctx, ContextSpecCategory detail) {
        super(ctx);
        this.detail = detail;
    }

    public void validateGetContextProps(LinkedHashMap<String, Object> props, String contextName, int controllerLevel, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) throws ExprValidationException {
        if (detail.getItems().isEmpty()) {
            throw new ExprValidationException("Empty list of partition items");
        }
        props.put(ContextPropertyEventType.PROP_CTX_LABEL, EPTypePremade.STRING.getEPType());
    }

    public void planStateSettings(ContextMetaData detail, FabricCharge fabricCharge, int controllerLevel, String nestedContextName, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) {
        stateMgmtSettings = services.getStateMgmtSettingsProvider().context().contextCategory(fabricCharge, detail, this, statementRawInfo, controllerLevel);
    }

    public CodegenMethod makeCodegen(CodegenClassScope classScope, CodegenMethodScope parent, SAIFFInitializeSymbol symbols) {
        CodegenMethod method = parent.makeChild(ContextControllerCategoryFactory.EPTYPE, ContextControllerCategoryFactoryForge.class, classScope);
        method.getBlock()
                .declareVar(ContextControllerCategoryFactory.EPTYPE, "factory", exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPStatementInitServices.GETCONTEXTSERVICEFACTORY).add("categoryFactory", stateMgmtSettings.toExpression()))
                .exprDotMethod(ref("factory"), "setContextName", constant(ctx.getContextName()))
                .exprDotMethod(ref("factory"), "setCategorySpec", detail.makeCodegen(method, symbols, classScope))
                .methodReturn(ref("factory"));
        return method;
    }

    public ContextControllerPortableInfo getValidationInfo() {
        return new ContextControllerCategoryValidation(detail.getFilterSpecCompiled().getFilterForEventType());
    }

    public <T> T accept(ContextControllerFactoryForgeVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
