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

package org.apache.camel.component.cxf.blueprint.configuration;

import org.apache.cxf.interceptor.Interceptor;
import org.osgi.service.blueprint.container.Converter;
import org.osgi.service.blueprint.container.ReifiedType;

/**
 *
 */
public class InterceptorTypeConverter implements Converter {

    /** {@inheritDoc}*/
    public boolean canConvert(Object sourceObject, ReifiedType targetType) {
        return sourceObject instanceof Interceptor
                && targetType.getRawClass().isInstance(sourceObject);
    }

    /** {@inheritDoc}*/
    public Object convert(Object sourceObject, ReifiedType targetType) throws Exception {
        return sourceObject;
    }

}
