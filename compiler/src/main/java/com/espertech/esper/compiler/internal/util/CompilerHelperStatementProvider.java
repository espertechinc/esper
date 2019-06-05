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
package com.espertech.esper.compiler.internal.util;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventType;
import com.espertech.esper.common.client.annotation.HookType;
import com.espertech.esper.common.client.annotation.Name;
import com.espertech.esper.common.client.util.StatementType;
import com.espertech.esper.common.internal.bytecodemodel.base.CodegenPackageScope;
import com.espertech.esper.common.internal.bytecodemodel.core.CodeGenerationIDGenerator;
import com.espertech.esper.common.internal.bytecodemodel.core.CodegenClass;
import com.espertech.esper.common.internal.bytecodemodel.util.IdentifierUtil;
import com.espertech.esper.common.internal.compile.stage1.Compilable;
import com.espertech.esper.common.internal.compile.stage1.spec.*;
import com.espertech.esper.common.internal.compile.stage2.*;
import com.espertech.esper.common.internal.compile.stage3.*;
import com.espertech.esper.common.internal.context.aifactory.createcontext.StmtForgeMethodCreateContext;
import com.espertech.esper.common.internal.context.aifactory.createdataflow.StmtForgeMethodCreateDataflow;
import com.espertech.esper.common.internal.context.aifactory.createexpression.StmtForgeMethodCreateExpression;
import com.espertech.esper.common.internal.context.aifactory.createindex.StmtForgeMethodCreateIndex;
import com.espertech.esper.common.internal.context.aifactory.createschema.StmtForgeMethodCreateSchema;
import com.espertech.esper.common.internal.context.aifactory.createtable.StmtForgeMethodCreateTable;
import com.espertech.esper.common.internal.context.aifactory.createvariable.StmtForgeMethodCreateVariable;
import com.espertech.esper.common.internal.context.aifactory.createwindow.StmtForgeMethodCreateWindow;
import com.espertech.esper.common.internal.context.aifactory.ontrigger.core.StmtForgeMethodOnTrigger;
import com.espertech.esper.common.internal.context.aifactory.select.StmtForgeMethodSelect;
import com.espertech.esper.common.internal.context.aifactory.update.StmtForgeMethodUpdate;
import com.espertech.esper.common.internal.context.compile.ContextCompileTimeDescriptor;
import com.espertech.esper.common.internal.context.compile.ContextMetaData;
import com.espertech.esper.common.internal.context.controller.core.ContextControllerPortableInfo;
import com.espertech.esper.common.internal.context.module.StatementProvider;
import com.espertech.esper.common.internal.context.util.ContextPropertyRegistry;
import com.espertech.esper.common.internal.epl.annotation.AnnotationUtil;
import com.espertech.esper.common.internal.epl.expression.core.ExprChainedSpec;
import com.espertech.esper.common.internal.epl.expression.core.ExprNode;
import com.espertech.esper.common.internal.epl.expression.core.ExprValidationException;
import com.espertech.esper.common.internal.epl.expression.declared.compiletime.ExprDeclaredNode;
import com.espertech.esper.common.internal.epl.expression.dot.core.ExprDotNode;
import com.espertech.esper.common.internal.epl.expression.ops.ExprAndNode;
import com.espertech.esper.common.internal.epl.expression.ops.ExprAndNodeImpl;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectNode;
import com.espertech.esper.common.internal.epl.expression.subquery.ExprSubselectRowNode;
import com.espertech.esper.common.internal.epl.expression.table.ExprTableAccessNode;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeSubselectDeclaredDotVisitor;
import com.espertech.esper.common.internal.epl.expression.visitor.ExprNodeTableAccessVisitor;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowCompileTimeResolver;
import com.espertech.esper.common.internal.epl.namedwindow.path.NamedWindowMetaData;
import com.espertech.esper.common.internal.epl.script.core.ScriptValidationPrecompileUtil;
import com.espertech.esper.common.internal.epl.util.StatementSpecRawWalkerSubselectAndDeclaredDot;
import com.espertech.esper.common.internal.event.json.core.JsonEventType;
import com.espertech.esper.common.internal.filterspec.FilterSpecParamExprNodeForge;
import com.espertech.esper.common.internal.schedule.ScheduleHandleCallbackProvider;
import com.espertech.esper.common.internal.settings.ClasspathImportUtil;
import com.espertech.esper.compiler.client.CompilerOptions;
import com.espertech.esper.compiler.client.option.StatementNameContext;
import com.espertech.esper.compiler.client.option.StatementUserObjectContext;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.*;

