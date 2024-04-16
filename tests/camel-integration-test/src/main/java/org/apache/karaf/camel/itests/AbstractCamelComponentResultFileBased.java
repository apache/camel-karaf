/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.camel.itests;

import org.apache.camel.model.RouteDefinition;

public abstract class AbstractCamelComponentResultFileBased extends AbstractCamelComponent {

    protected String getResultFileName() {
        return getTestComponentName();
    }

    @Override
    protected void configureConsumer(RouteDefinition consumerRoute) {
        consumerRoute.toF("file:%s?fileName=%s", getBaseDir(), getResultFileName());
    }
}
