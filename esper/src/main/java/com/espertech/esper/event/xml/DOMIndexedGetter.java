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
import com.espertech.esper.client.EventPropertyGetter;
import com.espertech.esper.client.PropertyAccessException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Getter for retrieving a value at a certain index.
 */
public class DOMIndexedGetter implements EventPropertyGetter, DOMPropertyGetter {
    private final String propertyName;
    private final int index;
    private final FragmentFactory fragmentFactory;

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

    public Node getValueAsNode(Node node) {
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
}