import static com.espertech.esper.compiler.internal.util.CompilerHelperSingleEPL.parseWalk;
import static com.espertech.esper.compiler.internal.util.CompilerHelperValidator.verifySubstitutionParams;

public class CompilerHelperStatementProvider {

    static CompilableItem compileItem(Compilable compilable,
                                      String optionalModuleName,
                                      String moduleIdentPostfix,
                                      int statementNumber,
                                      Set<String> statementNames,
                                      StatementCompileTimeServices compileTimeServices,
                                      CompilerOptions compilerOptions)
        throws StatementSpecCompileException {

        // Stage 1 - parse statement
        StatementSpecRaw raw = parseWalk(compilable, compileTimeServices);

        try {
            // Stage 2(a) - precompile: compile annotations
            Annotation[] annotations = AnnotationUtil.compileAnnotations(raw.getAnnotations(), compileTimeServices.getClasspathImportServiceCompileTime(), compilable);

            // Stage 2(b) - walk subselects, alias expressions, declared expressions, dot-expressions
            ExprNodeSubselectDeclaredDotVisitor visitor;
            try {
                visitor = StatementSpecRawWalkerSubselectAndDeclaredDot.walkSubselectAndDeclaredDotExpr(raw);
            } catch (ExprValidationException ex) {
                throw new StatementSpecCompileException(ex.getMessage(), compilable.toEPL());
            }
            List<ExprSubselectNode> subselectNodes = visitor.getSubselects();

            // Determine a statement name
            String statementNameProvided = getNameFromAnnotation(annotations);
            if (compilerOptions.getStatementName() != null) {
                String assignedName = compilerOptions.getStatementName().getValue(new StatementNameContext(() -> compilable.toEPL(), statementNameProvided, optionalModuleName, annotations, statementNumber));
                if (assignedName != null) {
                    statementNameProvided = assignedName;
                }
            }

            String statementName = statementNameProvided == null ? ("stmt-" + statementNumber) : statementNameProvided;
            if (statementNames.contains(statementName)) {
                int count = 1;
                String newStatementName = statementName + "-" + count;
                while (statementNames.contains(newStatementName)) {
                    count++;
                    newStatementName = statementName + "-" + count;
                }
                statementName = newStatementName;
            }
            statementName = statementName.trim();

            statementNames.add(statementName);

            // Determine table access nodes
            List<ExprTableAccessNode> tableAccessNodes = determineTableAccessNodes(raw.getTableExpressions(), visitor);

            // compile scripts once in this central place, may also compile later in expression
            ScriptValidationPrecompileUtil.validateScripts(raw.getScriptExpressions(), raw.getExpressionDeclDesc(), compileTimeServices);

            // Determine subselects for compilation, and lambda-expression shortcut syntax for named windows
            if (!visitor.getChainedExpressionsDot().isEmpty()) {
                rewriteNamedWindowSubselect(visitor.getChainedExpressionsDot(), subselectNodes, compileTimeServices.getNamedWindowCompileTimeResolver());
            }

            // Stage 2(c) compile context descriptor
            ContextCompileTimeDescriptor contextDescriptor = null;
            String optionalContextName = raw.getOptionalContextName();
            if (optionalContextName != null) {
                ContextMetaData detail = compileTimeServices.getContextCompileTimeResolver().getContextInfo(optionalContextName);
                if (detail == null) {
                    throw new StatementSpecCompileException("Context by name '" + optionalContextName + "' could not be found", compilable.toEPL());
                }
                contextDescriptor = new ContextCompileTimeDescriptor(optionalContextName, detail.getContextModuleName(), detail.getContextVisibility(), new ContextPropertyRegistry(detail), detail.getValidationInfos());
            }

            // Stage 2(d) compile raw statement spec
            StatementType statementType = StatementTypeUtil.getStatementType(raw);
            StatementRawInfo statementRawInfo = new StatementRawInfo(statementNumber, statementName, annotations, statementType, contextDescriptor, raw.getIntoTableSpec() == null ? null : raw.getIntoTableSpec().getName(), compilable, optionalModuleName);
            StatementSpecCompiledDesc compiledDesc = StatementRawCompiler.compile(raw, compilable, false, false, annotations, subselectNodes, tableAccessNodes, statementRawInfo, compileTimeServices);
            StatementSpecCompiled specCompiled = compiledDesc.getCompiled();
            String statementIdentPostfix = IdentifierUtil.getIdentifierMayStartNumeric(statementName);

            // get compile-time user object
            Serializable userObjectCompileTime = null;
            if (compilerOptions.getStatementUserObject() != null) {
                userObjectCompileTime = compilerOptions.getStatementUserObject().getValue(new StatementUserObjectContext(() -> compilable.toEPL(), statementName, optionalModuleName, annotations, statementNumber));
            }

            // handle hooks
            handleStatementCompileHook(annotations, compileTimeServices, specCompiled);

            // Stage 3(a) - statement-type-specific forge building
            StatementBaseInfo base = new StatementBaseInfo(compilable, specCompiled, userObjectCompileTime, statementRawInfo, optionalModuleName);
            StmtForgeMethod forgeMethod;
            if (raw.getUpdateDesc() != null) {
                forgeMethod = new StmtForgeMethodUpdate(base);
            } else if (raw.getOnTriggerDesc() != null) {
                forgeMethod = new StmtForgeMethodOnTrigger(base);
            } else if (raw.getCreateIndexDesc() != null) {
                forgeMethod = new StmtForgeMethodCreateIndex(base);
            } else if (raw.getCreateVariableDesc() != null) {
                forgeMethod = new StmtForgeMethodCreateVariable(base);
            } else if (raw.getCreateDataFlowDesc() != null) {
                forgeMethod = new StmtForgeMethodCreateDataflow(base);
            } else if (raw.getCreateTableDesc() != null) {
                forgeMethod = new StmtForgeMethodCreateTable(base);
            } else if (raw.getCreateExpressionDesc() != null) {
                forgeMethod = new StmtForgeMethodCreateExpression(base);
            } else if (raw.getCreateWindowDesc() != null) {
                forgeMethod = new StmtForgeMethodCreateWindow(base);
            } else if (raw.getCreateContextDesc() != null) {
                forgeMethod = new StmtForgeMethodCreateContext(base);
            } else if (raw.getCreateSchemaDesc() != null) {
                forgeMethod = new StmtForgeMethodCreateSchema(base);
            } else {
                forgeMethod = new StmtForgeMethodSelect(base);
            }

            // check context-validity conditions for this statement
            if (contextDescriptor != null) {
                try {
                    for (ContextControllerPortableInfo validator : contextDescriptor.getValidationInfos()) {
                        validator.validateStatement(contextDescriptor.getContextName(), specCompiled, compileTimeServices);
                    }
                } catch (ExprValidationException ex) {
                    throw new StatementSpecCompileException(ex.getMessage(), ex, compilable.toEPL());
                }
            }

            // Stage 3(b) - forge-factory-to-forge
            String classPostfix = moduleIdentPostfix + "_" + statementIdentPostfix;
            List<StmtClassForgeable> forgeables = new ArrayList<>();

            // add forgeables from filter-related processing i.e. multikeys
            for (StmtClassForgeableFactory additional : compiledDesc.getAdditionalForgeables()) {
                CodegenPackageScope packageScope = new CodegenPackageScope(compileTimeServices.getPackageName(), null, false);
                forgeables.add(additional.make(packageScope, classPostfix));
            }

            List<FilterSpecCompiled> filterSpecCompileds = new ArrayList<>();
            List<ScheduleHandleCallbackProvider> scheduleHandleCallbackProviders = new ArrayList<>();
            List<NamedWindowConsumerStreamSpec> namedWindowConsumers = new ArrayList<>();
            List<FilterSpecParamExprNodeForge> filterBooleanExpressions = new ArrayList<>();
            StmtForgeMethodResult result = forgeMethod.make(compileTimeServices.getPackageName(), classPostfix, compileTimeServices);
            forgeables.addAll(result.getForgeables());
            verifyForgeables(forgeables);

            filterSpecCompileds.addAll(result.getFiltereds());
            scheduleHandleCallbackProviders.addAll(result.getScheduleds());
            namedWindowConsumers.addAll(result.getNamedWindowConsumers());
            filterBooleanExpressions.addAll(result.getFilterBooleanExpressions());

            // Stage 3(c) - filter assignments: assign filter callback ids and filter-path-num for boolean expressions
            int filterId = -1;
            for (FilterSpecCompiled provider : filterSpecCompileds) {
                int assigned = ++filterId;
                provider.setFilterCallbackId(assigned);
            }

            // Stage 3(d) - schedule assignments: assign schedule callback ids
            int scheduleId = 0;
            for (ScheduleHandleCallbackProvider provider : scheduleHandleCallbackProviders) {
                provider.setScheduleCallbackId(scheduleId++);
            }

            // Stage 3(e) - named window consumers: assign consumer id
            int namedWindowConsumerId = 0;
            for (NamedWindowConsumerStreamSpec provider : namedWindowConsumers) {
                provider.setNamedWindowConsumerId(namedWindowConsumerId++);
            }

            // Stage 3(f) - filter boolean expression id assignment
            int filterBooleanExprNum = 0;
            for (FilterSpecParamExprNodeForge expr : filterBooleanExpressions) {
                expr.setFilterBoolExprId(filterBooleanExprNum++);
            }

            // Stage 3(f) - verify substitution parameters
            verifySubstitutionParams(raw.getSubstitutionParameters());

            // Stage 4 - forge-to-class (forge with statement-fields last)
            List<CodegenClass> classes = new ArrayList<>(forgeables.size());
            for (StmtClassForgeable forgeable : forgeables) {
                CodegenClass clazz = forgeable.forge(true, false);
                classes.add(clazz);
            }

            // Stage 5 - refactor methods to make sure the constant pool does not grow too large for any given class
            CompilerHelperRefactorToStaticMethods.refactorMethods(classes, compileTimeServices.getConfiguration().getCompiler().getByteCode().getMaxMethodsPerClass());

            // Stage 6 - sort to make the "fields" class first and all the rest later
            classes.sort((o1, o2) -> Integer.compare(o1.getClassType().getSortCode(), o2.getClassType().getSortCode()));

            // We are making sure JsonEventType receives the underlying class itself
            CompilableItemPostCompileLatch postCompile = CompilableItemPostCompileLatchDefault.INSTANCE;
            for (EventType eventType : compileTimeServices.getEventTypeCompileTimeRegistry().getNewTypesAdded()) {
                if (eventType instanceof JsonEventType) {
                    postCompile = new CompilableItemPostCompileLatchJson(compileTimeServices.getEventTypeCompileTimeRegistry().getNewTypesAdded(), compileTimeServices.getParentClassLoader());
                    break;
                }
            }

            String statementProviderClassName = CodeGenerationIDGenerator.generateClassNameWithPackage(compileTimeServices.getPackageName(), StatementProvider.class, classPostfix);
            return new CompilableItem(statementProviderClassName, classes, postCompile);
        } catch (StatementSpecCompileException ex) {
            throw ex;
        } catch (ExprValidationException ex) {
            throw new StatementSpecCompileException(ex.getMessage(), ex, compilable.toEPL());
        } catch (EPException ex) {
            throw new StatementSpecCompileException(ex.getMessage(), ex, compilable.toEPL());
        } catch (Throwable t) {
            String text = t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
            throw new StatementSpecCompileException(text, t, compilable.toEPL());
        }
    }

