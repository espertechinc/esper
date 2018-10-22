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
package com.espertech.esper.regressionlib.support.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SupportSelectorWithListEvent implements Serializable {
    private final String selector;
    private final List<String> myList;

    public SupportSelectorWithListEvent(String selector) {
        this.selector = selector;

        myList = new ArrayList<String>();
        myList.add("1");
        myList.add("2");
        myList.add("3");
    }

    public String getSelector() {
        return selector;
    }

    public List<String> getTheList() {
        return myList;
    }

    public String[] getTheArray() {
        return myList.toArray(new String[myList.size()]);
    }

    public SupportStringListEvent getNestedMyEvent() {
        return new SupportStringListEvent(myList);
    }

    public static String[] convertToArray(List<String> list) {
        return list.toArray(new String[list.size()]);
    }
}
