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

import java.util.List;

/**
 * Getter for nested properties in a DOM tree.
 */
public class DOMNestedPropertyGetter implements EventPropertyGetter, DOMPropertyGetter {
    private final DOMPropertyGetter[] domGetterChain;
    private final FragmentFactory fragmentFactory;

    /**
     * Ctor.
     *
     * @param getterChain     is the chain of getters to retrieve each nested property
     * @param fragmentFactory for creating fragments
     */
    public DOMNestedPropertyGetter(List<EventPropertyGetter> getterChain, FragmentFactory fragmentFactory) {
        this.domGetterChain = new DOMPropertyGetter[getterChain.size()];
        this.fragmentFactory = fragmentFactory;

        int count = 0;
        for (EventPropertyGetter getter : getterChain) {
            domGetterChain[count++] = (DOMPropertyGetter) getter;
        }
    }

    public Object getValueAsFragment(Node node) {
        Node result = getValueAsNode(node);
        if (result == null) {
            return null;
        }
        return fragmentFactory.getEvent(result);
    }

    public Node[] getValueAsNodeArray(Node node) {
        Node value = node;

        for (int i = 0; i < domGetterChain.length - 1; i++) {
            value = domGetterChain[i].getValueAsNode(value);

            if (value == null) {
                return null;
            }
        }

        return domGetterChain[domGetterChain.length - 1].getValueAsNodeArray(value);
    }

    public Node getValueAsNode(Node node) {
        Node value = node;

        for (int i = 0; i < domGetterChain.length; i++) {
            value = domGetterChain[i].getValueAsNode(value);

            if (value == null) {
                return null;
            }
        }

        return value;
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

    public boolean isExistsProperty(EventBean obj) {
        // The underlying is expected to be a map
        if (!(obj.getUnderlying() instanceof Node)) {
            throw new PropertyAccessException("Mismatched property getter to event bean type, " +
                    "the underlying data object is not of type Node");
        }

        Node value = (Node) obj.getUnderlying();

        for (int i = 0; i < domGetterChain.length; i++) {
            value = domGetterChain[i].getValueAsNode(value);

            if (value == null) {
                return false;
            }
        }

        return true;
    }

    public Object getFragment(EventBean obj) throws PropertyAccessException {
        // The underlying is expected to be a map
        if (!(obj.getUnderlying() instanceof Node)) {
            throw new PropertyAccessException("Mismatched property getter to event bean type, " +
                    "the underlying data object is not of type Node");
        }

        Node value = (Node) obj.getUnderlying();

        for (int i = 0; i < domGetterChain.length - 1; i++) {
            value = domGetterChain[i].getValueAsNode(value);

            if (value == null) {
                return false;
            }
        }

        return domGetterChain[domGetterChain.length - 1].getValueAsFragment(value);
    }
}
