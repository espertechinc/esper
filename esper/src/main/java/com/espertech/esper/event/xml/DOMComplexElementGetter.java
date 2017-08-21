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
import com.espertech.esper.event.EventPropertyGetterSPI;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

/**
 * Getter for a DOM complex element.
 */
public class DOMComplexElementGetter implements EventPropertyGetterSPI, DOMPropertyGetter {
    private final String propertyName;
    private final FragmentFactory fragmentFactory;
    private final boolean isArray;

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param node node
     * @param propertyName prop name
     * @return node
     */
    public static Node getValueAsNode(Node node, String propertyName) {
        NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node childNode = list.item(i);
            if (childNode == null) {
                continue;
            }
            if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (childNode.getLocalName() != null) {
                if (propertyName.equals(childNode.getLocalName())) {
                    return childNode;
                }
                continue;
            }
            if (propertyName.equals(childNode.getNodeName())) {
                return childNode;
            }
        }
        return null;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param node node
     * @param propertyName prop name
     * @return node
     */
    public static Node[] getValueAsNodeArray(Node node, String propertyName) {
        NodeList list = node.getChildNodes();

        int count = 0;
        for (int i = 0; i < list.getLength(); i++) {
            Node childNode = list.item(i);
            if (childNode == null) {
                continue;
            }
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                count++;
            }
        }

        if (count == 0) {
            return new Node[0];
        }

        Node[] nodes = new Node[count];
        int realized = 0;
        for (int i = 0; i < list.getLength(); i++) {
            Node childNode = list.item(i);
            if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (childNode.getLocalName() != null) {
                if (propertyName.equals(childNode.getLocalName())) {
                    nodes[realized++] = childNode;
                }
                continue;
            }
            if (childNode.getNodeName().equals(propertyName)) {
                nodes[realized++] = childNode;
            }
        }

        if (realized == count) {
            return nodes;
        }
        if (realized == 0) {
            return new Node[0];
        }

        Node[] shrunk = new Node[realized];
        System.arraycopy(nodes, 0, shrunk, 0, realized);
        return shrunk;
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param node node
     * @param propertyName prop name
     * @param fragmentFactory fragment factory
     * @return node
     */
    public static Object getValueAsNodeFragment(Node node, String propertyName, FragmentFactory fragmentFactory) {
        Node result = getValueAsNode(node, propertyName);
        if (result == null) {
            return null;
        }
        return fragmentFactory.getEvent(result);
    }

    /**
     * NOTE: Code-generation-invoked method, method name and parameter order matters
     * @param node node
     * @param propertyName prop name
     * @param fragmentFactory fragment factory
     * @return node
     */
    public static Object getValueAsNodeFragmentArray(Node node, String propertyName, FragmentFactory fragmentFactory) {
        Node[] result = getValueAsNodeArray(node, propertyName);
        if ((result == null) || (result.length == 0)) {
            return new EventBean[0];
        }

        EventBean[] events = new EventBean[result.length];
        int count = 0;
        for (int i = 0; i < result.length; i++) {
            events[count++] = fragmentFactory.getEvent(result[i]);
        }
        return events;
    }

    /**
     * Ctor.
     *
     * @param propertyName    property name
     * @param fragmentFactory for creating fragments
     * @param isArray         if this is an array property
     */
    public DOMComplexElementGetter(String propertyName, FragmentFactory fragmentFactory, boolean isArray) {
        this.propertyName = propertyName;
        this.fragmentFactory = fragmentFactory;
        this.isArray = isArray;
    }

    public Object getValueAsFragment(Node node) {
        if (!isArray) {
            return getValueAsNodeFragment(node, propertyName, fragmentFactory);
        } else {
            return getValueAsNodeFragmentArray(node, propertyName, fragmentFactory);
        }
    }

    public Node getValueAsNode(Node node) {
        return getValueAsNode(node, propertyName);
    }

    public Node[] getValueAsNodeArray(Node node) {
        return getValueAsNodeArray(node, propertyName);
    }

    public Object get(EventBean obj) throws PropertyAccessException {
        // The underlying is expected to be a map
        if (!(obj.getUnderlying() instanceof Node)) {
            throw new PropertyAccessException("Mismatched property getter to event bean type, " +
                    "the underlying data object is not of type Node");
        }

        if (!isArray) {
            Node node = (Node) obj.getUnderlying();
            return getValueAsNode(node);
        } else {
            Node node = (Node) obj.getUnderlying();
            return getValueAsNodeArray(node);
        }
    }

    public boolean isExistsProperty(EventBean eventBean) {
        return true;
    }

    public Object getFragment(EventBean obj) throws PropertyAccessException {
        // The underlying is expected to be a map
        if (!(obj.getUnderlying() instanceof Node)) {
            throw new PropertyAccessException("Mismatched property getter to event bean type, " +
                    "the underlying data object is not of type Node");
        }

        Node node = (Node) obj.getUnderlying();
        return getValueAsFragment(node);
    }

    public CodegenExpression eventBeanGetCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingGetCodegen(castUnderlying(Node.class, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression eventBeanExistsCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantTrue();
    }

    public CodegenExpression eventBeanFragmentCodegen(CodegenExpression beanExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingFragmentCodegen(castUnderlying(Node.class, beanExpression), codegenMethodScope, codegenClassScope);
    }

    public CodegenExpression underlyingGetCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        if (!isArray) {
            return staticMethod(this.getClass(), "getValueAsNode", underlyingExpression, constant(propertyName));
        }
        return staticMethod(this.getClass(), "getValueAsNodeArray", underlyingExpression, constant(propertyName));
    }

    public CodegenExpression underlyingExistsCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return constantTrue();
    }

    public CodegenExpression underlyingFragmentCodegen(CodegenExpression underlyingExpression, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        CodegenMember member = codegenClassScope.makeAddMember(FragmentFactory.class, fragmentFactory);
        if (!isArray) {
            return staticMethod(this.getClass(), "getValueAsNodeFragment", underlyingExpression, constant(propertyName), member(member.getMemberId()));
        } else {
            return staticMethod(this.getClass(), "getValueAsNodeFragmentArray", underlyingExpression, constant(propertyName), member(member.getMemberId()));
        }
    }

    public CodegenExpression getValueAsNodeCodegen(CodegenExpression value, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return staticMethod(this.getClass(), "getValueAsNode", value, constant(propertyName));
    }

    public CodegenExpression getValueAsNodeArrayCodegen(CodegenExpression value, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return staticMethod(this.getClass(), "getValueAsNodeArray", value, constant(propertyName));
    }

    public CodegenExpression getValueAsFragmentCodegen(CodegenExpression value, CodegenMethodScope codegenMethodScope, CodegenClassScope codegenClassScope) {
        return underlyingFragmentCodegen(value, codegenMethodScope, codegenClassScope);
    }
}
