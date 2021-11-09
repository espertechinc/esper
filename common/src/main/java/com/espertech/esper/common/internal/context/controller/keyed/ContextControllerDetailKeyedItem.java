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
package com.espertech.esper.common.internal.context.controller.keyed;

import com.espertech.esper.common.client.EventPropertyValueGetter;
import com.espertech.esper.common.client.serde.DataInputOutputSerde;
import com.espertech.esper.common.client.type.EPType;
import com.espertech.esper.common.client.type.EPTypeClass;
import com.espertech.esper.common.internal.epl.expression.core.ExprFilterSpecLookupable;
import com.espertech.esper.common.internal.filterspec.FilterSpecActivatable;

public class ContextControllerDetailKeyedItem {
    public final static EPTypeClass EPTYPE = new EPTypeClass(ContextControllerDetailKeyedItem.class);
    public final static EPTypeClass EPTYPEARRAY = new EPTypeClass(ContextControllerDetailKeyedItem[].class);

    private EventPropertyValueGetter getter;
    private FilterSpecActivatable filterSpecActivatable;
    private String optionalInitConditionAsName;
    private ExprFilterSpecLookupable[] lookupables;
    private EPType[] propertyTypes;
    private DataInputOutputSerde keySerde;
    private String aliasName;

    public EventPropertyValueGetter getGetter() {
        return getter;
    }

    public void setGetter(EventPropertyValueGetter getter) {
        this.getter = getter;
    }

    public FilterSpecActivatable getFilterSpecActivatable() {
        return filterSpecActivatable;
    }

    public void setFilterSpecActivatable(FilterSpecActivatable filterSpecActivatable) {
        this.filterSpecActivatable = filterSpecActivatable;
    }

    public String getOptionalInitConditionAsName() {
        return optionalInitConditionAsName;
    }

    public void setOptionalInitConditionAsName(String optionalInitConditionAsName) {
        this.optionalInitConditionAsName = optionalInitConditionAsName;
    }

    public ExprFilterSpecLookupable[] getLookupables() {
        return lookupables;
    }

    public void setLookupables(ExprFilterSpecLookupable[] lookupables) {
        this.lookupables = lookupables;
    }

    public EPType[] getPropertyTypes() {
        return propertyTypes;
    }

    public void setPropertyTypes(EPType[] propertyTypes) {
        this.propertyTypes = propertyTypes;
    }

    public String getAliasName() {
        return aliasName;
    }

    public void setAliasName(String aliasName) {
        this.aliasName = aliasName;
    }

    public DataInputOutputSerde getKeySerde() {
        return keySerde;
    }

    public void setKeySerde(DataInputOutputSerde keySerde) {
        this.keySerde = keySerde;
    }
}
