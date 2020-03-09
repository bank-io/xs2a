/*
 * Copyright 2018-2018 adorsys GmbH & Co KG
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

package de.adorsys.psd2.consent.config;

import lombok.RequiredArgsConstructor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

@Configuration
@RequiredArgsConstructor
public class HibernateListenerConfig {
    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    private final ServiceInstanceIdEventListener serviceInstanceIdEventListener;

    @PostConstruct
    public void registerListeners() {
        if (entityManagerFactory instanceof SessionFactoryImpl) {
            final SessionFactoryImpl sessionFactory = (SessionFactoryImpl) entityManagerFactory;

            final EventListenerRegistry registry = sessionFactory.getServiceRegistry()
                                                       .getService(EventListenerRegistry.class);

            registry.getEventListenerGroup(EventType.PRE_INSERT)
                .appendListener(serviceInstanceIdEventListener);
        }
    }
}
