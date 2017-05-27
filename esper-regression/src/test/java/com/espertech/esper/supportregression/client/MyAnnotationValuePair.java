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

public @interface MyAnnotationValuePair {
    String stringVal();

    byte byteVal();

    short shortVal();

    int intVal();

    long longVal();

    boolean booleanVal();

    char charVal();

    double doubleVal();

    String stringValDef() default "def";

    int intValDef() default 100;

    long longValDef() default 200;

    boolean booleanValDef() default true;

    char charValDef() default 'D';

    double doubleValDef() default 1.1;
}
