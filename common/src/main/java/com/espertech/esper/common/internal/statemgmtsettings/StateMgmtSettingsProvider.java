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
import com.espertech.esper.common.internal.fabric.FabricStatement;
import com.espertech.esper.common.internal.schedule.ScheduleHandleTracked;
import com.espertech.esper.common.internal.view.core.ViewFactoryForge;
import com.espertech.esper.common.internal.view.core.ViewForgeEnv;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

public interface StateMgmtSettingsProvider {
    FabricCharge newCharge();

    void spec(List<FabricStatement> formatStatements, ModuleCompileTimeServices compileTimeServices, Map<String, byte[]> moduleBytes);
    FabricStatement statement(int statementNumber, ContextCompileTimeDescriptor context, FabricCharge fabricCharge);

    StateMgmtSettingsProviderContext context();
    StateMgmtSettingsProviderResultSet resultSet();
    StateMgmtSettingsProviderIndex index();

    StateMgmtSetting view(FabricCharge fabricCharge, int[] grouping, ViewForgeEnv viewForgeEnv, ViewFactoryForge forge);
    StateMgmtSetting aggregation(FabricCharge fabricCharge, AggregationAttributionKey attributionKey, StatementRawInfo raw, AggregationServiceFactoryForge forge);
    StateMgmtSetting rowRecogPartitionState(FabricCharge fabricCharge, StatementRawInfo raw, RowRecogDescForge forge, MatchRecognizeSpec spec);
    StateMgmtSetting rowRecogScheduleState(FabricCharge fabricCharge, StatementRawInfo raw, RowRecogDescForge forge, MatchRecognizeSpec spec);
    StateMgmtSetting previous(FabricCharge fabricCharge, StatementRawInfo raw, int stream, Integer subqueryNumber, EventType eventType);
    StateMgmtSetting prior(FabricCharge fabricCharge, StatementRawInfo raw, int streamNum, Integer subqueryNumber, boolean unbound, EventType eventType, SortedSet<Integer> priorRequesteds);
    StateMgmtSetting tableUnkeyed(FabricCharge fabricCharge, String tableName, TableAccessAnalysisResult tableInternalType, StatementRawInfo statementRawInfo);

    void filterViewable(FabricCharge fabricCharge, int stream, boolean isCanIterateUnbound, StatementRawInfo statementRawInfo, EventType eventType);
    void filterNonContext(FabricCharge fabricCharge, FilterSpecTracked spec);
    void filterSubtypes(FabricCharge fabricCharge, List<FilterSpecTracked> provider, ContextCompileTimeDescriptor contextDescriptor, StatementSpecCompiled compiled);
    void pattern(FabricCharge fabricCharge, PatternAttributionKey attributionKey, PatternStreamSpecCompiled patternStreamSpec, StatementRawInfo raw);
    void namedWindow(FabricCharge fabricCharge, StatementRawInfo statementRawInfo, NamedWindowMetaData metaData, EventType eventType);
    void table(FabricCharge fabricCharge, String tableName, TableAccessAnalysisResult plan, StatementRawInfo statementRawInfo);
    void inlinedClassesLocal(FabricCharge fabricCharge, ClassProvidedPrecompileResult classesInlined);
    void inlinedClasses(FabricCharge fabricCharge, ClassProvided classProvided);
    void historicalExpiryTime(FabricCharge fabricCharge, int streamNum);
    void schedules(FabricCharge fabricCharge, List<ScheduleHandleTracked> trackeds);
}
