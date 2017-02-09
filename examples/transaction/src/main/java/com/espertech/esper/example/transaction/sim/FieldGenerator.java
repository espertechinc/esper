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
package com.espertech.esper.example.transaction.sim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Utils that generate random entries for various fields.
 *
 * @author Hans Gilde
 */
public class FieldGenerator {
    private final Random random = RandomUtil.getNewInstance();

    public static final List<String> CUSTOMERS;

    static {
        List<String> l = new ArrayList<String>();
        l.add("RED");
        l.add("ORANGE");
        l.add("YELLOW");
        l.add("GREEN");
        l.add("BLUE");
        l.add("INDIGO");
        l.add("VIOLET");
        CUSTOMERS = Collections.unmodifiableList(l);
    }

    public static final List<String> SUPPLIERS;

    static {
        List<String> l = new ArrayList<String>();
        l.add("WASHINGTON");
        l.add("ADAMS");
        l.add("JEFFERSON");
        l.add("MADISON");
        l.add("MONROE");
        SUPPLIERS = Collections.unmodifiableList(l);
    }

    public String getRandomCustomer() {
        return CUSTOMERS.get(random.nextInt(CUSTOMERS.size() - 1));
    }

    public String getRandomSupplier() {
        return SUPPLIERS.get(random.nextInt(SUPPLIERS.size() - 1));
    }

    public long randomLatency(long time) {
        return time + random.nextInt(1000);
    }
}
