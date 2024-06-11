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

import org.apache.karaf.features.internal.model.Bundle;
import org.apache.karaf.features.internal.model.Feature;

public abstract class AbstractWrapBundleMojo extends AbstractFeaturesMojo {

    private static final String WRAP_PROTOCOL = "wrap:mvn:";

    @Override
    protected void processFeature(Feature feature) {
        boolean processed = false;
        for (Bundle bundle : feature.getBundle()) {
            String location = bundle.getLocation();
            if (location != null && location.startsWith(WRAP_PROTOCOL)) {
                processed |= processWrappedBundle(bundle);
            }
        }
        if (processed) {
            onFeatureUpdated(feature);
        }
    }

    /**
     * Called when a feature has been updated.
     */
    protected void onFeatureUpdated(Feature feature) {
        // Do nothing by default
    }

    /**
     * Process the given wrapped bundle.
     *
     * @param bundle the bundle
     * @return {@code true} if the feature has been updated, {@code false} otherwise
     */
    protected abstract boolean processWrappedBundle(Bundle bundle);
}
