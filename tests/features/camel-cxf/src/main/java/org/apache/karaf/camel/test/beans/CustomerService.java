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
package org.apache.karaf.camel.test.beans;


import java.io.InputStream;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@Path("/customerservice/")
public interface CustomerService {

    @GET
    @Path("/customers/{id}/")
    Customer getCustomer(@PathParam("id") String id, @QueryParam("test") String test);

    @PUT
    @Path("/customers/{id}")
    Response updateCustomer(Customer customer, @PathParam("id") String id);

    @POST
    @Path("/customers/")
    Response newCustomer(Customer customer, @QueryParam("age") int age);

    @Consumes("image/jpeg")
    @POST
    @Path("/customers/{id}/image_inputstream")
    Response uploadImageInputStream(InputStream is);

}
