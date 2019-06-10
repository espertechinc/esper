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
package com.espertech.esper.common.client.configuration;

import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.configuration.common.ConfigurationCommon;
import com.espertech.esper.common.client.configuration.compiler.ConfigurationCompiler;
import com.espertech.esper.common.client.configuration.runtime.ConfigurationRuntime;
import com.espertech.esper.common.internal.util.ConfigurationParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.io.*;
import java.net.URL;

/**
 * An instance of <tt>Configuration</tt> allows the application
 * to specify properties to be used when compiling and when getting a runtime.
 * The <tt>Configuration</tt> is meant
 * only as an initialization-time object.
 * <br>
 * The format of an Esper XML configuration file is defined in
 * <tt>esper-configuration-(version).xsd</tt>.
 */
public class Configuration implements Serializable {
    private final static Logger log = LoggerFactory.getLogger(Configuration.class);
    private static final long serialVersionUID = -2806573144337726660L;

    private ConfigurationCommon common;
    private ConfigurationCompiler compiler;
    private ConfigurationRuntime runtime;

    /**
     * Default name of the configuration file.
     */
    protected static final String ESPER_DEFAULT_CONFIG = "esper.cfg.xml";

    /**
     * Constructs an empty configuration. The auto import values
     * are set by default to java.lang, java.math, java.text and
     * java.util.
     */
    public Configuration() {
        reset();
    }

    /**
     * Use the configuration specified in an application
     * resource named <tt>esper.cfg.xml</tt>.
     *
     * @return Configuration initialized from the resource
     * @throws EPException thrown to indicate error reading configuration
     */
    public Configuration configure() throws EPException {
        configure('/' + ESPER_DEFAULT_CONFIG);
        return this;
    }

    /**
     * Use the configuration specified in the given application
     * resource. The format of the resource is defined in
     * <tt>esper-configuration-(version).xsd</tt>.
     * <p>
     * The resource is found via <tt>getConfigurationInputStream(resource)</tt>.
     * That method can be overridden to implement an arbitrary lookup strategy.
     * </p>
     * <p>
     * See <tt>getResourceAsStream</tt> for information on how the resource name is resolved.
     * </p>
     *
     * @param resource if the file name of the resource
     * @return Configuration initialized from the resource
     * @throws EPException thrown to indicate error reading configuration
     */
    public Configuration configure(String resource) throws EPException {
        if (log.isDebugEnabled()) {
            log.debug("Configuring from resource: " + resource);
        }
        InputStream stream = getConfigurationInputStream(resource);
        ConfigurationParser.doConfigure(this, stream, resource);
        return this;
    }

    /**
     * Get the configuration file as an <tt>InputStream</tt>. Might be overridden
     * by subclasses to allow the configuration to be located by some arbitrary
     * mechanism.
     * <p>
     * See <tt>getResourceAsStream</tt> for information on how the resource name is resolved.
     *
     * @param resource is the resource name
     * @return input stream for resource
     * @throws EPException thrown to indicate error reading configuration
     */
    protected static InputStream getConfigurationInputStream(String resource) throws EPException {
        return getResourceAsStream(resource);
    }


    /**
     * Use the configuration specified by the given URL.
     * The format of the document obtained from the URL is defined in
     * <tt>esper-configuration-(version).xsd</tt>.
     *
     * @param url URL from which you wish to load the configuration
     * @return A configuration configured via the file
     * @throws EPException is thrown when the URL could not be access
     */
    public Configuration configure(URL url) throws EPException {
        if (log.isDebugEnabled()) {
            log.debug("configuring from url: " + url.toString());
        }
        try {
            ConfigurationParser.doConfigure(this, url.openStream(), url.toString());
            return this;
        } catch (IOException ioe) {
            throw new EPException("could not configure from URL: " + url, ioe);
        }
    }

