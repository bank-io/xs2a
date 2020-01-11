/*
 * Copyright 2018-2020 adorsys GmbH & Co KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.adorsys.psd2.aspsp.profile.config;

import de.adorsys.psd2.logger.web.LoggingContextInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@Configuration
@RequiredArgsConstructor
public class AspspProfileRestConfig {
    private final LoggingContextInterceptor loggingContextInterceptor;

    @Value("${http-client.read-timeout.ms:10000}")
    private int readTimeout;
    @Value("${http-client.connection-timeout.ms:10000}")
    private int connectionTimeout;

    @Bean(name = "aspspProfileRestTemplate")
    public RestTemplate aspspProfileRestTemplate() {
        RestTemplate rest = new RestTemplate(clientHttpRequestFactory());
        rest.getMessageConverters().removeIf(m -> m.getClass().isAssignableFrom(MappingJackson2XmlHttpMessageConverter.class));
        rest.setErrorHandler(new AspspProfileRestErrorHandler());
        rest.getInterceptors().add(loggingContextInterceptor);
        return rest;
    }

    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setReadTimeout(readTimeout);
        factory.setConnectTimeout(connectionTimeout);
        return factory;
    }
}
