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
import com.espertech.esper.common.internal.event.map.MapEventType;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

/**
 * Factory for Map-underlying events.
 */
public class EventBeanManufacturerMapForge implements EventBeanManufacturerForge {
    private final MapEventType mapEventType;
    private final WriteablePropertyDescriptor[] writables;

    public EventBeanManufacturerMapForge(MapEventType mapEventType, WriteablePropertyDescriptor[] writables) {
        this.mapEventType = mapEventType;
        this.writables = writables;
    }

    public CodegenExpression make(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMethod init = codegenClassScope.getPackageScope().getInitMethod();

        CodegenExpressionField factory = codegenClassScope.addOrGetFieldSharable(EventBeanTypedEventFactoryCodegenField.INSTANCE);
        CodegenExpressionField eventType = codegenClassScope.addFieldUnshared(true, EventType.EPTYPE, EventTypeUtility.resolveTypeCodegen(mapEventType, EPStatementInitServices.REF));

        CodegenExpressionNewAnonymousClass manufacturer = newAnonymousClass(init.getBlock(), EventBeanManufacturer.EPTYPE);

        CodegenMethod makeUndMethod = CodegenMethod.makeParentNode(EPTypePremade.MAP.getEPType(), this.getClass(), codegenClassScope).addParam(EPTypePremade.OBJECTARRAY.getEPType(), "properties");
        manufacturer.addMethod("makeUnderlying", makeUndMethod);
        makeUnderlyingCodegen(makeUndMethod, codegenClassScope);

        CodegenMethod makeMethod = CodegenMethod.makeParentNode(EventBean.EPTYPE, this.getClass(), codegenClassScope).addParam(EPTypePremade.OBJECTARRAY.getEPType(), "properties");
        manufacturer.addMethod("make", makeMethod);
        makeMethod.getBlock()
                .declareVar(EPTypePremade.MAP.getEPType(), "und", localMethod(makeUndMethod, ref("properties")))
                .methodReturn(exprDotMethod(factory, "adapterForTypedMap", ref("und"), eventType));

        return codegenClassScope.addFieldUnshared(true, EventBeanManufacturer.EPTYPE, manufacturer);
    }

    public EventBeanManufacturer getManufacturer(EventBeanTypedEventFactory eventBeanTypedEventFactory) {
        return new EventBeanManufacturerMap(mapEventType, eventBeanTypedEventFactory, writables);
    }

    private void makeUnderlyingCodegen(CodegenMethod method, CodegenClassScope codegenClassScope) {
        method.getBlock().declareVar(EPTypePremade.MAP.getEPType(), "values", newInstance(EPTypePremade.HASHMAP.getEPType()));
        for (int i = 0; i < writables.length; i++) {
            method.getBlock().exprDotMethod(ref("values"), "put", constant(writables[i].getPropertyName()), arrayAtIndex(ref("properties"), constant(i)));
        }
        method.getBlock().methodReturn(ref("values"));
    }
}