    private static void verifyForgeables(List<StmtClassForgeable> forgeables) {
        // there can only be one class of the same name
        Set<String> names = new HashSet<>();
        for (StmtClassForgeable forgeable : forgeables) {
            if (names.contains(forgeable.getClassName())) {
                throw new IllegalStateException("Class name '" + forgeable.getClassName() + "' appears twice");
            }
            names.add(forgeable.getClassName());
        }

        // there can be only one fields and statement provider
        StmtClassForgeable stmtProvider = null;
        for (StmtClassForgeable forgeable : forgeables) {
            if (forgeable.getForgeableType() == StmtClassForgeableType.STMTPROVIDER) {
                if (stmtProvider != null) {
                    throw new IllegalStateException("Multiple stmt-provider classes");
                }
                stmtProvider = forgeable;
            }
        }
    }

    private static void handleStatementCompileHook(Annotation[] annotations, StatementCompileTimeServices compileTimeServices, StatementSpecCompiled specCompiled) {
        StatementCompileHook compileHook = null;
        try {
            compileHook = (StatementCompileHook) ClasspathImportUtil.getAnnotationHook(annotations, HookType.INTERNAL_COMPILE, StatementCompileHook.class, compileTimeServices.getClasspathImportServiceCompileTime());
        } catch (ExprValidationException e) {
            throw new EPException("Failed to obtain hook for " + HookType.INTERNAL_QUERY_PLAN);
        }
        if (compileHook != null) {
            compileHook.compiled(specCompiled);
        }
    }

