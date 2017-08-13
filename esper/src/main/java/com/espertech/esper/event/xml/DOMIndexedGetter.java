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
import com.espertech.esper.codegen.core.CodegenMethodId;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.event.EventPropertyGetterSPI;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Getter for retrieving a value at a certain index.
 */
public class DOMIndexedGetter implements EventPropertyGetterSPI, DOMPropertyGetter {
    private final String propertyName;
    private final int index;
    private final FragmentFactory fragmentFactory;

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param node node
     * @param propertyName property
     * @param index index
     * @return value
     */
    public static Node getNodeValue(Node node, String propertyName, int index) {
        NodeList list = node.getChildNodes();
        int count = 0;
        for (int i = 0; i < list.getLength(); i++) {
            Node childNode = list.item(i);
            if (childNode == null) {
                continue;
            }
            if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            String elementName = childNode.getLocalName();
            if (elementName == null) {
                elementName = childNode.getNodeName();
            }

            if (!(propertyName.equals(elementName))) {
                continue;
            }

            if (count == index) {
                return childNode;
            }
            count++;
        }
        return null;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param node node
     * @param propertyName property
     * @param index index
     * @return value
     */
    public static boolean getNodeValueExists(Node node, String propertyName, int index) {
        return getNodeValue(node, propertyName, index) != null;
    }

    /**
     * Ctor.
     *
     * @param propertyName    property name
     * @param index           index
     * @param fragmentFactory for creating fragments if required
     */
    public DOMIndexedGetter(String propertyName, int index, FragmentFactory fragmentFactory) {
        this.propertyName = propertyName;
        this.index = index;
        this.fragmentFactory = fragmentFactory;
    }

    public Node[] getValueAsNodeArray(Node node) {
        return null;
    }

    public Object getValueAsFragment(Node node) {
        if (fragmentFactory == null) {
            return null;
        }
        Node result = getValueAsNode(node);
        if (result == null) {
            return null;
        }
        return fragmentFactory.getEvent(result);
    }

    private CodegenMethodId getValueAsFragmentCodegen(CodegenContext context) {
        CodegenMember member = context.makeAddMember(FragmentFactory.class, fragmentFactory);
        return context.addMethod(Object.class, this.getClass()).add(Node.class, "node").begin()
                .declareVar(Node.class, "result", staticMethod(DOMIndexedGetter.class, "getNodeValue", ref("node"), constant(propertyName), constant(index)))
                .ifRefNullReturnNull("result")
                .methodReturn(exprDotMethod(member(member.getMemberId()), "getEvent", ref("result")));
    }

    public Node getValueAsNode(Node node) {
        return getNodeValue(node, propertyName, index);
    }

    public Object get(EventBean eventBean) throws PropertyAccessException {
        Object result = eventBean.getUnderlying();
        if (!(result instanceof Node)) {
            return null;
        }
        Node node = (Node) result;
        return getValueAsNode(node);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        Object result = eventBean.getUnderlying();
        if (!(result instanceof Node)) {
            return false;
        }
        Node node = (Node) result;
        return getValueAsNode(node) != null;
    }

    public Object getFragment(EventBean eventBean) {
        Object result = eventBean.getUnderlying();
        if (!(result instanceof Node)) {
            return null;
        }
        Node node = (Node) result;
        return getValueAsFragment(node);
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenContext context) {
        return underlyingGetCodegen(castUnderlying(Node.class, beanExpression), context);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenContext context) {
        return underlyingExistsCodegen(castUnderlying(Node.class, beanExpression), context);
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenContext context) {
        return underlyingFragmentCodegen(castUnderlying(Node.class, beanExpression), context);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        return staticMethod(this.getClass(), "getNodeValue", underlyingExpression, constant(propertyName), constant(index));
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        return staticMethod(this.getClass(), "getNodeValueExists", underlyingExpression, constant(propertyName), constant(index));
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenContext context) {
        if (fragmentFactory == null) {
            return constantNull();
        }
        return localMethod(getValueAsFragmentCodegen(context), underlyingExpression);
    }

    public CodegenExpression getValueAsNodeCodegen(CodegenExpression value, CodegenContext context) {
        return underlyingGetCodegen(value, context);
    }

    public CodegenExpression getValueAsNodeArrayCodegen(CodegenExpression value, CodegenContext context) {
        return constantNull();
    }

    public CodegenExpression getValueAsFragmentCodegen(CodegenExpression value, CodegenContext context) {
        return underlyingFragmentCodegen(value, context);
    }
}
