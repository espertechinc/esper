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

import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.compile.stage1.spec.ContextSpecConditionFilter;
import com.espertech.esper.common.internal.compile.stage1.spec.ContextSpecKeyed;
import com.espertech.esper.common.internal.compile.stage1.spec.ContextSpecKeyedItem;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.aifactory.core.SAIFFInitializeSymbol;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerFactoryEnv;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerForgeBase;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerPortableInfo;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.context.util.ContextPropertyEventType;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.exprDotMethodChain;
import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.ref;

public class ContextControllerKeyedFactoryForge extends ContextControllerForgeBase {

    private final ContextSpecKeyed detail;

    public ContextControllerKeyedFactoryForge(ContextControllerFactoryEnv ctx, ContextSpecKeyed detail) throws ExprValidationException {
        super(ctx);
        this.detail = detail;
    }

    public void validateGetContextProps(LinkedHashMap<String, Object> props, String contextName, StatementRawInfo statementRawInfo, StatementCompileTimeServices services) throws ExprValidationException {
        Class[] propertyTypes = ContextControllerKeyedUtil.validateContextDesc(contextName, detail);

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
    }

    public CodegenMethod makeCodegen(CodegenClassScope classScope, CodegenMethodScope parent, SAIFFInitializeSymbol symbols) {
        CodegenMethod method = parent.makeChild(ContextControllerKeyedFactory.class, this.getClass(), classScope);
        method.getBlock()
                .declareVar(ContextControllerKeyedFactory.class, "factory", exprDotMethodChain(symbols.getAddInitSvc(method)).add(EPStatementInitServices.GETCONTEXTSERVICEFACTORY).add("keyedFactory"))
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
}
