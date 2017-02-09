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
 * Getter for a DOM complex element.
 */
public class DOMComplexElementGetter implements EventPropertyGetter, DOMPropertyGetter {
    private final String propertyName;
    private final FragmentFactory fragmentFactory;
    private final boolean isArray;

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
            Node result = getValueAsNode(node);
            if (result == null) {
                return result;
            }

            return fragmentFactory.getEvent(result);
        } else {
            Node[] result = getValueAsNodeArray(node);
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
    }

    public Node getValueAsNode(Node node) {
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

    public Node[] getValueAsNodeArray(Node node) {
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
}
