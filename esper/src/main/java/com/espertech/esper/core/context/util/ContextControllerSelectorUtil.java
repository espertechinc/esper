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
package com.espertech.esper.core.context.util;

import com.espertech.esper.client.context.*;
import com.espertech.esper.util.CollectionUtil;

import java.util.LinkedHashSet;

public class ContextControllerSelectorUtil {

    public static InvalidContextPartitionSelector getInvalidSelector(Class[] choice, ContextPartitionSelector selector) {
        return getInvalidSelector(choice, selector, false);
    }

    public static InvalidContextPartitionSelector getInvalidSelector(Class[] choice, ContextPartitionSelector selector, boolean isNested) {
        LinkedHashSet<String> expected = new LinkedHashSet<String>();
        expected.add(ContextPartitionSelectorAll.class.getSimpleName());
        if (!isNested) {
            expected.add(ContextPartitionSelectorFiltered.class.getSimpleName());
        }
        expected.add(ContextPartitionSelectorById.class.getSimpleName());
        for (int i = 0; i < choice.length; i++) {
            expected.add(choice[i].getSimpleName());
        }
        String expectedList = CollectionUtil.toString(expected);
        String receivedClass = selector.getClass().getName();
        String message = "Invalid context partition selector, expected an implementation class of any of [" + expectedList + "] interfaces but received " + receivedClass;
        return new InvalidContextPartitionSelector(message);
    }
}
