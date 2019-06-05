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

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.module.Module;
import com.espertech.esper.common.client.module.ModuleItem;
import com.espertech.esper.common.client.module.ModuleProperty;
import com.espertech.esper.common.client.module.ParseException;
import com.espertech.esper.common.client.soda.EPStatementObjectModel;
import com.espertech.esper.common.internal.compile.stage1.Compilable;
import com.espertech.esper.common.internal.compile.stage1.spec.StatementSpecRaw;
import com.espertech.esper.common.internal.compile.stage1.specmapper.StatementSpecMapEnv;
import com.espertech.esper.common.internal.compile.stage1.specmapper.StatementSpecMapper;
import com.espertech.esper.common.internal.compile.stage1.specmapper.StatementSpecUnMapResult;
import com.espertech.esper.common.internal.compile.stage2.StatementSpecCompileException;
import com.espertech.esper.common.internal.compile.stage3.ModuleCompileTimeServices;
import com.espertech.esper.common.internal.compile.stage3.StatementCompileTimeServices;
import com.espertech.esper.common.internal.context.compile.ContextCompileTimeResolverEmpty;
import com.espertech.esper.common.internal.epl.expression.declared.compiletime.ExprDeclaredCompileTimeResolverEmpty;
import com.espertech.esper.common.internal.epl.script.compiletime.ScriptCompileTimeResolverEmpty;
import com.espertech.esper.common.internal.epl.table.compiletime.TableCompileTimeResolverEmpty;
import com.espertech.esper.common.internal.epl.variable.compiletime.VariableCompileTimeResolverEmpty;
import com.espertech.esper.common.internal.settings.ClasspathImportException;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.CompilerOptions;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.option.ModuleNameContext;
import com.espertech.esper.compiler.client.option.ModuleUsesContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

import static com.espertech.esper.compiler.internal.util.CompilerHelperServices.getCompileTimeServices;
import static com.espertech.esper.compiler.internal.util.CompilerHelperServices.makeClasspathImportService;
import static com.espertech.esper.compiler.internal.util.CompilerHelperSingleEPL.parseWalk;

public class EPCompilerImpl implements EPCompilerSPI {

    private final static Logger log = LoggerFactory.getLogger(EPCompilerImpl.class);

    public EPCompiled compileQuery(String fireAndForgetEPLQuery, CompilerArguments arguments) throws EPCompileException {
        return compileQueryInternal(new CompilableEPL(fireAndForgetEPLQuery, 1), arguments);
    }

    public EPCompiled compileQuery(EPStatementObjectModel fireAndForgetEPLQueryModel, CompilerArguments arguments) throws EPCompileException {
        return compileQueryInternal(new CompilableSODA(fireAndForgetEPLQueryModel, 1), arguments);
    }

    public EPCompiled compile(String epl, CompilerArguments arguments) throws EPCompileException {
        if (arguments == null) {
            arguments = new CompilerArguments(new Configuration());
        }

        try {
            Module module = EPLModuleUtil.parseInternal(epl, null);
            List<Compilable> compilables = new ArrayList<>();
            for (ModuleItem item : module.getItems()) {
                if (item.isCommentOnly()) {
                    continue;
                }
                String stmtEpl = item.getExpression();
                compilables.add(new CompilableEPL(stmtEpl, item.getLineNumber()));
            }

            // determine module name
            String moduleName = determineModuleName(arguments.getOptions(), module);
            Set<String> moduleUses = determineModuleUses(moduleName, arguments.getOptions(), module);

            // get compile services
            ModuleCompileTimeServices compileTimeServices = getCompileTimeServices(arguments, moduleName, moduleUses, false);
            addModuleImports(module.getImports(), compileTimeServices);

            // compile
            return CompilerHelperModuleProvider.compile(compilables, moduleName, Collections.emptyMap(), compileTimeServices, arguments.getOptions());
        } catch (EPCompileException ex) {
            throw ex;
        } catch (ParseException t) {
            throw new EPCompileException("Failed to parse: " + t.getMessage(), t, Collections.emptyList());
        } catch (Throwable t) {
            throw new EPCompileException(t.getMessage(), t, Collections.emptyList());
        }
    }

