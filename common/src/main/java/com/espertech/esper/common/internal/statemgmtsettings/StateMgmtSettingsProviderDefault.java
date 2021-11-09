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

import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.util.StateMgmtSetting;
import com.espertech.esper.common.internal.compile.stage1.spec.MatchRecognizeSpec;
import com.espertech.esper.common.internal.compile.stage1.spec.PatternStreamSpecCompiled;
import com.espertech.esper.common.internal.compile.stage2.FilterSpecTracked;
import com.espertech.esper.common.internal.compile.stage2.StatementRawInfo;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompiled;
import com.espertech.esper.common.internal.compile.stage3.ModuleCompileTimeServices;
import com.espertech.esper.common.internal.context.compile.ContextCompileTimeDescriptor;
import com.espertech.esper.common.internal.epl.agg.core.AggregationAttributionKey;
import com.espertech.esper.common.internal.epl.agg.core.AggregationServiceFactoryForge;
import com.espertech.esper.common.internal.epl.classprovided.compiletime.ClassProvidedPrecompileResult;
import com.espertech.esper.common.internal.epl.classprovided.core.ClassProvided;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.pattern.core.PatternAttributionKey;
import com.espertech.esper.common.internal.epl.rowrecog.core.RowRecogDescForge;
import com.espertech.esper.common.internal.epl.table.compiletime.TableAccessAnalysisResult;
import com.espertech.esper.common.internal.fabric.FabricCharge;
import com.espertech.esper.common.internal.fabric.FabricChargeNonHA;
import com.espertech.esper.common.internal.fabric.FabricStatement;
import com.espertech.esper.common.internal.schedule.ScheduleHandleTracked;
import com.espertech.esper.common.internal.view.core.ViewFactoryForge;
import com.espertech.esper.common.internal.view.core.ViewForgeEnv;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

public class StateMgmtSettingsProviderDefault implements StateMgmtSettingsProvider {
    public final static StateMgmtSettingsProviderDefault INSTANCE = new StateMgmtSettingsProviderDefault();

    private StateMgmtSettingsProviderDefault() {
    }

    public FabricCharge newCharge() {
        return FabricChargeNonHA.INSTANCE;
    }

    public StateMgmtSetting view(FabricCharge fabricCharge, int[] grouping, ViewForgeEnv viewForgeEnv, ViewFactoryForge forge) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSettingsProviderContext context() {
        return StateMgmtSettingsProviderContextDefault.INSTANCE;
    }

    public StateMgmtSettingsProviderResultSet resultSet() {
        return StateMgmtSettingsProviderResultSetDefault.INSTANCE;
    }

    public StateMgmtSettingsProviderIndex index() {
        return StateMgmtSettingsProviderIndexDefault.INSTANCE;
    }

    public StateMgmtSetting aggregation(FabricCharge fabricCharge, AggregationAttributionKey attributionKey, StatementRawInfo raw, AggregationServiceFactoryForge forge) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting previous(FabricCharge fabricCharge, StatementRawInfo raw, int stream, Integer subqueryNumber, EventType eventType) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting prior(FabricCharge fabricCharge, StatementRawInfo raw, int streamNum, Integer subqueryNumber, boolean unbound, EventType eventType, SortedSet<Integer> priorRequesteds) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting rowRecogPartitionState(FabricCharge fabricCharge, StatementRawInfo raw, RowRecogDescForge forge, MatchRecognizeSpec spec) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting rowRecogScheduleState(FabricCharge fabricCharge, StatementRawInfo raw, RowRecogDescForge forge, MatchRecognizeSpec spec) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public StateMgmtSetting tableUnkeyed(FabricCharge fabricCharge, String tableName, TableAccessAnalysisResult tableInternalType, StatementRawInfo statementRawInfo) {
        return StateMgmtSettingDefault.INSTANCE;
    }

    public void spec(List<FabricStatement> formatStatements, ModuleCompileTimeServices compileTimeServices, Map<String, byte[]> moduleBytes) {
        throw new IllegalStateException("Not implemented for non-HA compile");
    }

    public FabricStatement statement(int statementNumber, ContextCompileTimeDescriptor context, FabricCharge fabricCharge) {
        return null;
    }

    public void filterViewable(FabricCharge fabricCharge, int stream, boolean isCanIterateUnbound, StatementRawInfo statementRawInfo, EventType eventType) {
        // no action
    }

    public void filterNonContext(FabricCharge fabricCharge, FilterSpecTracked spec) {
        // no action
    }

    public void namedWindow(FabricCharge fabricCharge, StatementRawInfo statementRawInfo, NamedWindowMetaData metaData, EventType eventType) {
        // no action
    }

    public void table(FabricCharge fabricCharge, String tableName, TableAccessAnalysisResult plan, StatementRawInfo statementRawInfo) {
        // no action
    }

    public void pattern(FabricCharge fabricCharge, PatternAttributionKey attributionKey, PatternStreamSpecCompiled patternStreamSpec, StatementRawInfo raw) {
        // no action
    }

    public void inlinedClassesLocal(FabricCharge fabricCharge, ClassProvidedPrecompileResult classesInlined) {
        // no action
    }

    public void inlinedClasses(FabricCharge fabricCharge, ClassProvided classProvided) {
        // no action
    }

    public void filterSubtypes(FabricCharge fabricCharge, List<FilterSpecTracked> provider, ContextCompileTimeDescriptor contextDescriptor, StatementSpecCompiled compiled) {
        // no action
    }

    public void historicalExpiryTime(FabricCharge fabricCharge, int streamNum) {
        // no action
    }

    public void schedules(FabricCharge fabricCharge, List<ScheduleHandleTracked> trackeds) {
        // no action
    }
}
