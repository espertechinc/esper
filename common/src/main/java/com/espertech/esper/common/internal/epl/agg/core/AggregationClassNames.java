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
package com.espertech.esper.common.internal.epl.agg.core;

public class AggregationClassNames {
    private final static String CLASSNAME_AGGREGATIONSERVICEFACTORYPROVIDER = "AggFactoryProvider";
    private final static String CLASSNAME_AGGREGATIONSERVICEFACTORY = "AggFactory";
    private final static String CLASSNAME_AGGREGATIONSERVICE = "AggSvc";
    private final static String CLASSNAME_AGGREGATIONROW_TOP = "AggRowTop";
    private final static String CLASSNAME_AGGREGATIONROW_LVL = "AggRowLvl";
    private final static String CLASSNAME_AGGREGATIONROWFACTORY = "AggRowFactoryTop";
    private final static String CLASSNAME_AGGREGATIONROWSERDE = "AggRowSerdeTop";
    private final static String CLASSNAME_AGGREGATIONROWSERDE_LVL = "AggRowSerdeLvl";
    private final static String CLASSNAME_AGGREGATIONROWFACTORY_LVL = "AggRowFactoryLvl";

    private final String optionalPostfix;
    private String rowTop = CLASSNAME_AGGREGATIONROW_TOP;
    private String rowFactory = CLASSNAME_AGGREGATIONROWFACTORY;
    private String rowSerde = CLASSNAME_AGGREGATIONROWSERDE;
    private String provider = CLASSNAME_AGGREGATIONSERVICEFACTORYPROVIDER;
    private String service = CLASSNAME_AGGREGATIONSERVICE;
    private String serviceFactory = CLASSNAME_AGGREGATIONSERVICEFACTORY;

    public AggregationClassNames() {
        this(null);
    }

    public AggregationClassNames(String optionalPostfix) {
        this.optionalPostfix = optionalPostfix;
        if (optionalPostfix != null) {
            rowTop += optionalPostfix;
            rowFactory += optionalPostfix;
            rowSerde += optionalPostfix;
            provider += optionalPostfix;
            service += optionalPostfix;
            serviceFactory += optionalPostfix;
        }
    }

    public String getRowTop() {
        return rowTop;
    }

    public String getRowFactoryTop() {
        return rowFactory;
    }

    public String getRowSerdeTop() {
        return rowSerde;
    }

    public String getRowPerLevel(int level) {
        String name = CLASSNAME_AGGREGATIONROW_LVL + "_" + level;
        if (optionalPostfix != null) {
            name += optionalPostfix;
        }
        return name;
    }

    public String getRowSerdePerLevel(int level) {
        String name = CLASSNAME_AGGREGATIONROWSERDE_LVL + "_" + level;
        if (optionalPostfix != null) {
            name += optionalPostfix;
        }
        return name;
    }

    public String getRowFactoryPerLevel(int level) {
        String name = CLASSNAME_AGGREGATIONROWFACTORY_LVL + "_" + level;
        if (optionalPostfix != null) {
            name += optionalPostfix;
        }
        return name;
    }

    public String getProvider() {
        return provider;
    }

    public String getService() {
        return service;
    }

    public String getServiceFactory() {
        return serviceFactory;
    }
}
