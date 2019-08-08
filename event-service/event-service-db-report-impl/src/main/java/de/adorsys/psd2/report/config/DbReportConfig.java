/*
 * Copyright 2018-2019 adorsys GmbH & Co KG
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

package de.adorsys.psd2.report.config;

import de.adorsys.psd2.report.jpa.builder.SqlEventReportBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DbReportConfig {
    private static final String SQL_REQUEST_FILE_NAME = "base_event_report_db.sql";
    private static final String ORACLE_SQL_REQUEST_FILE_NAME = "base_event_report_oracle.sql";

    @Value("${spring.datasource.driver-class-name:oracle.jdbc.OracleDriver}")
    private String driverName;

    @Bean
    public SqlEventReportBuilder getSqlEventReportBuilder() {
        return driverName.toLowerCase().contains("oracle")
                   ? new SqlEventReportBuilder(ORACLE_SQL_REQUEST_FILE_NAME)
                   : new SqlEventReportBuilder(SQL_REQUEST_FILE_NAME);
    }

}
