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
package com.espertech.esper.util;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator over DOM nodes that positions between elements.
 */
public class DOMElementIterator implements Iterator<Element> {
    private int index;
    private NodeList nodeList;

    /**
     * Ctor.
     *
     * @param nodeList is a list of DOM nodes.
     */
    public DOMElementIterator(NodeList nodeList) {
        this.nodeList = nodeList;
    }

    public boolean hasNext() {
        positionNext();
        return index < nodeList.getLength();
    }

    public Element next() {
        if (index >= nodeList.getLength()) {
            throw new NoSuchElementException();
        }
        Element result = (Element) nodeList.item(index);
        index++;
        return result;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    private void positionNext() {
        while (index < nodeList.getLength()) {
            Node node = nodeList.item(index);
            if (node instanceof Element) {
                break;
            }
            index++;
        }
    }
}