    /**
     * Use the configuration specified in the given application
     * file. The format of the file is defined in
     * <tt>esper-configuration-(version).xsd</tt>.
     *
     * @param configFile <tt>File</tt> from which you wish to load the configuration
     * @return A configuration configured via the file
     * @throws EPException when the file could not be found
     */
    public Configuration configure(File configFile) throws EPException {
        if (log.isDebugEnabled()) {
            log.debug("configuring from file: " + configFile.getName());
        }

        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(configFile);
            ConfigurationParser.doConfigure(this, inputStream, configFile.toString());
        } catch (FileNotFoundException fnfe) {
            throw new EPException("could not find file: " + configFile, fnfe);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.debug("Error closing input stream", e);
                }
            }
        }
        return this;
    }

    /**
     * Use the mappings and properties specified in the given XML document.
     * The format of the file is defined in
     * <tt>esper-configuration-(version).xsd</tt>.
     *
     * @param document an XML document from which you wish to load the configuration
     * @return A configuration configured via the <tt>Document</tt>
     * @throws EPException if there is problem in accessing the document.
     */
    public Configuration configure(Document document) throws EPException {
        if (log.isDebugEnabled()) {
            log.debug("configuring from XML document");
        }
        ConfigurationParser.doConfigure(this, document);
        return this;
    }

    /**
     * Returns an input stream from an application resource in the classpath.
     * <p>
     * The method first removes the '/' character from the resource name if
     * the first character is '/'.
     * <p>
     * The lookup order is as follows:
     * <p>
     * If a thread context class loader exists, use <tt>Thread.currentThread().getResourceAsStream</tt>
     * to obtain an InputStream.
     * <p>
     * If no input stream was returned, use the <tt>Configuration.class.getResourceAsStream</tt>.
     * to obtain an InputStream.
     * <p>
     * If no input stream was returned, use the <tt>Configuration.class.getClassLoader().getResourceAsStream</tt>.
     * to obtain an InputStream.
     * <p>
     * If no input stream was returned, throw an Exception.
     *
     * @param resource to get input stream for
     * @return input stream for resource
     */
    protected static InputStream getResourceAsStream(String resource) {
        String stripped = resource.startsWith("/") ?
                resource.substring(1) : resource;

        InputStream stream = null;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            stream = classLoader.getResourceAsStream(stripped);
        }
        if (stream == null) {
            stream = Configuration.class.getResourceAsStream(resource);
        }
        if (stream == null) {
            stream = Configuration.class.getClassLoader().getResourceAsStream(stripped);
        }
        if (stream == null) {
            throw new EPException(resource + " not found");
        }
        return stream;
    }

    /**
     * Returns the common section of the configuration.
     * <p>
     * The common section is for use by both the compiler and the runtime.
     * </p>
     *
     * @return common configuration
     */
    public ConfigurationCommon getCommon() {
        return common;
    }

    /**
     * Sets the common section of the configuration.
     * <p>
     * The common section is for use by both the compiler and the runtime.
     * </p>
     *
     * @param common common configuration
     */
    public void setCommon(ConfigurationCommon common) {
        this.common = common;
    }

    /**
     * Returns the compiler section of the configuration.
     * <p>
     * The compiler section is for use by the compiler. The runtime ignores this part of the configuration object.
     * </p>
     *
     * @return compiler configuration
     */
    public ConfigurationCompiler getCompiler() {
        return compiler;
    }

    /**
     * Sets the compiler section of the configuration.
     * <p>
     * The compiler section is for use by the compiler. The runtime ignores this part of the configuration object.
     * </p>
     *
     * @param compiler compiler configuration
     */
    public void setCompiler(ConfigurationCompiler compiler) {
        this.compiler = compiler;
    }

    /**
     * Returns the runtime section of the configuration.
     * <p>
     * The runtime section is for use by the runtime. The compiler ignores this part of the configuration object.
     * </p>
     *
     * @return runtime configuration
     */
    public ConfigurationRuntime getRuntime() {
        return runtime;
    }

    /**
     * Sets the runtime section of the configuration.
     * <p>
     * The runtime section is for use by the runtime. The compiler ignores this part of the configuration object.
     * </p>
     *
     * @param runtime runtime configuration
     */
    public void setRuntime(ConfigurationRuntime runtime) {
        this.runtime = runtime;
    }

    /**
     * Reset to an empty configuration.
     */
    protected void reset() {
        common = new ConfigurationCommon();
        compiler = new ConfigurationCompiler();
        runtime = new ConfigurationRuntime();
    }
}
