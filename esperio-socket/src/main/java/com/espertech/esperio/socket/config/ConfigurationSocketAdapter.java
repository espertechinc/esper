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
package com.espertech.esperio.socket.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ConfigurationSocketAdapter {
    private final static Logger log = LoggerFactory.getLogger(ConfigurationSocketAdapter.class);

    private Map<String, SocketConfig> sockets;

    public ConfigurationSocketAdapter() {
        sockets = new HashMap<String, SocketConfig>();
    }

    public Map<String, SocketConfig> getSockets() {
        return sockets;
    }

    public void setSockets(Map<String, SocketConfig> sockets) {
        this.sockets = sockets;
    }

    /**
     * Use the configuration specified in an application
     * resource named <tt>esper.cfg.xml</tt>.
     *
     * @return Configuration initialized from the resource
     * @throws RuntimeException thrown to indicate error reading configuration
     */
    public ConfigurationSocketAdapter configure() throws RuntimeException {
        configure('/' + "esperio.socket.cfg.xml");
        return this;
    }

    /**
     * Use the ConfigurationSocketAdapter specified in the given application
     * resource. The format of the resource is defined in
     * <tt>esper-configuration-(version).xsd</tt>.
     * <p>
     * The resource is found via <tt>getConfigurationInputStream(resource)</tt>.
     * That method can be overridden to implement an arbitrary lookup strategy.
     * </p>
     * See <tt>getResourceAsStream</tt> for information on how the resource name is resolved.
     *
     * @param resource if the file name of the resource
     * @return ConfigurationSocketAdapter initialized from the resource
     * @throws RuntimeException thrown to indicate error reading configuration
     */
    public ConfigurationSocketAdapter configure(String resource) throws RuntimeException {
        if (log.isInfoEnabled()) {
            log.info("Configuring from resource: " + resource);
        }
        InputStream stream = getConfigurationInputStream(resource);
        ConfigurationSocketAdapterParser.doConfigure(this, stream, resource);
        return this;
    }

    /**
     * Get the ConfigurationSocketAdapter file as an <tt>InputStream</tt>. Might be overridden
     * by subclasses to allow the ConfigurationSocketAdapter to be located by some arbitrary
     * mechanism.
     * <p>
     * See <tt>getResourceAsStream</tt> for information on how the resource name is resolved.
     *
     * @param resource is the resource name
     * @return input stream for resource
     * @throws RuntimeException thrown to indicate error reading configuration
     */
    protected static InputStream getConfigurationInputStream(String resource) throws RuntimeException {
        return getResourceAsStream(resource);
    }

    /**
     * Use the ConfigurationSocketAdapter specified by the given XML String.
     * The format of the document obtained from the URL is defined in
     * <tt>esper-configuration-(version).xsd</tt>.
     *
     * @param xml XML string
     * @return A ConfigurationSocketAdapter configured via the file
     * @throws RuntimeException is thrown when the URL could not be access
     */
    public ConfigurationSocketAdapter configureFromString(String xml) throws RuntimeException {
        if (log.isInfoEnabled()) {
            log.info("Configuring from string");
        }
        try {
            InputSource source = new InputSource(new StringReader(xml));
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setNamespaceAware(true);
            Document doc = builderFactory.newDocumentBuilder().parse(source);

            ConfigurationSocketAdapterParser.doConfigure(this, doc);
            return this;
        } catch (IOException ioe) {
            throw new RuntimeException("could not configure from String: " + ioe.getMessage(), ioe);
        } catch (SAXException e) {
            throw new RuntimeException("could not configure from String: " + e.getMessage(), e);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("could not configure from String: " + e.getMessage(), e);
        }
    }

    /**
     * Use the ConfigurationSocketAdapter specified by the given URL.
     * The format of the document obtained from the URL is defined in
     * <tt>esper-configuration-(version).xsd</tt>.
     *
     * @param url URL from which you wish to load the configuration
     * @return A ConfigurationSocketAdapter configured via the file
     * @throws RuntimeException is thrown when the URL could not be access
     */
    public ConfigurationSocketAdapter configure(URL url) throws RuntimeException {
        if (log.isInfoEnabled()) {
            log.info("Configuring from url: " + url.toString());
        }
        try {
            ConfigurationSocketAdapterParser.doConfigure(this, url.openStream(), url.toString());
            return this;
        } catch (IOException ioe) {
            throw new RuntimeException("could not configure from URL: " + url, ioe);
        }
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
            stream = ConfigurationSocketAdapter.class.getResourceAsStream(resource);
        }
        if (stream == null) {
            stream = ConfigurationSocketAdapter.class.getClassLoader().getResourceAsStream(stripped);
        }
        if (stream == null) {
            throw new RuntimeException(resource + " not found");
        }
        return stream;
    }

    /**
     * Use the ConfigurationSocketAdapter specified in the given application
     * file. The format of the file is defined in
     * <tt>esper-configuration-(version).xsd</tt>.
     *
     * @param configFile <tt>File</tt> from which you wish to load the configuration
     * @return A ConfigurationSocketAdapter configured via the file
     * @throws RuntimeException when the file could not be found
     */
    public ConfigurationSocketAdapter configure(File configFile) throws RuntimeException {
        if (log.isDebugEnabled()) {
            log.debug("configuring from file: " + configFile.getName());
        }
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(configFile);
            ConfigurationSocketAdapterParser.doConfigure(this, inputStream, configFile.toString());
        } catch (FileNotFoundException fnfe) {
            throw new RuntimeException("could not find file: " + configFile, fnfe);
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

}
