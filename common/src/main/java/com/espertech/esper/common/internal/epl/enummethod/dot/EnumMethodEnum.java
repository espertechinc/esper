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
package com.espertech.esper.common.internal.epl.enummethod.dot;

public enum EnumMethodEnum {

    AGGREGATE,

    ALLOF,
    ANYOF,

    TOMAP,
    GROUPBY,

    COUNTOF,
    MIN,
    MAX,
    AVERAGE,
    SUMOF,

    MOSTFREQUENT,
    LEASTFREQUENT,

    SELECTFROM,

    FIRSTOF,
    LASTOF,
    MINBY,
    MAXBY,

    TAKE,
    TAKELAST,
    TAKEWHILE,
    TAKEWHILELAST,
    ORDERBY,
    ORDERBYDESC,
    DISTINCTOF,
    WHERE,
    UNION,
    EXCEPT,
    INTERSECT,
    REVERSE,
    ESPERINTERNALNOOP,

    SEQUENCEEQUAL,

    PLUGIN;
}
