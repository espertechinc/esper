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
package com.espertech.esper.common.internal.event.eventtyperepo;

import com.espertech.esper.common.client.configuration.common.ConfigurationCommonEventTypeWithSupertype;
import junit.framework.TestCase;

import java.util.*;

public class TestEventTypeRepositoryUtil extends TestCase {
    public void testSort() {
        Set<String> setOne = new LinkedHashSet<>();
        setOne.add("a");
        setOne.add("b_sub");
        Set<String> setTwo = new LinkedHashSet<>();
        setOne.add("b_super");
        setOne.add("y");

        Map<String, ConfigurationCommonEventTypeWithSupertype> configs = new HashMap<>();
        ConfigurationCommonEventTypeWithSupertype config = new ConfigurationCommonEventTypeWithSupertype();
        config.setSuperTypes(Collections.singleton("b_super"));
        configs.put("b_sub", config);

        List<String> result = EventTypeRepositoryUtil.getCreationOrder(setOne, setTwo, configs);
        assertEquals("[a, b_super, y, b_sub]", result.toString());
    }
}
