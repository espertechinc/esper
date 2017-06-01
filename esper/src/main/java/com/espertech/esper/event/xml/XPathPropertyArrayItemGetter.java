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
import com.espertech.esper.codegen.core.CodegenContext;
import com.espertech.esper.codegen.core.CodegenMember;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
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

    private String getCodegen(CodegenContext context) throws PropertyAccessException {
        return context.addMethod(Object.class, Node.class, "node", this.getClass())
                .declareVar(Object.class, "value", getter.codegenUnderlyingGet(ref("node"), context))
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

    private String getFragmentCodegen(CodegenContext context) {
        CodegenMember member = context.makeAddMember(FragmentFactory.class, fragmentFactory);
        return context.addMethod(Object.class, Node.class, "node", this.getClass())
                .declareVar(Node.class, "result", cast(Node.class, codegenUnderlyingGet(ref("node"), context)))
                .ifRefNullReturnNull("result")
                .methodReturn(exprDotMethod(ref(member.getMemberName()), "getEvent", ref("result")));
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true;
    }

    public CodegenExpression codegenEventBeanGet(CodegenExpression beanExpression, CodegenContext context) {
        return codegenUnderlyingGet(castUnderlying(Node.class, beanExpression), context);
    }

    public CodegenExpression codegenEventBeanExists(CodegenExpression beanExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression codegenEventBeanFragment(CodegenExpression beanExpression, CodegenContext context) {
        if (fragmentFactory == null) {
            return constantNull();
        }
        return codegenUnderlyingFragment(castUnderlying(Node.class, beanExpression), context);
    }

    public CodegenExpression codegenUnderlyingGet(CodegenExpression underlyingExpression, CodegenContext context) {
        return localMethod(getCodegen(context), underlyingExpression);
    }

    public CodegenExpression codegenUnderlyingExists(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantTrue();
    }

    public CodegenExpression codegenUnderlyingFragment(CodegenExpression underlyingExpression, CodegenContext context) {
        if (fragmentFactory == null) {
            return constantNull();
        }
        return localMethod(getFragmentCodegen(context), underlyingExpression);
    }
}
