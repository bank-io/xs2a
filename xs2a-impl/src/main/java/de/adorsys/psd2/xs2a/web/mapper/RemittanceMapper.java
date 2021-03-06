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

package de.adorsys.psd2.xs2a.web.mapper;

import de.adorsys.psd2.model.RemittanceInformationStructured;
import de.adorsys.psd2.xs2a.core.pis.Remittance;
import de.adorsys.psd2.xs2a.spi.domain.payment.SpiRemittance;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RemittanceMapper {
    RemittanceInformationStructured mapToRemittanceInformationStructured(Remittance remittance);

    Remittance mapToRemittance(RemittanceInformationStructured remittanceInformationStructured);

    SpiRemittance mapToSpiRemittance(Remittance remittance);

    Remittance mapToRemittance(SpiRemittance spiRemittance);
}
