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
package com.espertech.esper.common.internal.event.bean.manufacturer;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionField;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionNewAnonymousClass;
import com.espertech.esper.common.internal.context.module.EPStatementInitServices;
import com.espertech.esper.common.internal.event.core.*;
import com.espertech.esper.common.internal.event.json.compiletime.JsonUnderlyingField;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.util.JavaClassHelper;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Factory for Map-underlying events.
 */
public class EventBeanManufacturerJsonForge implements EventBeanManufacturerForge {
    private final JsonEventType jsonEventType;
    private final WriteablePropertyDescriptor[] writables;

    public EventBeanManufacturerJsonForge(JsonEventType jsonEventType, WriteablePropertyDescriptor[] writables) {
        this.jsonEventType = jsonEventType;
        this.writables = writables;
    }

    public CodegenExpression make(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMethod init = codegenClassScope.getPackageScope().getInitMethod();

        CodegenExpressionField factory = codegenClassScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
        CodegenExpressionField eventType = codegenClassScope.addFieldUnshared(true, EventType.EPTYPE, EventTypeUtility.resolveTypeCodegen(jsonEventType, EPStatementInitServices.REF));

        CodegenExpressionNewAnonymousClass manufacturer = newAnonymousClass(init.getBlock(), EventBeanManufacturer.EPTYPE);

        CodegenMethod makeUndMethod = CodegenMethod.makeParentNode(EPTypePremade.OBJECT.getEPType(), this.getClass(), codegenClassScope).addParam(EPTypePremade.OBJECTARRAY.getEPType(), "properties");
        manufacturer.addMethod("makeUnderlying", makeUndMethod);
        makeUnderlyingCodegen(makeUndMethod, codegenClassScope);

        CodegenMethod makeMethod = CodegenMethod.makeParentNode(EventBean.EPTYPE, this.getClass(), codegenClassScope).addParam(EPTypePremade.OBJECTARRAY.getEPType(), "properties");
        manufacturer.addMethod("make", makeMethod);
        makeMethod.getBlock()
            .declareVar(EPTypePremade.OBJECT.getEPType(), "und", localMethod(makeUndMethod, ref("properties")))
            .methodReturn(exprDotMethod(factory, "adapterForTypedJson", ref("und"), eventType));

        return codegenClassScope.addFieldUnshared(true, EventBeanManufacturer.EPTYPE, manufacturer);
    }

    public EventBeanManufacturer getManufacturer(EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        int[] nativeNums = EventBeanManufacturerJson.findPropertyIndexes(jsonEventType, writables);
        return new EventBeanManufacturerJson(jsonEventType, eventBeanTypedEventFactory, nativeNums);
    }

    private void makeUnderlyingCodegen(CodegenMethod method, CodegenClassScope codegenClassScope) {
        method.getBlock().declareVar(jsonEventType.getUnderlyingEPType(), "und", newInstance(jsonEventType.getUnderlyingEPType()));
        for (int i = 0; i < writables.length; i++) {
            JsonUnderlyingField field = jsonEventType.getDetail().getFieldDescriptors().get(writables[i].getPropertyName());
            CodegenExpression rhs = arrayAtIndex(ref("properties"), constant(i));
            if (field.getPropertyType().getType().isPrimitive()) {
                method.getBlock()
                    .ifCondition(notEqualsNull(rhs))
                    .assignRef(ref("und." + field.getFieldName()), cast(JavaClassHelper.getBoxedType(field.getPropertyType()), rhs));
            } else {
                method.getBlock().assignRef(ref("und." + field.getFieldName()), cast(JavaClassHelper.getBoxedType(field.getPropertyType()), rhs));
            }
        }
        method.getBlock().methodReturn(ref("und"));
    }
}
