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

package de.adorsys.psd2.xs2a.service.payment.create;

import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.tpp.TppInfo;
import de.adorsys.psd2.xs2a.domain.pis.PaymentInitiationParameters;
import de.adorsys.psd2.xs2a.domain.pis.PaymentInitiationResponse;
import de.adorsys.psd2.xs2a.domain.pis.SinglePayment;
import de.adorsys.psd2.xs2a.service.authorization.AuthorisationMethodDecider;
import de.adorsys.psd2.xs2a.service.authorization.pis.PisScaAuthorisationServiceResolver;
import de.adorsys.psd2.xs2a.service.consent.Xs2aPisCommonPaymentService;
import de.adorsys.psd2.xs2a.service.mapper.consent.Xs2aPisCommonPaymentMapper;
import de.adorsys.psd2.xs2a.service.mapper.consent.Xs2aToCmsPisCommonPaymentRequestMapper;
import de.adorsys.psd2.xs2a.service.payment.sca.ScaPaymentService;
import de.adorsys.psd2.xs2a.service.payment.sca.ScaPaymentServiceResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CreateSinglePaymentService extends AbstractCreatePaymentService<SinglePayment> {
    private ScaPaymentServiceResolver scaPaymentServiceResolver;

    @Autowired
    public CreateSinglePaymentService(ScaPaymentServiceResolver scaPaymentServiceResolver,
                                      Xs2aPisCommonPaymentService pisCommonPaymentService,
                                      PisScaAuthorisationServiceResolver pisScaAuthorisationServiceResolver,
                                      AuthorisationMethodDecider authorisationMethodDecider,
                                      Xs2aPisCommonPaymentMapper xs2aPisCommonPaymentMapper,
                                      Xs2aToCmsPisCommonPaymentRequestMapper xs2aToCmsPisCommonPaymentRequestMapper) {
        super(pisCommonPaymentService, pisScaAuthorisationServiceResolver, authorisationMethodDecider,
              xs2aPisCommonPaymentMapper, xs2aToCmsPisCommonPaymentRequestMapper);
        this.scaPaymentServiceResolver = scaPaymentServiceResolver;
    }

    @Override
    protected SinglePayment getPaymentRequest(Object payment, PaymentInitiationParameters paymentInitiationParameters) {
        return (SinglePayment) payment;
    }

    @Override
    protected PaymentInitiationResponse initiatePayment(SinglePayment paymentRequest, PaymentInitiationParameters paymentInitiationParameters,
                                                              TppInfo tppInfo, PsuIdData psuData) {
        ScaPaymentService scaPaymentService = scaPaymentServiceResolver.getService();
        return scaPaymentService.createSinglePayment(paymentRequest, tppInfo, paymentInitiationParameters.getPaymentProduct(), psuData);
    }

    @Override
    protected SinglePayment updateCommonPayment(SinglePayment paymentRequest, PaymentInitiationParameters paymentInitiationParameters,
                                                PaymentInitiationResponse response, String paymentId) {
        paymentRequest.setTransactionStatus(response.getTransactionStatus());
        paymentRequest.setPaymentId(response.getPaymentId());
        pisCommonPaymentService.updateSinglePaymentInCommonPayment(paymentRequest, paymentInitiationParameters, paymentId);
        return paymentRequest;
    }
}