/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.karaf.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.camel.tooling.util.FileUtil;
import org.apache.camel.tooling.util.PackageHelper;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

/**
 * Prepares the Karaf provider camel catalog to include component it supports
 */
@Mojo(name = "prepare-catalog-karaf", threadSafe = true)
public class PrepareCatalogKarafMojo extends AbstractMojo {

    /**
     * The maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    protected MavenProject project;

    /**
     * The output directory for components catalog
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/resources/org/apache/camel/catalog/karaf/components")
    protected File componentsOutDir;

    /**
     * The output directory for dataformats catalog
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/resources/org/apache/camel/catalog/karaf/dataformats")
    protected File dataFormatsOutDir;

    /**
     * The output directory for languages catalog
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/resources/org/apache/camel/catalog/karaf/languages")
    protected File languagesOutDir;

    /**
     * The output directory for others catalog
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/resources/org/apache/camel/catalog/karaf/others")
    protected File othersOutDir;

    /**
     * The karaf features directory
     */
    @Parameter(defaultValue = "${project.basedir}/../../platforms/karaf/features/src/main/resources/")
    protected File featuresDir;

    /**
     * The components directory where all the Apache Camel components are from the camel-catalog
     */
    @Parameter(defaultValue = "${project.build.directory}/sources/camel-catalog/org/apache/camel/catalog/components")
    protected File componentsDir;

    /**
     * The components directory where there are karaf only components
     */
    @Parameter(defaultValue = "${project.basedir}/../../components")
    protected File karafComponentsDir;

    /**
     * The dataformats directory where all the Apache Camel components are from the camel-catalog
     */
    @Parameter(defaultValue = "${project.build.directory}/sources/camel-catalog/org/apache/camel/catalog/dataformats")
    protected File dataformatsDir;

    /**
     * The languages directory where all the Apache Camel components are from the camel-catalog
     */
    @Parameter(defaultValue = "${project.build.directory}/sources/camel-catalog/org/apache/camel/catalog/languages")
    protected File languagesDir;

    /**
     * The languages directory where all the Apache Camel components are from the camel-catalog
     */
    @Parameter(defaultValue = "${project.build.directory}/sources/camel-catalog/org/apache/camel/catalog/others")
    protected File othersDir;

    /**
     * Maven ProjectHelper.
     */
    @Component
    private MavenProjectHelper projectHelper;

