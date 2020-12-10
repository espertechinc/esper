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
package com.espertech.esper.common.internal.context.controller.keyed;

import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.util.StateMgmtSetting;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.compile.stage1.spec.ContextSpecConditionFilter;
import com.espertech.esper.common.internal.compile.stage1.spec.ContextSpecKeyed;
import com.espertech.esper.common.internal.compile.stage1.spec.ContextSpecKeyedItem;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.compile.ContextMetaData;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerFactoryEnv;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerForgeBase;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerPortableInfo;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.context.util.ContextPropertyEventType;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.fabric.FabricCharge;
import com.espertech.esper.common.internal.statemgmtsettings.StateMgmtSettingDefault;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.exprDotMethodChain;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

public class ContextControllerKeyedFactoryForge extends ContextControllerForgeBase {

    private final ContextSpecKeyed detail;
    private StateMgmtSetting terminationStateMgmtSettings = StateMgmtSettingDefault.INSTANCE;
    private StateMgmtSetting ctxStateMgmtSettings;

    public ContextControllerKeyedFactoryForge(ContextControllerFactoryEnv ctx, ContextSpecKeyed detail) throws ExprValidationException {
        super(ctx);
        this.detail = detail;
    }

    public void validateGetContextProps(LinkedHashMap<String, Object> props, String contextName, int controllerLevel, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) throws ExprValidationException {
        EPType[] propertyTypes = ContextControllerKeyedUtil.validateContextDesc(contextName, detail);

        for (int i = 0; i < detail.getItems().get(0).getPropertyNames().size(); i++) {
            String propertyName = ContextPropertyEventType.PROP_CTX_KEY_PREFIX + (i + 1);
            props.put(propertyName, propertyTypes[i]);
        }

        LinkedHashSet<String> allTags = new LinkedHashSet<>();
        for (ContextSpecKeyedItem item : detail.getItems()) {
            if (item.getAliasName() != null) {
                allTags.add(item.getAliasName());
            }
        }

        if (detail.getOptionalInit() != null) {
            for (ContextSpecConditionFilter filter : detail.getOptionalInit()) {
                ContextPropertyEventType.addEndpointTypes(filter, props, allTags);
            }
        }

        if (detail.getOptionalTermination() != null) {
            ContextPropertyEventType.addEndpointTypes(detail.getOptionalTermination(), props, allTags);
        }
    }

    public void planStateSettings(ContextMetaData detail, FabricCharge fabricCharge, int controllerLevel, String nestedContextName, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) {
        if (this.detail.getOptionalTermination() != null) {
            terminationStateMgmtSettings = services.getStateMgmtSettingsProvider().context().contextKeyedTerm(fabricCharge, detail, this, statementRawInfo, controllerLevel);
        }
        ctxStateMgmtSettings = services.getStateMgmtSettingsProvider().context().contextKeyed(fabricCharge, detail, this, statementRawInfo, controllerLevel);
    }

    public CodegenMethod makeCodegen(CodegenClassScope classScope, CodegenMethodScope parent, SAIFFInitializeSymbol symbols) {
        CodegenMethod method = parent.makeChild(ContextControllerKeyedFactory.EPTYPE, this.getClass(), classScope);
        method.getBlock()
            .declareVar(ContextControllerKeyedFactory.EPTYPE, "factory", exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPStatementInitServices.GETCONTEXTSERVICEFACTORY).add("keyedFactory", terminationStateMgmtSettings.toExpression(), ctxStateMgmtSettings.toExpression()))
            .exprDotMethod(ref("factory"), "setKeyedSpec", detail.makeCodegen(method, symbols, classScope))
            .methodReturn(ref("factory"));
        return method;
    }

    public ContextControllerPortableInfo getValidationInfo() {
        ContextControllerKeyedValidationItem[] items = new ContextControllerKeyedValidationItem[detail.getItems().size()];
        for (int i = 0; i < detail.getItems().size(); i++) {
            ContextSpecKeyedItem props = detail.getItems().get(i);
            items[i] = new ContextControllerKeyedValidationItem(props.getFilterSpecCompiled().getFilterForEventType(), props.getPropertyNames().toArray(new String[props.getPropertyNames().size()]));
        }
        return new ContextControllerKeyedValidation(items);
    }

    public ContextSpecKeyed getDetail() {
        return detail;
    }
}
