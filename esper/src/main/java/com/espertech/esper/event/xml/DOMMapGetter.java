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
 * DOM getter for Map-property.
 */
public class DOMMapGetter implements EventPropertyGetterSPI, DOMPropertyGetter {
    private final String propertyMap;
    private final String mapKey;
    private final FragmentFactory fragmentFactory;

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param node node
     * @param propertyMap property
     * @param mapKey key
     * @return value
     */
    public static Node getNodeValue(Node node, String propertyMap, String mapKey) {
        NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node childNode = list.item(i);
            if (childNode == null) {
                continue;
            }
            if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if (!(childNode.getNodeName().equals(propertyMap))) {
                continue;
            }

            Node attribute = childNode.getAttributes().getNamedItem("id");
            if (attribute == null) {
                continue;
            }
            if (!(attribute.getTextContent().equals(mapKey))) {
                continue;
            }

            return childNode;
        }
        return null;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param node node
     * @param propertyMap property
     * @param mapKey key
     * @return exists flag
     */
    public static boolean getNodeValueExists(Node node, String propertyMap, String mapKey) {
        NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node childNode = list.item(i);
            if (childNode == null) {
                continue;
            }
            if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            if (!(childNode.getNodeName().equals(propertyMap))) {
                continue;
            }

            Node attribute = childNode.getAttributes().getNamedItem("id");
            if (attribute == null) {
                continue;
            }
            if (!(attribute.getTextContent().equals(mapKey))) {
                continue;
            }

            return true;
        }
        return false;
    }

    /**
     * Ctor.
     *
     * @param propertyName    property name
     * @param mapKey          key in map
     * @param fragmentFactory for creating fragments
     */
    public DOMMapGetter(String propertyName, String mapKey, FragmentFactory fragmentFactory) {
        this.propertyMap = propertyName;
        this.mapKey = mapKey;
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

    private String getValueAsFragmentCodegen(CodegenContext context) {
        CodegenMember mType = context.makeAddMember(FragmentFactory.class, fragmentFactory);
        return context.addMethod(Object.class, Node.class, "node", this.getClass())
                .declareVar(Node.class, "result", getValueAsNodeCodegen(ref("node"), context))
                .ifRefNullReturnNull("result")
                .methodReturn(exprDotMethod(ref(mType.getMemberName()), "getEvent", ref("result")));
    }

    public Node getValueAsNode(Node node) {
        return getNodeValue(node, propertyMap, mapKey);
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
        return getNodeValueExists(node, propertyMap, mapKey);
    }

    public Object getFragment(EventBean eventBean) {
        return null;
    }

    public CodegenExpression codegenEventBeanGet(CodegenExpression beanExpression, CodegenContext context) {
        return codegenUnderlyingGet(castUnderlying(Node.class, beanExpression), context);
    }

    public CodegenExpression codegenEventBeanExists(CodegenExpression beanExpression, CodegenContext context) {
        return codegenUnderlyingExists(castUnderlying(Node.class, beanExpression), context);
    }

    public CodegenExpression codegenEventBeanFragment(CodegenExpression beanExpression, CodegenContext context) {
        return constantNull();
    }

    public CodegenExpression codegenUnderlyingGet(CodegenExpression underlyingExpression, CodegenContext context) {
        return staticMethodTakingExprAndConst(this.getClass(), "getNodeValue", underlyingExpression, propertyMap, mapKey);
    }

    public CodegenExpression codegenUnderlyingExists(CodegenExpression underlyingExpression, CodegenContext context) {
        return staticMethodTakingExprAndConst(this.getClass(), "getNodeValueExists", underlyingExpression, propertyMap, mapKey);
    }

    public CodegenExpression codegenUnderlyingFragment(CodegenExpression underlyingExpression, CodegenContext context) {
        return constantNull();
    }

    public CodegenExpression getValueAsNodeCodegen(CodegenExpression value, CodegenContext context) {
        return codegenUnderlyingGet(value, context);
    }

    public CodegenExpression getValueAsNodeArrayCodegen(CodegenExpression value, CodegenContext context) {
        return constantNull();
    }

    public CodegenExpression getValueAsFragmentCodegen(CodegenExpression value, CodegenContext context) {
        return localMethod(getValueAsFragmentCodegen(context), value);
    }
}
