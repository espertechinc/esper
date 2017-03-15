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
package com.espertech.esperio.csv;

import com.espertech.esper.client.EPException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * An input source for adapters.
 */
public class AdapterInputSource {
    private final URL url;
    private final String classpathResource;
    private final File file;
    private final InputStream inputStream;
    private final Reader reader;

    private ZipFile zipFile;

    /**
     * Ctor.
     *
     * @param classpathResource - the name of the resource on the classpath to use as the source for an adapter
     */
    public AdapterInputSource(String classpathResource) {
        if (classpathResource == null) {
            throw new NullPointerException("Cannot create AdapterInputStream from a null classpathResource");
        }
        this.classpathResource = classpathResource;
        this.url = null;
        this.file = null;
        this.inputStream = null;
        this.reader = null;
    }

    /**
     * Ctor.
     *
     * @param url - the URL for the resource to use as source for an adapter
     */
    public AdapterInputSource(URL url) {
        if (url == null) {
            throw new NullPointerException("Cannot create AdapterInputStream from a null URL");
        }
        this.url = url;
        this.classpathResource = null;
        this.file = null;
        this.inputStream = null;
        this.reader = null;
    }

    /**
     * Ctor.
     *
     * @param file - the file to use as a source
     */
    public AdapterInputSource(File file) {
        if (file == null) {
            throw new NullPointerException("file cannot be null");
        }
        this.file = file;
        this.url = null;
        this.classpathResource = null;
        this.inputStream = null;
        this.reader = null;
    }

    /**
     * Ctor.
     *
     * @param inputStream - the stream to use as a source
     */
    public AdapterInputSource(InputStream inputStream) {
        if (inputStream == null) {
            throw new NullPointerException("stream cannot be null");
        }
        this.inputStream = inputStream;
        this.file = null;
        this.url = null;
        this.classpathResource = null;
        this.reader = null;
    }

    /**
     * Ctor.
     *
     * @param reader is any reader for reading a file or string
     */
    public AdapterInputSource(Reader reader) {
        if (reader == null) {
            throw new NullPointerException("reader cannot be null");
        }
        this.reader = reader;
        this.url = null;
        this.classpathResource = null;
        this.file = null;
        this.inputStream = null;
    }

    /**
     * Get the resource as an input stream. If this resource was specified as an InputStream,
     * return that InputStream, otherwise, create and return a new InputStream from the
     * resource. If the source cannot be converted to a stream, return null.
     *
     * @return a stream from the resource
     */
    public InputStream getAsStream() {
        if (reader != null) {
            return null;
        }
        if (inputStream != null) {
            return inputStream;
        }
        if (file != null) {
            if (file.getName().endsWith("zip")) {
                return openZipFile(file);
            }
            try {
                return file.toURL().openStream();
            } catch (IOException e) {
                throw new EPException(e);
            }
        }
        if (url != null) {
            if (url.toString().endsWith("zip")) {
                return openZipUrl(url);
            }
            try {
                return url.openStream();
            } catch (IOException e) {
                throw new EPException(e);
            }
        } else {
            return resolvePathAsStream(classpathResource);
        }
    }

    /**
     * Return the reader if it was set, null otherwise.
     *
     * @return the Reader
     */
    public Reader getAsReader() {
        return reader;
    }

    /**
     * Return true if calling getStream() will return a new InputStream created from the
     * resource, which, assuming that the resource hasn't been changed, will have the same
     * information as all the previous InputStreams returned by getStream() before they were
     * manipulated; return false if the call will return the same instance of InputStream that
     * has already been obtained.
     *
     * @return true if each call to getStream() will create a new InputStream from the
     * resource, false if each call will get the same instance of the InputStream
     */
    public boolean isResettable() {
        return inputStream == null && reader == null;
    }

    private InputStream resolvePathAsStream(String path) {
        if (path.endsWith(".zip")) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL url = null;
            if (classLoader != null) {
                url = classLoader.getResource(path);
            }
            if (url == null) {
                url = CSVReader.class.getResource(path);
            }
            if (url == null) {
                url = CSVReader.class.getClassLoader().getResource(path);
            }
            if (url == null) {
                throw new EPException("Resource '" + path + "' not found in classpath");
            }

            return openZipUrl(url);
        }

        InputStream stream = null;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            stream = classLoader.getResourceAsStream(path);
        }
        if (stream == null) {
            stream = CSVReader.class.getResourceAsStream(path);
        }
        if (stream == null) {
            stream = CSVReader.class.getClassLoader().getResourceAsStream(path);
        }
        if (stream == null) {
            throw new EPException("Resource '" + path + "' not found in classpath");
        }

        return stream;
    }

    private InputStream openZipUrl(URL url) {
        File file;
        try {
            file = new File(url.toURI());
        } catch (URISyntaxException e) {
            throw new EPException("Resource '" + url + "' did not return a file uri: " + e.getMessage(), e);
        }
        return openZipFile(file);
    }

    private InputStream openZipFile(File file) {
        try {
            zipFile = new ZipFile(file);
        } catch (IOException e) {
            throw new EPException("Resource '" + file + "' could not be opened as a valid zip file: " + e.getMessage(), e);
        }

        Enumeration<? extends ZipEntry> entry = zipFile.entries();
        ZipEntry zipEntry;
        try {
            zipEntry = entry.nextElement();
        } catch (NoSuchElementException ex) {
            closeZip();
            throw new EPException("Zip archive '" + file + "' is empty");
        }

        try {
            return zipFile.getInputStream(zipEntry);
        } catch (IOException ex) {
            throw new EPException("Zip archive '" + file + "' entry '" + zipEntry.getName() + " could not be opened for reading");
        }
    }

    public void close() {
        closeZip();
    }

    private synchronized void closeZip() {
        if (zipFile != null) {
            try {
                zipFile.close();
            } catch (IOException e) {
            }
            zipFile = null;
        }
    }
}
