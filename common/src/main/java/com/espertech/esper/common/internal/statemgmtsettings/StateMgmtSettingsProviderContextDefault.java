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
package com.espertech.esper.common.internal.statemgmtsettings;

import com.espertech.esper.common.client.util.StateMgmtSetting;
import com.espertech.esper.common.internal.compile.stage1.spec.ContextSpecHashItem;
import com.espertech.esper.common.internal.compile.stage1.spec.ContextSpecKeyedItem;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.context.compile.ContextMetaData;
import com.espertech.esper.common.internal.context.controller.category.ContextControllerCategoryFactoryForge;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerFactoryForge;
import com.espertech.esper.common.internal.context.controller.hash.ContextControllerHashFactoryForge;
import com.espertech.esper.common.internal.context.controller.initterm.ContextControllerInitTermFactoryForge;
import com.espertech.esper.common.internal.context.controller.keyed.ContextControllerKeyedFactoryForge;
import com.espertech.esper.common.internal.fabric.FabricCharge;

import java.util.List;

public class StateMgmtSettingsProviderContextDefault implements StateMgmtSettingsProviderContext {
    public final static StateMgmtSettingsProviderContextDefault INSTANCE = new StateMgmtSettingsProviderContextDefault();

    private StateMgmtSettingsProviderContextDefault() {
    }

    public void context(FabricCharge fabricCharge, ContextMetaData detail, ContextControllerFactoryForge[] controllerFactoryForges) {
        // no action
    }

    public void filterContextKeyed(FabricCharge fabricCharge, List<ContextSpecKeyedItem> items) {
        // no action
    }

    public void filterContextHash(FabricCharge fabricCharge, List<ContextSpecHashItem> items) {
        // no action
    }

    public StateMgmtSetting contextPartitionId(FabricCharge fabricCharge, StatementRawInfo statementRawInfo, ContextMetaData contextMetaData) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting contextCategory(FabricCharge fabricCharge, ContextMetaData detail, ContextControllerCategoryFactoryForge forge, StatementRawInfo raw, int controllerLevel) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting contextHash(FabricCharge fabricCharge, ContextMetaData detail, ContextControllerHashFactoryForge forge, StatementRawInfo raw, int controllerLevel) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting contextKeyed(FabricCharge fabricCharge, ContextMetaData detail, ContextControllerKeyedFactoryForge forge, StatementRawInfo raw, int controllerLevel) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting contextKeyedTerm(FabricCharge fabricCharge, ContextMetaData detail, ContextControllerKeyedFactoryForge forge, StatementRawInfo raw, int controllerLevel) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting contextInitTerm(FabricCharge fabricCharge, ContextMetaData detail, ContextControllerInitTermFactoryForge forge, StatementRawInfo raw, int controllerLevel) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting contextInitTermDistinct(FabricCharge fabricCharge, ContextMetaData detail, ContextControllerInitTermFactoryForge forge, StatementRawInfo raw, int controllerLevel) {
        return StateMgmtSettingDefault.INSTANCE;
    }
}
