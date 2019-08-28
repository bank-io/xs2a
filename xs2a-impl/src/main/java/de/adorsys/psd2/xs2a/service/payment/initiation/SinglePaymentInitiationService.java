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

package de.adorsys.psd2.xs2a.service.payment.initiation;

import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import de.adorsys.psd2.xs2a.domain.pis.SinglePayment;
import de.adorsys.psd2.xs2a.domain.pis.SinglePaymentInitiationResponse;
import de.adorsys.psd2.xs2a.service.RequestProviderService;
import de.adorsys.psd2.xs2a.service.context.SpiContextDataProvider;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.SpiErrorMapper;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.SpiToXs2aPaymentMapper;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.Xs2aToSpiSinglePaymentMapper;
import de.adorsys.psd2.xs2a.service.spi.InitialSpiAspspConsentDataProvider;
import de.adorsys.psd2.xs2a.service.spi.SpiAspspConsentDataProviderFactory;
import de.adorsys.psd2.xs2a.spi.domain.payment.SpiSinglePayment;
import de.adorsys.psd2.xs2a.spi.domain.payment.response.SpiSinglePaymentInitiationResponse;
import de.adorsys.psd2.xs2a.spi.service.PaymentSpi;
import de.adorsys.psd2.xs2a.spi.service.SinglePaymentSpi;
import org.springframework.stereotype.Service;

@Service
public class SinglePaymentInitiationService extends AbstractPaymentInitiationService<SinglePayment, SpiSinglePayment, SpiSinglePaymentInitiationResponse> {
    private final SpiToXs2aPaymentMapper spiToXs2aPaymentMapper;
    private final Xs2aToSpiSinglePaymentMapper xs2aToSpiSinglePaymentMapper;
    private final SinglePaymentSpi singlePaymentSpi;

    public SinglePaymentInitiationService(SpiContextDataProvider spiContextDataProvider, SpiAspspConsentDataProviderFactory aspspConsentDataProviderFactory,
                                          SpiErrorMapper spiErrorMapper, RequestProviderService requestProviderService, SpiToXs2aPaymentMapper spiToXs2aPaymentMapper,
                                          Xs2aToSpiSinglePaymentMapper xs2aToSpiSinglePaymentMapper, SinglePaymentSpi singlePaymentSpi) {
        super(spiContextDataProvider, aspspConsentDataProviderFactory, spiErrorMapper, requestProviderService);
        this.spiToXs2aPaymentMapper = spiToXs2aPaymentMapper;
        this.xs2aToSpiSinglePaymentMapper = xs2aToSpiSinglePaymentMapper;
        this.singlePaymentSpi = singlePaymentSpi;
    }

    @Override
    PaymentSpi<SpiSinglePayment, SpiSinglePaymentInitiationResponse> getSpiService() {
        return singlePaymentSpi;
    }

    @Override
    SpiSinglePayment mapToSpiPayment(SinglePayment xs2aPayment, String paymentProduct) {
        return xs2aToSpiSinglePaymentMapper.mapToSpiSinglePayment(xs2aPayment, paymentProduct);
    }

    @Override
    SinglePaymentInitiationResponse mapToXs2aResponse(SpiSinglePaymentInitiationResponse spiResponse, InitialSpiAspspConsentDataProvider provider, PaymentType paymentType) {
        return spiToXs2aPaymentMapper.mapToPaymentInitiateResponse(spiResponse, provider);
    }
}
