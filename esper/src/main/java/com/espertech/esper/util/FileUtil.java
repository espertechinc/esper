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
package com.espertech.esper.util;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class FileUtil {

    public static String findClasspathFile(String filename) {
        URL url = FileUtil.class.getClassLoader().getResource(filename);
        if (url != null) {
            return url.getFile();
        }
        return null;
    }

    public static void findDeleteClasspathFile(String filename) {
        URL url = FileUtil.class.getClassLoader().getResource(filename);
        if (url != null) {
            File file = new File(url.getFile());
            file.delete();
        }
    }

    public static String[] readClasspathTextFile(String filename) {
        String filenameCp = findClasspathFile(filename);
        if (filenameCp == null) {
            throw new RuntimeException("Failed to find file '" + filename + "' in classpath");
        }
        List<String> lines = new ArrayList<String>();
        try {
            FileInputStream fis = new FileInputStream(filenameCp);
            Scanner scanner = new Scanner(fis);
            try {
                while (scanner.hasNextLine()) {
                    lines.add(scanner.nextLine());
                }
            } finally {
                scanner.close();
                fis.close();
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read file '" + filename + "': " + ex.getMessage(), ex);
        }
        return lines.toArray(new String[lines.size()]);
    }

    public static String readTextFile(File file) throws IOException {
        String newline = System.getProperty("line.separator");
        FileReader fr = new FileReader(file);
        try {
            BufferedReader br = new BufferedReader(fr);
            StringWriter buffer = new StringWriter();
            String strLine;
            while ((strLine = br.readLine()) != null) {
                buffer.append(strLine);
                buffer.append(newline);
            }
            return buffer.toString();
        } finally {
            try {
                fr.close();
            } catch (IOException e) {
            }
        }
    }

    public static List<String> readFile(InputStream is) {
        InputStreamReader isr = new InputStreamReader(is);
        try {
            return readFile(isr);
        } finally {
            try {
                isr.close();
            } catch (IOException e) {
                // fine
            }
        }
    }

    public static List<String> readFile(Reader reader) {
        List<String> list = new ArrayList<String>();
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(reader);
            readFile(bufferedReader, list);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("File not found: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("IO Error reading file: " + e.getMessage(), e);
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            } catch (IOException e) {
            }
        }

        return list;
    }

    public static String linesToText(List<String> lines) {
        StringWriter writer = new StringWriter();
        for (String line : lines) {
            writer.append(line)
                    .append(System.getProperty(
                            "line.separator"));
        }
        return writer.toString();
    }

    private static void readFile(BufferedReader reader, List<String> list) throws IOException {
        String text;
        // repeat until all lines is read
        while ((text = reader.readLine()) != null) {
            list.add(text);
        }
    }
}
