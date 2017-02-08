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

import java.util.ArrayList;
import java.util.List;

/**
 * A simple node for the creation of a tree, intended in this case to mirror an XML model.
 */
public class ElementPathNode {

    private final String name;
    private final ElementPathNode parent;
    private List<ElementPathNode> children = null;

    public ElementPathNode(final ElementPathNode parent, final String name) {
        this.name = name;
        this.parent = parent;
    }

    public ElementPathNode addChild(String name) {
        if (children == null) {
            children = new ArrayList<ElementPathNode>();
        }
        ElementPathNode newChild = new ElementPathNode(this, name);
        children.add(newChild);
        return newChild;
    }

    public String getName() {
        return name;
    }

    public ElementPathNode getParent() {
        return parent;
    }

    public boolean doesNameAlreadyExistInHierarchy() {
        return doesNameAlreadyExistInHierarchy(this.getName());
    }

    private boolean doesNameAlreadyExistInHierarchy(String nameToFind) {
        boolean doesNameAlreadyExistInHierarchy = false;
        if (parent != null) {
            if (parent.getName().equals(nameToFind)) {
                doesNameAlreadyExistInHierarchy = true;
            } else {
                return parent.doesNameAlreadyExistInHierarchy(nameToFind);
            }
        }
        return doesNameAlreadyExistInHierarchy;
    }
}
