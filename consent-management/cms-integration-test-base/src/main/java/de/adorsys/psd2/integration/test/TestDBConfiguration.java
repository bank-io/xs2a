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

package de.adorsys.psd2.integration.test;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.sql.DataSource;
import java.sql.SQLException;

@Configuration
@EnableJpaRepositories("de.adorsys.psd2.consent.repository")
@EnableAutoConfiguration
@EntityScan("de.adorsys.psd2.consent.domain")
@ComponentScan(basePackages = {
    "de.adorsys.psd2.consent.repository.specification"
})
public class TestDBConfiguration {

    @Bean
    public SpringLiquibase springLiquibase(DataSource dataSource) throws SQLException {
        // here we create the schema first if not yet created before
//        tryToCreateSchema(dataSource);
        SpringLiquibase liquibase = new SpringLiquibase();
        // we want to drop the datasbe if it was created before to have immutable version
        liquibase.setDropFirst(true);
        liquibase.setDataSource(dataSource);
        //you set the schema name which will be used into ur integration test
//        liquibase.setDefaultSchema("test");
        // the classpath reference for your liquibase changlog
        liquibase.setChangeLog("classpath:/master.xml");
        return liquibase;
    }
}
