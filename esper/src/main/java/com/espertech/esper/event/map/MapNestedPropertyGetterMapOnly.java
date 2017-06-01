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
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.codegen.core.CodegenBlock;
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.event.EventAdapterService;
import com.espertech.esper.event.EventPropertyGetterSPI;

import java.util.List;
import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Getter for one or more levels deep nested properties of maps.
 */
public class MapNestedPropertyGetterMapOnly implements MapEventPropertyGetter {
    private final MapEventPropertyGetter[] mapGetterChain;

    /**
     * Ctor.
     *
     * @param getterChain        is the chain of getters to retrieve each nested property
     * @param eventAdaperService is a factory for POJO bean event types
     */
    public MapNestedPropertyGetterMapOnly(List<EventPropertyGetterSPI> getterChain,
                                          EventAdapterService eventAdaperService) {
        this.mapGetterChain = new MapEventPropertyGetter[getterChain.size()];
        for (int i = 0; i < getterChain.size(); i++) {
            mapGetterChain[i] = (MapEventPropertyGetter) getterChain.get(i);
        }
    }

    public Object getMap(Map<String, Object> map) throws PropertyAccessException {
        Object result = mapGetterChain[0].getMap(map);
        return handleGetterTrailingChain(result);
    }

    public boolean isMapExistsProperty(Map<String, Object> map) {
        if (!mapGetterChain[0].isMapExistsProperty(map)) {
            return false;
        }
        Object result = mapGetterChain[0].getMap(map);
        return handleIsExistsTrailingChain(result);
    }

    private String isMapExistsPropertyCodegen(CodegenContext context) {
        return context.addMethod(boolean.class, Map.class, "map", this.getClass())
                .ifConditionReturnConst(not(mapGetterChain[0].codegenUnderlyingExists(ref("map"), context)), false)
                .declareVar(Object.class, "result", mapGetterChain[0].codegenUnderlyingGet(ref("map"), context))
                .methodReturn(localMethod(handleIsExistsTrailingChainCodegen(context), ref("result")));
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        Object result = mapGetterChain[0].get(eventBean);
        return handleGetterTrailingChain(result);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        if (!mapGetterChain[0].isExistsProperty(eventBean)) {
            return false;
        }
        Object result = mapGetterChain[0].get(eventBean);
        return handleIsExistsTrailingChain(result);
    }

    public Object getFragment(EventBean eventBean) {
        return null;
    }

    public CodegenExpression codegenEventBeanGet(CodegenExpression beanExpression, CodegenContext context) {
        return codegenUnderlyingGet(castUnderlying(Map.class, beanExpression), context);
    }

    public CodegenExpression codegenEventBeanExists(CodegenExpression beanExpression, CodegenContext context) {
        return codegenUnderlyingExists(castUnderlying(Map.class, beanExpression), context);
    }

    public CodegenExpression codegenEventBeanFragment(CodegenExpression beanExpression, CodegenContext context) {
        return constantNull();
    }

    public CodegenExpression codegenUnderlyingGet(CodegenExpression underlyingExpression, CodegenContext context) {
        CodegenExpression resultExpression = mapGetterChain[0].codegenUnderlyingGet(underlyingExpression, context);
        return localMethod(handleGetterTrailingChainCodegen(context), resultExpression);
    }

    public CodegenExpression codegenUnderlyingExists(CodegenExpression underlyingExpression, CodegenContext context) {
        return localMethod(isMapExistsPropertyCodegen(context), underlyingExpression);
    }

    public CodegenExpression codegenUnderlyingFragment(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantNull();
    }

    private boolean handleIsExistsTrailingChain(Object result) {
        for (int i = 1; i < mapGetterChain.length; i++) {
            if (result == null) {
                return false;
            }

            MapEventPropertyGetter getter = mapGetterChain[i];

            if (i == mapGetterChain.length - 1) {
                if (!(result instanceof Map)) {
                    if (result instanceof EventBean) {
                        return getter.isExistsProperty((EventBean) result);
                    }
                    return false;
                } else {
                    return getter.isMapExistsProperty((Map<String, Object>) result);
                }
            }

            if (!(result instanceof Map)) {
                if (result instanceof EventBean) {
                    result = getter.get((EventBean) result);
                } else {
                    return false;
                }
            } else {
                result = getter.getMap((Map<String, Object>) result);
            }
        }
        return true;
    }

    private String handleIsExistsTrailingChainCodegen(CodegenContext context) {
        CodegenBlock block = context.addMethod(boolean.class, Object.class, "result", this.getClass());
        for (int i = 1; i < mapGetterChain.length; i++) {
            block.ifRefNullReturnFalse("result");
            MapEventPropertyGetter getter = mapGetterChain[i];

            if (i == mapGetterChain.length - 1) {
                block.ifNotInstanceOf("result", Map.class)
                        .ifInstanceOf("result", EventBean.class)
                        .assignRef("result", getter.codegenEventBeanExists(castRef(EventBean.class, "result"), context))
                        .blockElse()
                        .blockReturn(constantFalse())
                        .blockElse()
                        .blockReturn(getter.codegenUnderlyingExists(castRef(Map.class, "result"), context));
            }

            block.ifNotInstanceOf("result", Map.class)
                    .ifInstanceOf("result", EventBean.class)
                    .assignRef("result", getter.codegenEventBeanGet(castRef(EventBean.class, "result"), context))
                    .blockElse()
                    .blockReturn(constantFalse())
                    .blockElse()
                    .assignRef("result", getter.codegenUnderlyingGet(castRef(Map.class, "result"), context))
                    .blockEnd();
        }
        return block.methodReturn(constantTrue());
    }

    private Object handleGetterTrailingChain(Object result) {
        for (int i = 1; i < mapGetterChain.length; i++) {
            if (result == null) {
                return null;
            }

            MapEventPropertyGetter getter = mapGetterChain[i];
            if (!(result instanceof Map)) {
                if (result instanceof EventBean) {
                    result = getter.get((EventBean) result);
                } else {
                    return null;
                }
            } else {
                result = getter.getMap((Map<String, Object>) result);
            }
        }
        return result;
    }

    private String handleGetterTrailingChainCodegen(CodegenContext context) {
        CodegenBlock block = context.addMethod(Object.class, Object.class, "result", this.getClass());
        for (int i = 1; i < mapGetterChain.length; i++) {
            block.ifRefNullReturnNull("result");
            MapEventPropertyGetter getter = mapGetterChain[i];
            block.ifNotInstanceOf("result", Map.class)
                    .ifInstanceOf("result", EventBean.class)
                    .assignRef("result", getter.codegenEventBeanGet(castRef(EventBean.class, "result"), context))
                    .blockElse()
                    .blockReturn(constantNull())
                    .blockElse()
                    .assignRef("result", getter.codegenUnderlyingGet(castRef(Map.class, "result"), context))
                    .blockEnd();
        }
        return block.methodReturn(ref("result"));
    }
}
