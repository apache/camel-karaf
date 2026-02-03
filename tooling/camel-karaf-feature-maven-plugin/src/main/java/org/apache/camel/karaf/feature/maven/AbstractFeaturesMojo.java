/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.camel.karaf.feature.maven;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.karaf.features.internal.model.Feature;
import org.apache.karaf.features.internal.model.Features;
import org.apache.karaf.features.internal.model.JaxbUtil;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

public abstract class AbstractFeaturesMojo extends AbstractMojo {

    private static final String FILE_PROTOCOL = "file:";

    private static final String DEFAULT_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>";
    private static final String LICENCE_HEADER = """
<?xml version="1.0" encoding="UTF-8"?>
<!--

    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements.  See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

-->""";

    @Parameter(required = true)
    private String featuresFilePath;

    @Parameter
    private String featuresFileResultPath;

    /**
     * If true, the "Wrap" prefix will be additionally added to the Bundle-Name and Bundle-Symbolic-Name.
     */
    @Parameter(defaultValue = "true")
    boolean addWrapPrefix = true;

    public String getFeaturesFilePath() {
        return featuresFilePath;
    }

    public String getFeaturesFileResultPath() {
        return featuresFileResultPath;
    }

    @Override
    public void execute() throws MojoExecutionException {
        Features featuresData = JaxbUtil.unmarshal(getFeaturesFilePath(), false);
        processFeatures(featuresData.getFeature());
        marshal(featuresData);
    }

    private void marshal(Features featuresData) throws MojoExecutionException {
        String outputPath = getFeaturesFileResultPath();
        if (outputPath == null) {
            outputPath = getFeaturesFilePath();
        }
        try (StringWriter writer = new StringWriter()) {
            JaxbUtil.marshal(featuresData, writer);

            String result = writer.toString().replace(DEFAULT_HEADER, LICENCE_HEADER);

            Path path = Paths.get(outputPath.replaceFirst(FILE_PROTOCOL, ""));

            Files.createDirectories(path.getParent());

            Files.writeString(path, result);

            getLog().info("File '%s' was successfully modified and saved".formatted(outputPath));
        } catch (Exception e) {
            getLog().error("File '%s' was successfully modified but an error occurred while saving it"
                    .formatted(outputPath), e);
            throw new MojoExecutionException(e);
        }
    }


    private void processFeatures(List<Feature> features) {
        for (Feature feature : features) {
            processFeature(feature);
        }
    }

    /**
     * Process the given feature.
     */
    protected abstract void processFeature(Feature feature);
}
