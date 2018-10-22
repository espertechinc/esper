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
package com.espertech.esper.compiler.client;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.module.Module;
import com.espertech.esper.common.client.module.ParseException;
import com.espertech.esper.common.client.soda.EPStatementObjectModel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * The EPL compiler compiles EPL text as well as object models to byte code.
 */
public interface EPCompiler {
    /**
     * Compiles EPL and returns the byte code for deployment into a runtime.
     * <p>
     * Use semicolon(;) to separate multiple statements in a module.
     * </p>
     *
     * @param epl       epl to compile
     * @param arguments compiler arguments
     * @return byte code
     * @throws EPCompileException when the compilation failed
     */
    EPCompiled compile(String epl, CompilerArguments arguments) throws EPCompileException;

    /**
     * Compiles a module object model and returns the byte code for deployment into a runtime.
     *
     * @param module    module object to compile
     * @param arguments compiler arguments
     * @return byte code
     * @throws EPCompileException when the compilation failed
     */
    EPCompiled compile(Module module, CompilerArguments arguments) throws EPCompileException;

    /**
     * Compiles a single fire-and-forget query for execution by the runtime.
     *
     * @param fireAndForgetEPLQuery fire-and-forget query to compile
     * @param arguments             compiler arguments
     * @return byte code
     * @throws EPCompileException when the compilation failed
     */
    EPCompiled compileQuery(String fireAndForgetEPLQuery, CompilerArguments arguments) throws EPCompileException;

    /**
     * Compiles fire-and-forget query object model for execution by the runtime.
     *
     * @param fireAndForgetEPLQueryModel fire-and-forget query to compile
     * @param arguments                  compiler arguments
     * @return byte code
     * @throws EPCompileException when the compilation failed
     */
    EPCompiled compileQuery(EPStatementObjectModel fireAndForgetEPLQueryModel, CompilerArguments arguments) throws EPCompileException;

    /**
     * Parse the module text returning the module object model.
     *
     * @param eplModuleText to parse
     * @return module object model
     * @throws IOException    when the parser failed
     * @throws ParseException when parsing of the module failed
     */
    Module parseModule(String eplModuleText) throws IOException, ParseException;

    /**
     * Parse the single-statement EPL and return a statement object model.
     *
     * @param epl           to parse
     * @param configuration a configuration object when available
     * @return statement object model
     * @throws EPCompileException when the EPL could not be parsed
     */
    EPStatementObjectModel eplToModel(String epl, Configuration configuration) throws EPCompileException;

    /**
     * Validate the syntax of the module.
     *
     * @param module    to validate
     * @param arguments compiler arguments
     * @throws EPCompileException when the EPL could not be parsed
     */
    void syntaxValidate(Module module, CompilerArguments arguments) throws EPCompileException;

    /**
     * Read the input stream and return the module. It is up to the calling method to close the stream when done.
     *
     * @param stream    to read
     * @param moduleUri uri of the module
     * @return module module
     * @throws IOException    when the io operation failed
     * @throws ParseException when parsing of the module failed
     */
    Module readModule(InputStream stream, String moduleUri) throws IOException, ParseException;

    /**
     * Read the resource by opening from classpath and return the module.
     *
     * @param resource    name of the classpath resource
     * @param classLoader classloader
     * @return module module
     * @throws IOException    when the resource could not be read
     * @throws ParseException when parsing of the module failed
     */
    Module readModule(String resource, ClassLoader classLoader) throws IOException, ParseException;

    /**
     * Read the module by reading the text file and return the module.
     *
     * @param file the file to read
     * @return module
     * @throws IOException    when the file could not be read
     * @throws ParseException when parsing of the module failed
     */
    Module readModule(File file) throws IOException, ParseException;

    /**
     * Read the module by reading from the URL provided and return the module.
     *
     * @param url the URL to read
     * @return module
     * @throws IOException    when the url input stream could not be read
     * @throws ParseException when parsing of the module failed
     */
    Module readModule(URL url) throws IOException, ParseException;
}
