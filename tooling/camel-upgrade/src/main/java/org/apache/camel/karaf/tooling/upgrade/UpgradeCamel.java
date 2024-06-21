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
package org.apache.camel.karaf.tooling.upgrade;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpgradeCamel {

    private static final String BANNER = """
              ___                   _            _____                      _   _   _                           _        _____           _\s
             / _ \\                 | |          /  __ \\                    | | | | | |                         | |      |_   _|         | |
            / /_\\ \\_ __   __ _  ___| |__   ___  | /  \\/ __ _ _ __ ___   ___| | | | | |_ __   __ _ _ __ __ _  __| | ___    | | ___   ___ | |
            |  _  | '_ \\ / _` |/ __| '_ \\ / _ \\ | |    / _` | '_ ` _ \\ / _ \\ | | | | | '_ \\ / _` | '__/ _` |/ _` |/ _ \\   | |/ _ \\ / _ \\| |
            | | | | |_) | (_| | (__| | | |  __/ | \\__/\\ (_| | | | | | |  __/ | | |_| | |_) | (_| | | | (_| | (_| |  __/   | | (_) | (_) | |
            \\_| |_/ .__/ \\__,_|\\___|_| |_|\\___|  \\____/\\__,_|_| |_| |_|\\___|_|  \\___/| .__/ \\__, |_|  \\__,_|\\__,_|\\___|   \\_/\\___/ \\___/|_|
                  | |                                                                | |     __/ |                                        \s
                  |_|                                                                |_|    |___/                                         \s
            """;
    private static final Logger LOG = LoggerFactory.getLogger(UpgradeCamel.class);

    private static final Set<String> EXCLUDED_COMPONENTS = Set.of("camel-cxf-common", "camel-cxf-rest", "camel-cxf-soap",
            "camel-cxf-spring-common", "camel-cxf-spring-rest", "camel-cxf-spring-soap", "camel-cxf-spring-transport",
            "camel-cxf-transport", "camel-test-junit5", "camel-test-main-junit5", "camel-test-spring-junit5",
            "camel-blueprint", "camel-cxf-all", "camel-cxf-blueprint", "camel-cxf-spring-all",
            "camel-cxf-transport-blueprint", "camel-directvm", "camel-test", "camel-test-spring", "camel-vm",
            "camel-yaml-dsl");
    private static final String COMPONENTS_FOLDER = "components";

    public static void main(String[] args) throws Exception {
        LOG.info(BANNER);
        UserInputProvider userInputProvider = new UserInputProvider(System.console(), new DefaultValuesProvider("camel-upgrade.properties"));
        upgrade(
            userInputProvider.readCamelVersion(), userInputProvider.readCamelKarafRootDirectory(),
            userInputProvider.readCamelRootDirectory(), userInputProvider::doAction
        );
    }

    private static void upgrade(String camelVersion, Path camelKarafRoot, Path camelRoot, Function<String, Boolean> askForAction) throws GitAPIException, IOException {
        try (UpgradeCamelContext context = new UpgradeCamelContext(camelVersion, camelKarafRoot, camelRoot, askForAction)) {
            LOG.info("Updating the pom files");
            updateAllPoms(context);
            checkoutCamelIfNeeded(context);
            LOG.info("Updating the parent pom file");
            updateParentPom(context);
            compareComponents(context);
        }
    }

    private static void updateAllPoms(UpgradeCamelContext context) throws IOException {
        new PomUpdater(context.camelKarafRoot(), context.camelVersion()).execute();
    }

    private static void updateParentPom(UpgradeCamelContext context) throws IOException {
        new ParentPomUpdater(context.camelKarafRoot().resolve("pom.xml"),
                context.camelRoot().resolve("parent/pom.xml"),
                context.camelVersion())
                .execute();
    }

    private static void compareComponents(UpgradeCamelContext context) throws IOException {
        new UpgradeCamelComponentComparator(context).execute();
    }

    private static void checkoutCamelIfNeeded(UpgradeCamelContext context) throws GitAPIException {
        if (context.cloned()) {
            checkoutCamel(context);
        }
    }

    private static void checkoutCamel(UpgradeCamelContext context) throws GitAPIException {
        LOG.info("Cloning the Camel repository into {}", context.camelRoot().toAbsolutePath());
        String tag = "refs/tags/camel-%s".formatted(context.camelVersion());
        try (Git git = Git.cloneRepository()
                .setURI("https://github.com/apache/camel.git")
                .setDirectory(context.camelRoot().toFile())
                .setBranchesToClone(List.of(tag))
                .setBranch(tag)
                .call()) {
            LOG.info("Camel repository successfully cloned");
        }
    }

    private static class UpgradeCamelComponentComparator extends ComponentComparator {
        private final UpgradeCamelContext context;

        UpgradeCamelComponentComparator(UpgradeCamelContext context) {
            super(context.camelKarafRoot().resolve(COMPONENTS_FOLDER), context.camelRoot().resolve(COMPONENTS_FOLDER));
            this.context = context;
        }

        @Override
        protected void onAddSubComponent(String parent, String subComponent) throws IOException {
            if (EXCLUDED_COMPONENTS.contains(subComponent)) {
                LOG.debug("Ignoring the sub component {}", subComponent);
                return;
            }
            if (context.doAction("The sub component %s is not in Camel Karaf. Do you want to add it?".formatted(subComponent))) {
                addSubComponent(parent, subComponent);
            }
        }

        private void addSubComponent(String parent, String subComponent) throws IOException {
            LOG.info("Adding sub component {}", subComponent);
            context.multiModuleWrapperHandler().add(parent, subComponent);
            context.featureHandler().add(subComponent);
        }

        @Override
        protected void onAddComponent(String component) throws IOException {
            if (EXCLUDED_COMPONENTS.contains(component)) {
                LOG.debug("Ignoring the component {}", component);
                return;
            }
            if (context.doAction("The component %s is not in Camel Karaf. Do you want to add it?".formatted(component))) {
                addComponent(component);
            }
        }

        private void addComponent(String component) throws IOException {
            LOG.info("Adding component {}", component);
            context.singleModuleWrapperHandler().add(component);
            context.featureHandler().add(component);
        }

        @Override
        protected void onRemoveSubComponent(String parent, String subComponent) throws IOException {
            if (EXCLUDED_COMPONENTS.contains(subComponent)) {
                LOG.debug("Ignoring the sub component {}", subComponent);
                return;
            }
            if (context.doAction("The sub component %s is not in Camel. Do you want to remove it?".formatted(subComponent))) {
                removeSubComponent(parent, subComponent);
            }
        }

        private void removeSubComponent(String parent, String subComponent) throws IOException {
            LOG.info("Removing sub component {}", subComponent);
            context.multiModuleWrapperHandler().remove(parent, subComponent);
            context.featureHandler().remove(subComponent);
        }

        @Override
        protected void onRemoveComponent(String component) throws IOException {
            if (EXCLUDED_COMPONENTS.contains(component)) {
                LOG.debug("Ignoring the component {}", component);
                return;
            }
            if (context.doAction("The component %s is not in Camel. Do you want to remove it?".formatted(component))) {
                removeComponent(component);
            }
        }

        private void removeComponent(String component) throws IOException {
            LOG.info("Removing component {}", component);
            context.singleModuleWrapperHandler().remove(component);
            context.featureHandler().remove(component);
        }

        @Override
        protected void beforeRemovingComponents() {
            LOG.info(">> Removing the components that are not in Camel");
        }

        @Override
        protected void beforeAddingComponents() {
            LOG.info(">> Adding the components that are not in Camel Karaf");
        }
    }

    private static class UpgradeCamelContext implements Closeable {
        private final String camelVersion;
        private final Path camelKarafRoot;
        private final Path camelRoot;
        private final boolean cloned;
        private final Function<String, Boolean> askForAction;
        private final MultiModuleWrapperHandler multiModuleWrapperHandler;
        private final SingleModuleWrapperHandler singleModuleWrapperHandler;
        private final FeatureHandler featureHandler;

        private UpgradeCamelContext(String camelVersion, Path camelKarafRoot, Path camelRoot,
                                    Function<String, Boolean> askForAction) throws IOException {
            this.camelVersion = camelVersion;
            this.camelKarafRoot = camelKarafRoot;
            if (camelRoot == null) {
                this.camelRoot = Files.createTempDirectory("camel-upgrade");
                this.cloned = true;
            } else {
                this.camelRoot = camelRoot;
                this.cloned = false;
            }
            this.askForAction = askForAction;
            Path camelKarafComponentRoot = camelKarafRoot.resolve(COMPONENTS_FOLDER);
            Path camelComponentRoot = camelRoot.resolve(COMPONENTS_FOLDER);
            this.multiModuleWrapperHandler = new MultiModuleWrapperHandler(camelKarafComponentRoot, camelComponentRoot,
                    camelVersion);
            this.singleModuleWrapperHandler = new SingleModuleWrapperHandler(camelKarafComponentRoot, camelComponentRoot,
                    camelVersion);
            this.featureHandler = new FeatureHandler(camelKarafRoot.resolve("features/src/main/feature/camel-features.xml"));
        }

        String camelVersion() {
            return camelVersion;
        }

        Path camelKarafRoot() {
            return camelKarafRoot;
        }

        Path camelRoot() {
            return camelRoot;
        }

        boolean cloned() {
            return cloned;
        }

        boolean doAction(String messageFormat) {
            return askForAction.apply(messageFormat);
        }

        MultiModuleWrapperHandler multiModuleWrapperHandler() {
            return multiModuleWrapperHandler;
        }

        SingleModuleWrapperHandler singleModuleWrapperHandler() {
            return singleModuleWrapperHandler;
        }

        FeatureHandler featureHandler() {
            return featureHandler;
        }

        @Override
        public void close() throws IOException {
            if (cloned) {
                FileUtils.deleteDirectory(camelRoot.toFile());
            }
        }
    }
}