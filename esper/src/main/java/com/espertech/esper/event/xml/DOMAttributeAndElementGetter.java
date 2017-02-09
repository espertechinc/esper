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
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Getter for both attribute and element values, attributes are checked first.
 */
public class DOMAttributeAndElementGetter implements EventPropertyGetter, DOMPropertyGetter {
    private final String propertyName;

    /**
     * Ctor.
     *
     * @param propertyName property name
     */
    public DOMAttributeAndElementGetter(String propertyName) {
        this.propertyName = propertyName;
    }

    public Object getValueAsFragment(Node node) {
        return null;
    }

    public Node[] getValueAsNodeArray(Node node) {
        return null;
    }

    public Node getValueAsNode(Node node) {
        NamedNodeMap namedNodeMap = node.getAttributes();
        if (namedNodeMap != null) {
            for (int i = 0; i < namedNodeMap.getLength(); i++) {
                Node attrNode = namedNodeMap.item(i);
                if (attrNode.getLocalName() != null) {
                    if (propertyName.equals(attrNode.getLocalName())) {
                        return attrNode;
                    }
                    continue;
                }
                if (propertyName.equals(attrNode.getNodeName())) {
                    return attrNode;
                }
            }
        }

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
            if (childNode.getNodeName().equals(propertyName)) {
                return childNode;
            }
        }

        return null;
    }

    public Object get(EventBean obj) throws PropertyAccessException {
        // The underlying is expected to be a map
        if (!(obj.getUnderlying() instanceof Node)) {
            throw new PropertyAccessException("Mismatched property getter to event bean type, " +
                    "the underlying data object is not of type Node");
        }

        Node node = (Node) obj.getUnderlying();
        return getValueAsNode(node);
    }

    public boolean isExistsProperty(EventBean eventBean) {
        // The underlying is expected to be a map
        if (!(eventBean.getUnderlying() instanceof Node)) {
            throw new PropertyAccessException("Mismatched property getter to event bean type, " +
                    "the underlying data object is not of type Node");
        }

        Node node = (Node) eventBean.getUnderlying();
        NamedNodeMap namedNodeMap = node.getAttributes();
        if (namedNodeMap != null) {
            for (int i = 0; i < namedNodeMap.getLength(); i++) {
                Node attrNode = namedNodeMap.item(i);
                if (attrNode.getLocalName() != null) {
                    if (propertyName.equals(attrNode.getLocalName())) {
                        return true;
                    }
                    continue;
                }
                if (propertyName.equals(attrNode.getNodeName())) {
                    return true;
                }
            }
        }

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
                    return true;
                }
                continue;
            }
            if (childNode.getNodeName().equals(propertyName)) {
                return true;
            }
        }

        return false;
    }

    public Object getFragment(EventBean eventBean) throws PropertyAccessException {
        return null;  // Never a fragment
    }
}