    public EPCompilerSPIExpression expressionCompiler(Configuration configuration) throws EPCompileException {
        CompilerArguments arguments = new CompilerArguments(configuration);
        arguments.setConfiguration(configuration);
        ModuleCompileTimeServices compileTimeServices = getCompileTimeServices(arguments, null, null, false);
        return new EPCompilerSPIExpressionImpl(compileTimeServices);
    }

    public EPStatementObjectModel eplToModel(String stmtText, Configuration configuration) throws EPCompileException {
        try {
            StatementSpecMapEnv mapEnv = new StatementSpecMapEnv(makeClasspathImportService(configuration), VariableCompileTimeResolverEmpty.INSTANCE, configuration, ExprDeclaredCompileTimeResolverEmpty.INSTANCE, ContextCompileTimeResolverEmpty.INSTANCE, TableCompileTimeResolverEmpty.INSTANCE, ScriptCompileTimeResolverEmpty.INSTANCE, new CompilerServicesImpl());
            StatementSpecRaw statementSpec = CompilerHelperSingleEPL.parseWalk(stmtText, mapEnv);
            StatementSpecUnMapResult unmapped = StatementSpecMapper.unmap(statementSpec);
            return unmapped.getObjectModel();
        } catch (StatementSpecCompileException ex) {
            throw new EPCompileException(ex.getMessage(), ex, Collections.emptyList());
        } catch (Throwable t) {
            throw new EPCompileException(t.getMessage(), t, Collections.emptyList());
        }
    }

    public Module parseModule(String eplModuleText) throws IOException, ParseException {
        return EPLModuleUtil.parseInternal(eplModuleText, null);
    }

    public EPCompiled compile(Module module, CompilerArguments arguments) throws EPCompileException {
        if (arguments == null) {
            arguments = new CompilerArguments(new Configuration());
        }

        // determine module name
        String moduleName = determineModuleName(arguments.getOptions(), module);
        Set<String> moduleUses = determineModuleUses(moduleName, arguments.getOptions(), module);

        // get compile services
        ModuleCompileTimeServices compileTimeServices = getCompileTimeServices(arguments, moduleName, moduleUses, false);
        addModuleImports(module.getImports(), compileTimeServices);

        List<Compilable> compilables = new ArrayList<>();
        for (ModuleItem item : module.getItems()) {
            if (item.isCommentOnly()) {
                continue;
            }
            if (item.getExpression() != null && item.getModel() != null) {
                throw new EPCompileException("Module item has both an EPL expression and a statement object model");
            }
            if (item.getExpression() != null) {
                compilables.add(new CompilableEPL(item.getExpression(), item.getLineNumber()));
            } else if (item.getModel() != null) {
                compilables.add(new CompilableSODA(item.getModel(), item.getLineNumber()));
            } else {
                throw new EPCompileException("Module item has neither an EPL expression nor a statement object model");
            }
        }

        Map<ModuleProperty, Object> moduleProperties = new HashMap<>();
        addModuleProperty(moduleProperties, ModuleProperty.ARCHIVENAME, module.getArchiveName());
        addModuleProperty(moduleProperties, ModuleProperty.URI, module.getUri());
        if (arguments.getConfiguration().getCompiler().getByteCode().isAttachModuleEPL()) {
            addModuleProperty(moduleProperties, ModuleProperty.MODULETEXT, module.getModuleText());
        }
        addModuleProperty(moduleProperties, ModuleProperty.USEROBJECT, module.getModuleUserObjectCompileTime());
        addModuleProperty(moduleProperties, ModuleProperty.USES, toNullOrArray(module.getUses()));
        addModuleProperty(moduleProperties, ModuleProperty.IMPORTS, toNullOrArray(module.getImports()));

        // compile
        return CompilerHelperModuleProvider.compile(compilables, moduleName, moduleProperties, compileTimeServices, arguments.getOptions());
    }

    public Module readModule(InputStream stream, String uri) throws IOException, ParseException {
        if (log.isDebugEnabled()) {
            log.debug("Reading module from input stream");
        }
        return EPLModuleUtil.readInternal(stream, uri);
    }

    public Module readModule(File file) throws IOException, ParseException {
        if (log.isDebugEnabled()) {
            log.debug("Reading resource '" + file.getAbsolutePath() + "'");
        }
        return EPLModuleUtil.readFile(file);
    }