    /**
     * Execute goal.
     *
     * @throws MojoExecutionException execution of the main class or one of the
     *                                threads it generated failed.
     * @throws MojoFailureException   something bad happened...
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Set<String> features = findKarafFeatures();
        executeFeatures(features);
    }

    protected void executeFeatures(Set<String> features) throws MojoExecutionException, MojoFailureException {
        // lets use sorted set/maps
        Set<File> jsonFiles = new TreeSet<>();

        if (componentsDir != null && componentsDir.isDirectory()) {
            File[] files = componentsDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    String name = stripExt(file);
                    if (features.contains("camel-" + name) || isCoreComponent(name)) {
                        jsonFiles.add(file);
                    }
                }
            }
        }

        // include paxlogging and eventadmin as regular components
        jsonFiles.add(new File(karafComponentsDir, "camel-eventadmin/target/classes/org/apache/camel/component/eventadmin/eventadmin.json"));
        jsonFiles.add(new File(karafComponentsDir, "camel-paxlogging/target/classes/org/apache/camel/component/paxlogging/paxlogging.json"));

        if (!jsonFiles.isEmpty()) {
            Path outDir = componentsOutDir.toPath();
            copyFiles(outDir, jsonFiles);
            generateJsonList(outDir, "../components.properties");
            getLog().info("Copying " + jsonFiles.size() + " Camel component json descriptors");
        }

        jsonFiles.clear();
        if (dataformatsDir != null && dataformatsDir.isDirectory()) {
            File[] files = dataformatsDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    // all dataformats are supported on karaf
                    jsonFiles.add(file);
                }
            }
        }
        if (!jsonFiles.isEmpty()) {
            Path outDir = dataFormatsOutDir.toPath();
            copyFiles(outDir, jsonFiles);
            generateJsonList(outDir, "../dataformats.properties");
            getLog().info("Copying " + jsonFiles.size() + " Camel dataformat json descriptors");
        }

        jsonFiles.clear();
        if (languagesDir != null && languagesDir.isDirectory()) {
            File[] files = languagesDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    // all languages are supported on karaf
                    jsonFiles.add(file);
                }
            }
        }
        if (!jsonFiles.isEmpty()) {
            Path outDir = languagesOutDir.toPath();
            copyFiles(outDir, jsonFiles);
            generateJsonList(outDir, "../languages.properties");
            getLog().info("Copying " + jsonFiles.size() + " Camel language json descriptors");
        }

        jsonFiles.clear();
        if (othersDir != null && othersDir.isDirectory()) {
            File[] files = othersDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (features.contains("camel-" + stripExt(file))) {
                        jsonFiles.add(file);
                    }
                }
            }
        }
        // include others that are in camel-karaf only
        jsonFiles.add(new File(karafComponentsDir, "camel-blueprint/target/classes/blueprint.json"));
        jsonFiles.add(new File(karafComponentsDir, "camel-kura/target/classes/kura.json"));
        if (!jsonFiles.isEmpty()) {
            Path outDir = othersOutDir.toPath();
            copyFiles(outDir, jsonFiles);
            generateJsonList(outDir, "../others.properties");
            getLog().info("Copying " + jsonFiles.size() + " Camel other json descriptors");
        }
    }

    private static boolean isCoreComponent(String name) {
        return ("bean,browse,controlbus,dataformat,dataset,direct,directvm,file,language,log,mock,ref"
                + ",rest,saga,scheduler,seda,stub,timer,validator,vm,xpath,xslt").contains(name);
    }

    private static String stripExt(File file) {
        String name = file.getName();
        return name.substring(0, name.indexOf("."));
    }

    public static void copyFiles(Path outDir, Collection<File> files) throws MojoFailureException {
        for (File file : files) {
            Path to = outDir.resolve(file.getName());
            try {
                FileUtil.updateFile(file.toPath(), to);
            } catch (IOException e) {
                throw new MojoFailureException("Cannot copy file from " + file + " -> " + to, e);
            }
        }
    }

    public static Set<String> generateJsonList(Path outDir, String outFile) throws MojoFailureException {
        Path all = outDir.resolve(outFile);
        try {
            Set<String> answer = Files.list(outDir).filter(p -> p.getFileName().toString().endsWith(PackageHelper.JSON_SUFIX)).map(p -> p.getFileName().toString())
                    // strip out .json from the name
                    .map(n -> n.substring(0, n.length() - PackageHelper.JSON_SUFIX.length())).sorted().collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
            String data = String.join("\n", answer) + "\n";
            FileUtil.updateFile(all, data);
            return answer;
        } catch (IOException e) {
            throw new MojoFailureException("Error writing to file " + all);
        }
    }

    private Set<String> findKarafFeatures() throws MojoExecutionException, MojoFailureException {
        // load features.xml file and parse it

        Set<String> answer = new LinkedHashSet<>();
        File file = new File(featuresDir, "features.xml");
        try (InputStream is = new FileInputStream(file)) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setIgnoringComments(true);
            dbf.setIgnoringElementContentWhitespace(true);
            dbf.setNamespaceAware(false);
            dbf.setValidating(false);
            dbf.setXIncludeAware(false);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
            Document dom = dbf.newDocumentBuilder().parse(is);

            NodeList children = dom.getElementsByTagName("features");
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    NodeList children2 = child.getChildNodes();
                    for (int j = 0; j < children2.getLength(); j++) {
                        Node child2 = children2.item(j);
                        if ("feature".equals(child2.getNodeName())) {
                            String artifactId = child2.getAttributes().getNamedItem("name").getTextContent();
                            if (artifactId != null && artifactId.startsWith("camel-")) {
                                answer.add(artifactId);
                            }
                        }
                    }
                }
            }

            getLog().info("Found " + answer.size() + " Camel features in file: " + file);

        } catch (Exception e) {
            throw new MojoExecutionException("Error reading features.xml file", e);
        }

        return answer;
    }

}
