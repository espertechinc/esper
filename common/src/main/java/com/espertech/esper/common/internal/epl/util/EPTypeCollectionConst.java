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
package com.espertech.esper.common.internal.epl.util;

import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.client.type.EPTypeClassParameterized;
import com.espertech.esper.common.client.type.EPTypePremade;
import com.espertech.esper.common.internal.collection.MultiKeyArrayOfKeys;
import com.espertech.esper.common.internal.collection.UniformPair;
import com.espertech.esper.common.internal.epl.agg.core.AggregationRow;
import com.espertech.esper.common.internal.epl.agg.groupbylocal.AggSvcLocalGroupLevelKeyPair;
import com.espertech.esper.common.internal.epl.agg.rollup.GroupByRollupKey;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EPTypeCollectionConst {
    public final static EPTypeClass EPTYPE_MAP_OBJECT_OBJECT = new EPTypeClassParameterized(Map.class, EPTypePremade.OBJECT.getEPType(), EPTypePremade.OBJECT.getEPType());
    public final static EPTypeClass EPTYPE_MAP_STRING_OBJECT = new EPTypeClassParameterized(Map.class, EPTypePremade.STRING.getEPType(), EPTypePremade.OBJECT.getEPType());
    public final static EPTypeClass EPTYPE_MAP_OBJECT_EVENTBEAN = new EPTypeClassParameterized(Map.class, EPTypePremade.OBJECT.getEPType(), EventBean.EPTYPE);
    public final static EPTypeClass EPTYPE_MAP_OBJECT_EVENTBEANARRAY = new EPTypeClassParameterized(Map.class, EPTypePremade.OBJECT.getEPType(), EventBean.EPTYPEARRAY);
    public final static EPTypeClass EPTYPE_MAP_OBJECT_AGGROW = new EPTypeClassParameterized(Map.class, EPTypePremade.OBJECT.getEPType(), AggregationRow.EPTYPE);

    public final static EPTypeClass EPTYPE_MAPARRAY_OBJECT_EVENTBEANARRAY = new EPTypeClassParameterized(Map[].class, EPTypePremade.OBJECT.getEPType(), EventBean.EPTYPEARRAY);
    public final static EPTypeClass EPTYPE_MAPARRAY_OBJECT_EVENTBEAN = new EPTypeClassParameterized(Map[].class, EPTypePremade.OBJECT.getEPType(), EventBean.EPTYPE);
    public final static EPTypeClass EPTYPE_MAPARRAY_OBJECT_AGGROW = new EPTypeClassParameterized(Map[].class, EPTypePremade.OBJECT.getEPType(), AggregationRow.EPTYPE);

    public final static EPTypeClass EPTYPE_MULTIKEYARRAYOFKEYS_EVENTBEAN = new EPTypeClassParameterized(MultiKeyArrayOfKeys.EPTYPE.getType(), EventBean.EPTYPE);
    public final static EPTypeClass EPTYPE_SET_MULTIKEYARRAYOFKEYS_EVENTBEAN = new EPTypeClassParameterized(Set.class, EPTYPE_MULTIKEYARRAYOFKEYS_EVENTBEAN);

    public final static EPTypeClass EPTYPE_MULTIKEYARRAYOFKEYS_EVENTBEANARRAY = new EPTypeClassParameterized(MultiKeyArrayOfKeys.EPTYPE.getType(), EventBean.EPTYPEARRAY);
    public final static EPTypeClass EPTYPE_SET_MULTIKEYARRAYOFKEYS_EVENTBEANARRAY = new EPTypeClassParameterized(Set.class, EPTYPE_MULTIKEYARRAYOFKEYS_EVENTBEANARRAY);

    public final static EPTypeClass EPTYPE_COLLECTION_EVENTBEAN = new EPTypeClassParameterized(Collection.class, EventBean.EPTYPE);
    public final static EPTypeClass EPTYPE_COLLECTION_NUMBER = new EPTypeClassParameterized(Collection.class, EPTypePremade.NUMBER.getEPType());

    public final static EPTypeClass EPTYPE_LIST_OBJECT = new EPTypeClassParameterized(List.class, EPTypePremade.OBJECT.getEPType());
    public final static EPTypeClass EPTYPE_LIST_UNIFORMPAIR_EVENTBEANARRAY = new EPTypeClassParameterized(List.class, new EPTypeClassParameterized(UniformPair.class, EventBean.EPTYPEARRAY));
    public final static EPTypeClass EPTYPE_LIST_GROUPBYROLLUPKEY = new EPTypeClassParameterized(List.class, GroupByRollupKey.EPTYPE);
    public final static EPTypeClass EPTYPE_LIST_UNIFORMPAIR_SET_MKARRAY_EVENTBEAN = new EPTypeClassParameterized(List.class, new EPTypeClassParameterized(UniformPair.class, EPTYPE_SET_MULTIKEYARRAYOFKEYS_EVENTBEAN));
    public final static EPTypeClass EPTYPE_LIST_AFFLOCALGROUPPAIR = new EPTypeClassParameterized(List.class, AggSvcLocalGroupLevelKeyPair.EPTYPE);
}
