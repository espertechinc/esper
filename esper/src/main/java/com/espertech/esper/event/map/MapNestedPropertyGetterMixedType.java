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
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.event.EventPropertyGetterSPI;
import com.espertech.esper.event.bean.BeanEventPropertyGetter;

import java.util.List;
import java.util.Map;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Getter for one or more levels deep nested properties of maps.
 */
public class MapNestedPropertyGetterMixedType implements MapEventPropertyGetter {
    private final EventPropertyGetterSPI[] getterChain;

    public MapNestedPropertyGetterMixedType(List<EventPropertyGetterSPI> getterChain) {
        this.getterChain = getterChain.toArray(new EventPropertyGetterSPI[getterChain.size()]);
    }

    public Object getMap(Map<String, Object> map) throws PropertyAccessException {
        Object result = ((MapEventPropertyGetter) getterChain[0]).getMap(map);
        return handleGetterTrailingChain(result);
    }

    private CodegenMethodNode getMapCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) throws PropertyAccessException {
        return codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(Map.class, "map").getBlock()
                .declareVar(Object.class, "result", getterChain[0].underlyingGetCodegen(ref("map"), codegenMethodScope, codegenClassScope))
                .methodReturn(localMethod(handleGetterTrailingChainCodegen(codegenMethodScope, codegenClassScope), ref("result")));
    }

    public boolean isMapExistsProperty(Map<String, Object> map) {
        if (!((MapEventPropertyGetter) getterChain[0]).isMapExistsProperty(map)) {
            return false;
        }
        Object result = ((MapEventPropertyGetter) getterChain[0]).getMap(map);
        return handleIsExistsTrailingChain(result);
    }

    private CodegenMethodNode isMapExistsPropertyCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) throws PropertyAccessException {
        return codegenMethodScope.makeChild(boolean.class, this.getClass(), codegenClassScope).addParam(Map.class, "map").getBlock()
                .ifConditionReturnConst(getterChain[0].underlyingExistsCodegen(ref("map"), codegenMethodScope, codegenClassScope), false)
                .declareVar(Object.class, "result", getterChain[0].underlyingGetCodegen(ref("map"), codegenMethodScope, codegenClassScope))
                .methodReturn(localMethod(handleIsExistsTrailingChainCodegen(codegenMethodScope, codegenClassScope), ref("result")));
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        Object result = getterChain[0].get(eventBean);
        return handleGetterTrailingChain(result);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        if (!getterChain[0].isExistsProperty(eventBean)) {
            return false;
        }
        Object result = getterChain[0].get(eventBean);
        return handleIsExistsTrailingChain(result);
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(Map.class, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingExistsCodegen(castUnderlying(Map.class, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getMapCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(isMapExistsPropertyCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantNull();
    }

    private boolean handleIsExistsTrailingChain(Object result) {
        for (int i = 1; i < getterChain.length; i++) {
            if (result == null) {
                return false;
            }

            EventPropertyGetter getter = getterChain[i];

            if (i == getterChain.length - 1) {
                if (getter instanceof BeanEventPropertyGetter) {
                    return ((BeanEventPropertyGetter) getter).isBeanExistsProperty(result);
                } else if (result instanceof Map && getter instanceof MapEventPropertyGetter) {
                    return ((MapEventPropertyGetter) getter).isMapExistsProperty((Map) result);
                } else if (result instanceof EventBean) {
                    return getter.isExistsProperty((EventBean) result);
                } else {
                    return false;
                }
            }

            if (getter instanceof BeanEventPropertyGetter) {
                result = ((BeanEventPropertyGetter) getter).getBeanProp(result);
            } else if (result instanceof Map && getter instanceof MapEventPropertyGetter) {
                result = ((MapEventPropertyGetter) getter).getMap((Map) result);
            } else if (result instanceof EventBean) {
                result = getter.get((EventBean) result);
            } else {
                return false;
            }
        }
        return false;
    }

    private CodegenMethodNode handleIsExistsTrailingChainCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenBlock block = codegenMethodScope.makeChild(boolean.class, this.getClass(), codegenClassScope).addParam(Object.class, "result").getBlock();
        for (int i = 1; i < getterChain.length - 1; i++) {
            block.ifRefNullReturnFalse("result");
            EventPropertyGetterSPI getter = getterChain[i];
            CodegenBlock blockBean = block.ifInstanceOf("result", EventBean.class);
            blockBean.assignRef("result", getter.eventBeanGetCodegen(cast(EventBean.class, ref("result")), codegenMethodScope, codegenClassScope));

            if (getter instanceof BeanEventPropertyGetter) {
                Class type = ((BeanEventPropertyGetter) getter).getTargetType();
                blockBean.ifElse()
                        .assignRef("result", getter.underlyingGetCodegen(cast(type, ref("result")), codegenMethodScope, codegenClassScope))
                        .blockEnd();
            } else if (getter instanceof MapEventPropertyGetter) {
                blockBean.ifElse()
                        .ifRefNotTypeReturnConst("result", Map.class, false)
                        .assignRef("result", getter.underlyingGetCodegen(cast(Map.class, ref("result")), codegenMethodScope, codegenClassScope))
                        .blockEnd();
            } else {
                blockBean.ifElse().blockReturn(constantFalse());
            }
        }

        EventPropertyGetterSPI getter = getterChain[getterChain.length - 1];
        if (getter instanceof BeanEventPropertyGetter) {
            BeanEventPropertyGetter beanGetter = (BeanEventPropertyGetter) getter;
            return block.methodReturn(getter.underlyingExistsCodegen(cast(beanGetter.getTargetType(), ref("result")), codegenMethodScope, codegenClassScope));
        } else if (getter instanceof MapEventPropertyGetter) {
            return block.methodReturn(getter.underlyingExistsCodegen(cast(Map.class, ref("result")), codegenMethodScope, codegenClassScope));
        } else {
            block.ifInstanceOf("result", EventBean.class)
                    .blockReturn(getter.eventBeanExistsCodegen(cast(EventBean.class, ref("result")), codegenMethodScope, codegenClassScope));
            return block.methodReturn(constantFalse());
        }
    }

    public Object getFragment(EventBean eventBean) {
        return null;
    }

    private Object handleGetterTrailingChain(Object result) {

        for (int i = 1; i < getterChain.length; i++) {
            if (result == null) {
                return null;
            }
            EventPropertyGetter getter = getterChain[i];
            if (result instanceof EventBean) {
                result = getter.get((EventBean) result);
            } else if (getter instanceof BeanEventPropertyGetter) {
                result = ((BeanEventPropertyGetter) getter).getBeanProp(result);
            } else if (result instanceof Map && getter instanceof MapEventPropertyGetter) {
                result = ((MapEventPropertyGetter) getter).getMap((Map) result);
            } else {
                return null;
            }
        }
        return result;
    }

    private CodegenMethodNode handleGetterTrailingChainCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenBlock block = codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(Object.class, "result").getBlock();
        for (int i = 1; i < getterChain.length; i++) {
            block.ifRefNullReturnNull("result");
            EventPropertyGetterSPI getter = getterChain[i];
            CodegenBlock blockBean = block.ifInstanceOf("result", EventBean.class);
            blockBean.assignRef("result", getter.eventBeanGetCodegen(cast(EventBean.class, ref("result")), codegenMethodScope, codegenClassScope));
            if (getter instanceof BeanEventPropertyGetter) {
                Class type = ((BeanEventPropertyGetter) getter).getTargetType();
                blockBean.ifElse()
                        .assignRef("result", getter.underlyingGetCodegen(cast(type, ref("result")), codegenMethodScope, codegenClassScope))
                        .blockEnd();
            } else if (getter instanceof MapEventPropertyGetter) {
                blockBean.ifElse()
                        .ifRefNotTypeReturnConst("result", Map.class, null)
                        .assignRef("result", getter.underlyingGetCodegen(cast(Map.class, ref("result")), codegenMethodScope, codegenClassScope))
                        .blockEnd();
            } else {
                blockBean.ifElse().blockReturn(constantNull());
            }
        }
        return block.methodReturn(ref("result"));
    }
}
