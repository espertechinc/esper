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
package com.espertech.esper.supportregression.bean;

public interface SupportBeanConstants {
    public final static String EVENT_BEAN_PACKAGE = SupportBean.class.getPackage().getName() + ".";

    public final static String EVENT_A_CLASS = EVENT_BEAN_PACKAGE + "SupportBean_A";
    public final static String EVENT_B_CLASS = EVENT_BEAN_PACKAGE + "SupportBean_B";
    public final static String EVENT_C_CLASS = EVENT_BEAN_PACKAGE + "SupportBean_C";
    public final static String EVENT_D_CLASS = EVENT_BEAN_PACKAGE + "SupportBean_D";
    public final static String EVENT_E_CLASS = EVENT_BEAN_PACKAGE + "SupportBean_E";
    public final static String EVENT_F_CLASS = EVENT_BEAN_PACKAGE + "SupportBean_F";
    public final static String EVENT_G_CLASS = EVENT_BEAN_PACKAGE + "SupportBean_G";
}