    public Module readModule(URL url) throws IOException, ParseException {
        if (log.isDebugEnabled()) {
            log.debug("Reading resource from url: " + url.toString());
        }
        return EPLModuleUtil.readInternal(url.openStream(), url.toString());
    }

    public Module readModule(String resource, ClassLoader classLoader) throws IOException, ParseException {
        if (log.isDebugEnabled()) {
            log.debug("Reading resource '" + resource + "'");
        }
        return EPLModuleUtil.readResource(resource, classLoader);
    }

    public void syntaxValidate(Module module, CompilerArguments arguments) throws EPCompileException {
        if (arguments == null) {
            arguments = new CompilerArguments(new Configuration());
        }

        // determine module name
        String moduleName = determineModuleName(arguments.getOptions(), module);
        Set<String> moduleUses = determineModuleUses(moduleName, arguments.getOptions(), module);

        ModuleCompileTimeServices moduleCompileTimeServices = getCompileTimeServices(arguments, moduleName, moduleUses, false);

        int statementNumber = 0;
        try {
            for (ModuleItem item : module.getItems()) {
                StatementCompileTimeServices services = new StatementCompileTimeServices(statementNumber, moduleCompileTimeServices);
                if (item.isCommentOnly()) {
                    continue;
                }
                if (item.getExpression() != null && item.getModel() != null) {
                    throw new EPCompileException("Module item has both an EPL expression and a statement object model");
                }
                if (item.getExpression() != null) {
                    parseWalk(new CompilableEPL(item.getExpression(), item.getLineNumber()), services);
                } else if (item.getModel() != null) {
                    parseWalk(new CompilableSODA(item.getModel(), item.getLineNumber()), services);
                    item.getModel().toEPL();
                } else {
                    throw new EPCompileException("Module item has neither an EPL expression nor a statement object model");
                }
                statementNumber++;
            }
        } catch (Throwable ex) {
            throw new EPCompileException(ex.getMessage(), ex);
        }
    }

    private EPCompiled compileQueryInternal(Compilable compilable, CompilerArguments arguments) throws EPCompileException {
        if (arguments == null) {
            arguments = new CompilerArguments(new Configuration());
        }

        // determine module name
        String moduleName = arguments.getOptions().getModuleName() == null ? null : arguments.getOptions().getModuleName().getValue(new ModuleNameContext(null));
        Set<String> moduleUses = arguments.getOptions().getModuleUses() == null ? null : arguments.getOptions().getModuleUses().getValue(new ModuleUsesContext(moduleName, null));

        ModuleCompileTimeServices compileTimeServices = getCompileTimeServices(arguments, moduleName, moduleUses, true);
        try {
            return CompilerHelperFAFProvider.compile(compilable, compileTimeServices, arguments);
        } catch (Throwable t) {
            throw new EPCompileException(t.getMessage() + " [" + compilable.toEPL() + "]", t, Collections.emptyList());
        }
    }

    private void addModuleProperty(Map<ModuleProperty, Object> moduleProperties, ModuleProperty key, Object value) {
        if (value == null) {
            return;
        }
        moduleProperties.put(key, value);
    }

    private String determineModuleName(CompilerOptions options, Module module) {
        return options.getModuleName() != null ? options.getModuleName().getValue(new ModuleNameContext(module.getName())) : module.getName();
    }

    private Set<String> determineModuleUses(String moduleName, CompilerOptions options, Module module) {
        return options.getModuleUses() != null ? options.getModuleUses().getValue(new ModuleUsesContext(moduleName, module.getUses())) : module.getUses();
    }

    private Object toNullOrArray(Set<String> values) {
        return values == null || values.isEmpty() ? null : values.toArray(new String[0]);
    }

    private void addModuleImports(Set<String> imports, ModuleCompileTimeServices compileTimeServices) throws EPCompileException {
        if (imports != null) {
            for (String imported : imports) {
                try {
                    compileTimeServices.getClasspathImportServiceCompileTime().addImport(imported);
                } catch (ClasspathImportException e) {
                    throw new EPCompileException("Invalid module import: " + e.getMessage(), e);
                }
            }
        }
    }
}
