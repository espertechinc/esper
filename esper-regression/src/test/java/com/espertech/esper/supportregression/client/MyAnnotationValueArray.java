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
package com.espertech.esper.supportregression.client;

public @interface MyAnnotationValueArray {
    public abstract long[] value();

    public abstract int[] intArray();

    public abstract double[] doubleArray();

    public abstract String[] stringArray();

    public abstract String[] stringArrayDef() default {"XYZ"};
}
