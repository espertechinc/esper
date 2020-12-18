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
package com.espertech.esper.common.internal.context.controller.hash;

import com.espertech.esper.common.client.util.StateMgmtSetting;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.compile.stage1.spec.ContextSpecHash;
import com.espertech.esper.common.internal.compile.stage1.spec.ContextSpecHashItem;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.compile.ContextMetaData;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerFactoryEnv;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerFactoryForgeVisitor;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerForgeBase;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerPortableInfo;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.fabric.FabricCharge;

import java.util.LinkedHashMap;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.exprDotMethodChain;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

public class ContextControllerHashFactoryForge extends ContextControllerForgeBase {

    private final ContextSpecHash detail;
    private StateMgmtSetting stateMgmtSettings;

    public ContextControllerHashFactoryForge(ContextControllerFactoryEnv ctx, ContextSpecHash detail) {
        super(ctx);
        this.detail = detail;
    }

    public void validateGetContextProps(LinkedHashMap<String, Object> props, String contextName, int controllerLevel, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) throws ExprValidationException {
        // no props
    }

    public void planStateSettings(ContextMetaData detail, FabricCharge fabricCharge, int controllerLevel, String nestedContextName, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) {
        stateMgmtSettings = services.getStateMgmtSettingsProvider().context().contextHash(fabricCharge, detail, this, statementRawInfo, controllerLevel);
    }

    public CodegenMethod makeCodegen(CodegenClassScope classScope, CodegenMethodScope parent, SAIFFInitializeSymbol symbols) {
        CodegenMethod method = parent.makeChild(ContextControllerHashFactory.EPTYPE, this.getClass(), classScope);
        method.getBlock()
                .declareVar(ContextControllerHashFactory.EPTYPE, "factory", exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPStatementInitServices.GETCONTEXTSERVICEFACTORY).add("hashFactory", stateMgmtSettings.toExpression()))
                .exprDotMethod(ref("factory"), "setHashSpec", detail.makeCodegen(method, symbols, classScope))
                .methodReturn(ref("factory"));
        return method;
    }

    public ContextControllerPortableInfo getValidationInfo() {
        ContextControllerHashValidationItem[] items = new ContextControllerHashValidationItem[detail.getItems().size()];
        for (int i = 0; i < detail.getItems().size(); i++) {
            ContextSpecHashItem props = detail.getItems().get(i);
            items[i] = new ContextControllerHashValidationItem(props.getFilterSpecCompiled().getFilterForEventType());
        }
        return new ContextControllerHashValidation(items);
    }

    public boolean isPreallocate() {
        return detail.isPreallocate();
    }

    public <T> T accept(ContextControllerFactoryForgeVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
