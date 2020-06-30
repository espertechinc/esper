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
package com.espertech.esper.common.internal.avro.getter;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.PropertyAccessException;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.avro.core.AvroConstant;
import com.espertech.esper.common.internal.avro.core.AvroEventPropertyGetter;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenBlock;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenClassScope;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethod;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenMethodScope;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpression;
import com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder;
import com.espertech.esper.common.internal.event.core.EventPropertyGetterSPI;
import org.apache.avro.generic.GenericData;

import static com.espertech.esper.common.internal.bytecodemodel.model.expression.CodegenExpressionBuilder.*;

public class AvroEventBeanGetterNestedIndexRootedMultilevel implements EventPropertyGetterSPI {
    private final int posTop;
    private final int index;
    private final AvroEventPropertyGetter[] nested;

    public AvroEventBeanGetterNestedIndexRootedMultilevel(int posTop, int index, AvroEventPropertyGetter[] nested) {
        this.posTop = posTop;
        this.index = index;
        this.nested = nested;
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        GenericData.Record value = navigate((GenericData.Record) eventBean.getUnderlying());
        if (value == null) {
            return null;
        }
        return nested[nested.length - 1].getAvroFieldValue(value);
    }

    private CodegenMethod getCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(EPTypePremade.OBJECT.getEPType(), this.getClass(), codegenClassScope).addParam(AvroConstant.EPTYPE_RECORD, "record").getBlock()
                .declareVar(AvroConstant.EPTYPE_RECORD, "value", localMethod(navigateMethodCodegen(codegenMethodScope, codegenClassScope), ref("record")))
                .ifRefNullReturnNull("value")
                .methodReturn(nested[nested.length - 1].underlyingGetCodegen(ref("value"), codegenMethodScope, codegenClassScope));
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true;
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        GenericData.Record value = navigate((GenericData.Record) eventBean.getUnderlying());
        if (value == null) {
            return null;
        }
        return nested[nested.length - 1].getAvroFragment(value);
    }

    private CodegenMethod getFragmentCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return codegenMethodScope.makeChild(EPTypePremade.OBJECT.getEPType(), this.getClass(), codegenClassScope).addParam(AvroConstant.EPTYPE_RECORD, "record").getBlock()
                .declareVar(AvroConstant.EPTYPE_RECORD, "value", localMethod(navigateMethodCodegen(codegenMethodScope, codegenClassScope), ref("record")))
                .ifRefNullReturnNull("value")
                .methodReturn(nested[nested.length - 1].underlyingFragmentCodegen(ref("value"), codegenMethodScope, codegenClassScope));
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(AvroConstant.EPTYPE_RECORD, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantTrue();
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingFragmentCodegen(castUnderlying(AvroConstant.EPTYPE_RECORD, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantTrue();
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getFragmentCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }

    private GenericData.Record navigate(GenericData.Record record) {
        Object value = AvroEventBeanGetterNestedIndexRooted.getAtIndex(record, posTop, index);
        if (value == null) {
            return null;
        }
        return navigateRecord((GenericData.Record) value);
    }

    private CodegenMethod navigateMethodCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMethod navigateRecordMethod = navigateRecordMethodCodegen(codegenMethodScope, codegenClassScope);
        return codegenMethodScope.makeChild(AvroConstant.EPTYPE_RECORD, this.getClass(), codegenClassScope).addParam(AvroConstant.EPTYPE_RECORD, "record").getBlock()
                .declareVar(EPTypePremade.OBJECT.getEPType(), "value", staticMethod(AvroEventBeanGetterNestedIndexRooted.class, "getAtIndex", ref("record"), constant(posTop), constant(index)))
                .ifRefNullReturnNull("value")
                .methodReturn(CodegenExpressionBuilder.localMethod(navigateRecordMethod, castRef(AvroConstant.EPTYPE_RECORD, "value")));
    }

    private GenericData.Record navigateRecord(GenericData.Record record) {
        GenericData.Record current = record;
        for (int i = 0; i < nested.length - 1; i++) {
            Object value = nested[i].getAvroFieldValue(current);
            if (!(value instanceof GenericData.Record)) {
                return null;
            }
            current = (GenericData.Record) value;
        }
        return current;
    }

    private CodegenMethod navigateRecordMethodCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenBlock block = codegenMethodScope.makeChild(AvroConstant.EPTYPE_RECORD, this.getClass(), codegenClassScope).addParam(AvroConstant.EPTYPE_RECORD, "record").getBlock()
                .declareVar(AvroConstant.EPTYPE_RECORD, "current", ref("record"))
                .declareVarNull(EPTypePremade.OBJECT.getEPType(), "value");
        for (int i = 0; i < nested.length - 1; i++) {
            block.assignRef("value", nested[i].underlyingGetCodegen(ref("current"), codegenMethodScope, codegenClassScope))
                    .ifRefNotTypeReturnConst("value", AvroConstant.EPTYPE_RECORD, null)
                    .assignRef("current", castRef(AvroConstant.EPTYPE_RECORD, "value"));
        }
        return block.methodReturn(ref("current"));
    }
}
