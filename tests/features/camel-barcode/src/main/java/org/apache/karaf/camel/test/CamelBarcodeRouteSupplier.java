/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.camel.test;

import static org.apache.camel.builder.Builder.constant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

import javax.imageio.ImageIO;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.barcode.BarcodeDataFormat;
import org.apache.camel.dataformat.barcode.BarcodeImageType;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.spi.DataFormat;
import org.apache.karaf.camel.itests.AbstractCamelSingleFeatureResultMockBasedRouteSupplier;
import org.osgi.service.component.annotations.Component;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;

@Component(
        name = "karaf-camel-barcode-test",
        immediate = true,
        service = CamelBarcodeRouteSupplier.class
)
public class CamelBarcodeRouteSupplier extends AbstractCamelSingleFeatureResultMockBasedRouteSupplier {

    @Override
    protected Function<RouteBuilder, RouteDefinition> consumerRoute() {

        try (DataFormat code1 = new BarcodeDataFormat(200, 200, BarcodeImageType.PNG, BarcodeFormat.CODE_39)) {
            final Path testDirectory = Files.createTempFile("barcode", ".png");

            return builder -> builder.from("timer://testTimer?repeatCount=1")
                    .setBody(constant("OK"))
                    .marshal(code1)
                    .process(new Processor() {
                        @Override
                        public void process(Exchange exchange) throws Exception {
                            InputStream bis = exchange.getIn().getBody(InputStream.class);
                            BinaryBitmap bitmap =
                                    new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(ImageIO.read(bis))));
                            BitMatrix blackMatrix = bitmap.getBlackMatrix();
                            blackMatrix.rotate180();
                            File file = testDirectory.toFile();
                            file.deleteOnExit();
                            FileOutputStream outputStream = new FileOutputStream(file);
                            MatrixToImageWriter.writeToStream(blackMatrix, "png", outputStream);
                            exchange.getIn().setBody(file);
                        }
                    })
                    .unmarshal(code1)
                    .log("unmarshalled ${body}");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    protected boolean producerEnabled() {
        return false;
    }
}

