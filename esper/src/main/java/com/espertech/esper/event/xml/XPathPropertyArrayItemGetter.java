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
package com.espertech.esper.event.xml;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.PropertyAccessException;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.event.EventPropertyGetterSPI;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Getter for XPath explicit properties returning an element in an array.
 */
public class XPathPropertyArrayItemGetter implements EventPropertyGetterSPI {
    private final EventPropertyGetterSPI getter;
    private final int index;
    private final FragmentFactory fragmentFactory;

    /**
     * Ctor.
     *
     * @param getter          property getter returning the parent node
     * @param index           to get item at
     * @param fragmentFactory for creating fragments, or null if not creating fragments
     */
    public XPathPropertyArrayItemGetter(EventPropertyGetterSPI getter, int index, FragmentFactory fragmentFactory) {
        this.getter = getter;
        this.index = index;
        this.fragmentFactory = fragmentFactory;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param object object
     * @param index index
     * @return value
     */
    public static Object getXPathNodeListWCheck(Object object, int index) {
        if (!(object instanceof NodeList)) {
            return null;
        }

        NodeList nodeList = (NodeList) object;
        if (nodeList.getLength() <= index) {
            return null;
        }
        return nodeList.item(index);
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        return getXPathNodeListWCheck(getter.get(eventBean), index);
    }

    private CodegenMethodNode getCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) throws PropertyAccessException {
        return codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(Node.class, "node").getBlock()
                .declareVar(Object.class, "value", getter.underlyingGetCodegen(ref("node"), codegenMethodScope, codegenClassScope))
                .methodReturn(staticMethod(this.getClass(), "getXPathNodeListWCheck", ref("value"), constant(index)));
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        if (fragmentFactory == null) {
            return null;
        }
        Node result = (Node) get(eventBean);
        if (result == null) {
            return null;
        }
        return fragmentFactory.getEvent(result);
    }

    private CodegenMethodNode getFragmentCodegen(CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMember member = codegenClassScope.makeAddMember(FragmentFactory.class, fragmentFactory);
        return codegenMethodScope.makeChild(Object.class, this.getClass(), codegenClassScope).addParam(Node.class, "node").getBlock()
                .declareVar(Node.class, "result", cast(Node.class, underlyingGetCodegen(ref("node"), codegenMethodScope, codegenClassScope)))
                .ifRefNullReturnNull("result")
                .methodReturn(exprDotMethod(member(member.getMemberId()), "getEvent", ref("result")));
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true;
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(Node.class, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantTrue();
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        if (fragmentFactory == null) {
            return constantNull();
        }
        return underlyingFragmentCodegen(castUnderlying(Node.class, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return localMethod(getCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantTrue();
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        if (fragmentFactory == null) {
            return constantNull();
        }
        return localMethod(getFragmentCodegen(codegenMethodScope, codegenClassScope), underlyingExpression);
    }
}
