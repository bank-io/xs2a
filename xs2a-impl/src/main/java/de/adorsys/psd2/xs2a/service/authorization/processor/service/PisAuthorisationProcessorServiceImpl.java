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

package de.adorsys.psd2.xs2a.service.authorization.processor.service;

import de.adorsys.psd2.xs2a.core.authorisation.Authorisation;
import de.adorsys.psd2.xs2a.core.domain.ErrorHolder;
import de.adorsys.psd2.xs2a.core.mapper.ServiceType;
import de.adorsys.psd2.xs2a.core.pis.TransactionStatus;
import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.sca.ScaStatus;
import de.adorsys.psd2.xs2a.domain.authorisation.UpdateAuthorisationRequest;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aUpdatePisCommonPaymentPsuDataRequest;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aUpdatePisCommonPaymentPsuDataResponse;
import de.adorsys.psd2.xs2a.service.authorization.Xs2aAuthorisationService;
import de.adorsys.psd2.xs2a.service.authorization.pis.PisCommonDecoupledService;
import de.adorsys.psd2.xs2a.service.authorization.pis.PisExecutePaymentService;
import de.adorsys.psd2.xs2a.service.authorization.pis.PisScaAuthorisationService;
import de.adorsys.psd2.xs2a.service.authorization.processor.model.AuthorisationProcessorRequest;
import de.adorsys.psd2.xs2a.service.authorization.processor.model.AuthorisationProcessorResponse;
import de.adorsys.psd2.xs2a.service.consent.PisAspspDataService;
import de.adorsys.psd2.xs2a.service.consent.Xs2aPisCommonPaymentService;
import de.adorsys.psd2.xs2a.service.context.SpiContextDataProvider;
import de.adorsys.psd2.xs2a.service.mapper.consent.Xs2aPisCommonPaymentMapper;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.SpiErrorMapper;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.Xs2aToSpiPaymentMapper;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.Xs2aToSpiPsuDataMapper;
import de.adorsys.psd2.xs2a.service.payment.Xs2aUpdatePaymentAfterSpiService;
import de.adorsys.psd2.xs2a.service.spi.SpiAspspConsentDataProviderFactory;
import de.adorsys.psd2.xs2a.spi.domain.SpiAspspConsentDataProvider;
import de.adorsys.psd2.xs2a.spi.domain.SpiContextData;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.SpiAuthorizationCodeResult;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.SpiAvailableScaMethodsResponse;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.SpiPsuAuthorisationResponse;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.SpiScaConfirmation;
import de.adorsys.psd2.xs2a.spi.domain.payment.response.SpiPaymentExecutionResponse;
import de.adorsys.psd2.xs2a.spi.domain.psu.SpiPsuData;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponse;
import de.adorsys.psd2.xs2a.spi.service.PaymentAuthorisationSpi;
import de.adorsys.psd2.xs2a.spi.service.SpiPayment;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PisAuthorisationProcessorServiceImpl extends PaymentBaseAuthorisationProcessorService {

    private final PisExecutePaymentService pisExecutePaymentService;
    private final SpiErrorMapper spiErrorMapper;
    private final SpiAspspConsentDataProviderFactory aspspConsentDataProviderFactory;
    private final Xs2aUpdatePaymentAfterSpiService updatePaymentAfterSpiService;
    private final Xs2aPisCommonPaymentService xs2aPisCommonPaymentService;
    private final PaymentAuthorisationSpi paymentAuthorisationSpi;
    private final PisCommonDecoupledService pisCommonDecoupledService;

    public PisAuthorisationProcessorServiceImpl(List<PisScaAuthorisationService> services,
                                                Xs2aToSpiPaymentMapper xs2aToSpiPaymentMapper, PisExecutePaymentService pisExecutePaymentService,
                                                PisAspspDataService pisAspspDataService, Xs2aPisCommonPaymentMapper xs2aPisCommonPaymentMapper,
                                                SpiContextDataProvider spiContextDataProvider, SpiErrorMapper spiErrorMapper,
                                                SpiAspspConsentDataProviderFactory aspspConsentDataProviderFactory,
                                                Xs2aUpdatePaymentAfterSpiService updatePaymentAfterSpiService,
                                                Xs2aAuthorisationService xs2aAuthorisationService,
                                                Xs2aPisCommonPaymentService xs2aPisCommonPaymentService,
                                                PaymentAuthorisationSpi paymentAuthorisationSpi,
                                                PisCommonDecoupledService pisCommonDecoupledService, Xs2aToSpiPsuDataMapper xs2aToSpiPsuDataMapper) {
        super(services, xs2aAuthorisationService, xs2aPisCommonPaymentService, xs2aToSpiPaymentMapper,
              spiContextDataProvider, aspspConsentDataProviderFactory, spiErrorMapper,
              pisAspspDataService, xs2aPisCommonPaymentMapper, xs2aToSpiPsuDataMapper);
        this.pisExecutePaymentService = pisExecutePaymentService;
        this.spiErrorMapper = spiErrorMapper;
        this.aspspConsentDataProviderFactory = aspspConsentDataProviderFactory;
        this.updatePaymentAfterSpiService = updatePaymentAfterSpiService;
        this.xs2aPisCommonPaymentService = xs2aPisCommonPaymentService;
        this.paymentAuthorisationSpi = paymentAuthorisationSpi;
        this.pisCommonDecoupledService = pisCommonDecoupledService;
    }

    @Override
    public void updateAuthorisation(AuthorisationProcessorRequest request, AuthorisationProcessorResponse response) {
        PisScaAuthorisationService authorizationService = getService(request.getScaApproach());
        authorizationService.updateAuthorisation(request.getUpdateAuthorisationRequest(), response);
    }

    @Override
    public AuthorisationProcessorResponse doScaReceived(AuthorisationProcessorRequest authorisationProcessorRequest) {
        Xs2aUpdatePisCommonPaymentPsuDataRequest request = (Xs2aUpdatePisCommonPaymentPsuDataRequest) authorisationProcessorRequest.getUpdateAuthorisationRequest();
        return request.isUpdatePsuIdentification()
                   ? applyIdentification(authorisationProcessorRequest)
                   : applyAuthorisation(authorisationProcessorRequest);
    }

    @Override
    SpiResponse<SpiPaymentExecutionResponse> verifyScaAuthorisationAndExecutePayment(Authorisation authorisation,
                                                                                     SpiPayment payment, SpiScaConfirmation spiScaConfirmation,
                                                                                     SpiContextData contextData, SpiAspspConsentDataProvider spiAspspConsentDataProvider) {
        return pisExecutePaymentService.verifyScaAuthorisationAndExecutePayment(contextData,
                                                                                spiScaConfirmation,
                                                                                payment,
                                                                                spiAspspConsentDataProvider);
    }

    @Override
    void updatePaymentData(String paymentId, SpiResponse<Object> spiResponse) {
        SpiPaymentExecutionResponse payload = (SpiPaymentExecutionResponse) spiResponse.getPayload();
        TransactionStatus paymentStatus = payload.getTransactionStatus();

        if (paymentStatus == TransactionStatus.PATC) {
            xs2aPisCommonPaymentService.updateMultilevelSca(paymentId, true);
        }

        updatePaymentAfterSpiService.updatePaymentStatus(paymentId, paymentStatus);
    }

    @Override
    public AuthorisationProcessorResponse doScaExempted(AuthorisationProcessorRequest authorisationProcessorRequest) {
        UpdateAuthorisationRequest request = authorisationProcessorRequest.getUpdateAuthorisationRequest();
        return new Xs2aUpdatePisCommonPaymentPsuDataResponse(ScaStatus.EXEMPTED, request.getBusinessObjectId(), request.getAuthorisationId(), request.getPsuData());
    }

    @Override
    boolean needProcessExemptedSca(PaymentType paymentType, boolean isScaExempted) {
        return isScaExempted && paymentType != PaymentType.PERIODIC;
    }

    @Override
    SpiResponse<SpiAvailableScaMethodsResponse> requestAvailableScaMethods(SpiPayment payment, SpiAspspConsentDataProvider aspspConsentDataProvider, SpiContextData contextData) {
        return paymentAuthorisationSpi.requestAvailableScaMethods(contextData, payment, aspspConsentDataProvider);
    }

    @Override
    SpiResponse<SpiPsuAuthorisationResponse> authorisePsu(Xs2aUpdatePisCommonPaymentPsuDataRequest request, SpiPayment payment,
                                                          SpiAspspConsentDataProvider aspspConsentDataProvider, SpiPsuData spiPsuData,
                                                          SpiContextData contextData) {
        return paymentAuthorisationSpi.authorisePsu(contextData, spiPsuData, request.getPassword(), payment, aspspConsentDataProvider);
    }

    @Override
    SpiResponse<SpiAuthorizationCodeResult> requestAuthorisationCode(SpiPayment payment, String authenticationMethodId,
                                                                     SpiContextData spiContextData, SpiAspspConsentDataProvider spiAspspConsentDataProvider) {
        return paymentAuthorisationSpi.requestAuthorisationCode(spiContextData, authenticationMethodId, payment, spiAspspConsentDataProvider);
    }

    @Override
    Xs2aUpdatePisCommonPaymentPsuDataResponse proceedDecoupledApproach(Xs2aUpdatePisCommonPaymentPsuDataRequest request, SpiPayment payment, String authenticationMethodId) {
        return pisCommonDecoupledService.proceedDecoupledInitiation(request, payment, authenticationMethodId);
    }

    @Override
    Xs2aUpdatePisCommonPaymentPsuDataResponse executePaymentWithoutSca(AuthorisationProcessorRequest authorisationProcessorRequest, PsuIdData psuData, PaymentType paymentType, SpiPayment payment, SpiContextData contextData, ScaStatus resultScaStatus) {
        Xs2aUpdatePisCommonPaymentPsuDataRequest request = (Xs2aUpdatePisCommonPaymentPsuDataRequest) authorisationProcessorRequest.getUpdateAuthorisationRequest();
        String authorisationId = request.getAuthorisationId();
        String paymentId = request.getPaymentId();

        final SpiAspspConsentDataProvider aspspConsentDataProvider = aspspConsentDataProviderFactory.getSpiAspspDataProviderFor(paymentId);

        SpiResponse<SpiPaymentExecutionResponse> spiResponse = pisExecutePaymentService.executePaymentWithoutSca(contextData, payment, aspspConsentDataProvider);

        if (spiResponse.hasError()) {
            ErrorHolder errorHolder = spiErrorMapper.mapToErrorHolder(spiResponse, ServiceType.PIS);
            writeErrorLog(authorisationProcessorRequest, psuData, errorHolder, "Execute payment without SCA has failed.");
            return new Xs2aUpdatePisCommonPaymentPsuDataResponse(errorHolder, paymentId, authorisationId, psuData);
        }

        TransactionStatus paymentStatus = spiResponse.getPayload().getTransactionStatus();

        if (paymentStatus == TransactionStatus.PATC) {
            xs2aPisCommonPaymentService.updateMultilevelSca(paymentId, true);
        }

        updatePaymentAfterSpiService.updatePaymentStatus(paymentId, paymentStatus);
        return new Xs2aUpdatePisCommonPaymentPsuDataResponse(resultScaStatus, paymentId, authorisationId, psuData);
    }
}