    protected static String getNameFromAnnotation(Annotation[] annotations) {
        if (annotations != null && annotations.length != 0) {
            for (Annotation annotation : annotations) {
                if (annotation instanceof Name) {
                    Name name = (Name) annotation;
                    if (name.value() != null) {
                        return name.value();
                    }
                }
            }
        }
        return null;
    }

    private static void rewriteNamedWindowSubselect(List<ExprDotNode> chainedExpressionsDot, List<ExprSubselectNode> subselects, NamedWindowCompileTimeResolver service) {
        for (ExprDotNode dotNode : chainedExpressionsDot) {
            String proposedWindow = dotNode.getChainSpec().get(0).getName();
            NamedWindowMetaData namedWindowDetail = service.resolve(proposedWindow);
            if (namedWindowDetail == null) {
                continue;
            }

            // build spec for subselect
            StatementSpecRaw raw = new StatementSpecRaw(SelectClauseStreamSelectorEnum.ISTREAM_ONLY);
            FilterSpecRaw filter = new FilterSpecRaw(proposedWindow, Collections.<ExprNode>emptyList(), null);
            raw.getStreamSpecs().add(new FilterStreamSpecRaw(filter, ViewSpec.EMPTY_VIEWSPEC_ARRAY, proposedWindow, StreamSpecOptions.DEFAULT));

            ExprChainedSpec firstChain = dotNode.getChainSpec().remove(0);
            if (!firstChain.getParameters().isEmpty()) {
                if (firstChain.getParameters().size() == 1) {
                    raw.setWhereClause(firstChain.getParameters().get(0));
                } else {
                    ExprAndNode andNode = new ExprAndNodeImpl();
                    for (ExprNode node : firstChain.getParameters()) {
                        andNode.addChildNode(node);
                    }
                    raw.setWhereClause(andNode);
                }
            }

            // activate subselect
            ExprSubselectNode subselect = new ExprSubselectRowNode(raw);
            subselects.add(subselect);
            dotNode.setChildNodes(subselect);
        }
    }

    private static List<ExprTableAccessNode> determineTableAccessNodes(Set<ExprTableAccessNode> statementDirectTableAccess, ExprNodeSubselectDeclaredDotVisitor visitor) {
        Set<ExprTableAccessNode> tableAccessNodes = new HashSet<ExprTableAccessNode>();
        if (statementDirectTableAccess != null) {
            tableAccessNodes.addAll(statementDirectTableAccess);
        }
        // include all declared expression usages
        ExprNodeTableAccessVisitor tableAccessVisitor = new ExprNodeTableAccessVisitor(tableAccessNodes);
        for (ExprDeclaredNode declared : visitor.getDeclaredExpressions()) {
            declared.getBody().accept(tableAccessVisitor);
        }
        // include all subqueries (and their declared expressions)
        // This is nested as declared expressions can have more subqueries, however all subqueries are in this list.
        for (ExprSubselectNode subselectNode : visitor.getSubselects()) {
            tableAccessNodes.addAll(subselectNode.getStatementSpecRaw().getTableExpressions());
        }
        return new ArrayList<>(tableAccessNodes);
    }
}
