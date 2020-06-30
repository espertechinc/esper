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
package com.espertech.esper.common.internal.avro.core;

import com.espertech.esper.common.client.type.EPTypeClass;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericEnumSymbol;
import org.apache.avro.generic.GenericFixed;

public class AvroConstant {
    public final static String PROP_JAVA_STRING_KEY = "avro.java.string";
    public final static String PROP_JAVA_STRING_VALUE = "String";

    public final static EPTypeClass EPTYPE_SCHEMA = new EPTypeClass(Schema.class);
    public final static EPTypeClass EPTYPE_RECORD = new EPTypeClass(GenericData.Record.class);
    public final static EPTypeClass EPTYPE_SCHEMAPARSER = new EPTypeClass(Schema.Parser.class);
    public static final EPTypeClass EPTYPE_GENERICFIXED = new EPTypeClass(GenericFixed.class);
    public static final EPTypeClass EPTYPE_GENERICENUMSYMBOL = new EPTypeClass(GenericEnumSymbol.class);
}
