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
package com.espertech.esper.event.map;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.event.BaseNestableEventUtil;
import com.espertech.esper.event.EventAdapterService;

import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class MapNestedEntryPropertyGetterPropertyProvidedDynamic extends MapNestedEntryPropertyGetterBase {

    private final EventPropertyGetter nestedGetter;

    public MapNestedEntryPropertyGetterPropertyProvidedDynamic(String propertyMap, EventType fragmentType, EventAdapterService eventAdapterService, EventPropertyGetter nestedGetter) {
        super(propertyMap, fragmentType, eventAdapterService);
        this.nestedGetter = nestedGetter;
    }

    @Override
    public boolean isExistsProperty(EventBean eventBean) {
        return isExistsProperty(BaseNestableEventUtil.checkedCastUnderlyingMap(eventBean));
    }

    public Object handleNestedValue(Object value) {
        if (!(value instanceof Map)) {
            return null;
        }
        if (nestedGetter instanceof MapEventPropertyGetter) {
            return ((MapEventPropertyGetter) nestedGetter).getMap((Map<String, Object>) value);
        }
        return null;
    }

    private CodegenMethodNode handleNestedValueCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenBlock block = codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(Object.class, "value").getBlock()
                .ifRefNotTypeReturnConst("value", Map.class, "null");
        if (nestedGetter instanceof MapEventPropertyGetter) {
            return block.methodReturn(((MapEventPropertyGetter) nestedGetter).underlyingGetCodegen(cast(Map.class, ref("value")), codegenMethodScope, codegenClassScope));
        }
        return block.methodReturn(constantNull());
    }

    private boolean isExistsProperty(Map map) {
        Object value = map.get(propertyMap);
        if (value == null || !(value instanceof Map)) {
            return false;
        }
        if (nestedGetter instanceof MapEventPropertyGetter) {
            return ((MapEventPropertyGetter) nestedGetter).isMapExistsProperty((Map) value);
        }
        return false;
    }

    private CodegenMethodNode isExistsPropertyCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenBlock block = codegenMethodScope.makeChild(boolean.class, this.getClass(), codegenClassScope).addParam(Map.class, "map").getBlock()
                .declareVar(Object.class, "value", exprDotMethod(ref("map"), "get", constant(propertyMap)))
                .ifRefNullReturnFalse("value")
                .ifRefNotTypeReturnConst("value", Map.class, false);
        if (nestedGetter instanceof MapEventPropertyGetter) {
            return block.methodReturn(((MapEventPropertyGetter) nestedGetter).underlyingExistsCodegen(cast(Map.class, ref("value")), codegenMethodScope, codegenClassScope));
        }
        return block.methodReturn(constantFalse());
    }

    public Object handleNestedValueFragment(Object value) {
        return null;
    }

    public CodegenExpression handleNestedValueCodegen(CodegenExpression valueExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(handleNestedValueCodegen(codegenMethodScope, codegenClassScope), valueExpression);
    }

    public CodegenExpression handleNestedValueFragmentCodegen(CodegenExpression name, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    @Override
    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingExistsCodegen(castUnderlying(Map.class, beanExpression), codegenMethodScope, codegenClassScope);
    }

    @Override
    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(isExistsPropertyCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }
}
