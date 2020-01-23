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

package de.adorsys.psd2.xs2a.service.authorization.pis.stage.cancellation;

import de.adorsys.psd2.consent.api.pis.authorisation.GetPisAuthorisationResponse;
import de.adorsys.psd2.consent.api.pis.proto.PisPaymentInfo;
import de.adorsys.psd2.consent.api.service.PisAuthorisationServiceEncrypted;
import de.adorsys.psd2.xs2a.core.error.MessageErrorCode;
import de.adorsys.psd2.xs2a.core.error.TppMessage;
import de.adorsys.psd2.xs2a.core.pis.TransactionStatus;
import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.sca.ScaStatus;
import de.adorsys.psd2.xs2a.core.tpp.TppInfo;
import de.adorsys.psd2.xs2a.domain.ErrorHolder;
import de.adorsys.psd2.xs2a.domain.TppMessageInformation;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aUpdatePisCommonPaymentPsuDataRequest;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aUpdatePisCommonPaymentPsuDataResponse;
import de.adorsys.psd2.xs2a.service.RequestProviderService;
import de.adorsys.psd2.xs2a.service.authorization.pis.PisCommonDecoupledService;
import de.adorsys.psd2.xs2a.service.context.SpiContextDataProvider;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ServiceType;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.SpiErrorMapper;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.Xs2aToSpiPsuDataMapper;
import de.adorsys.psd2.xs2a.service.spi.SpiAspspConsentDataProviderFactory;
import de.adorsys.psd2.xs2a.spi.domain.SpiAspspConsentDataProvider;
import de.adorsys.psd2.xs2a.spi.domain.SpiContextData;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.SpiAuthorisationStatus;
import de.adorsys.psd2.xs2a.spi.domain.payment.SpiPaymentInfo;
import de.adorsys.psd2.xs2a.spi.domain.psu.SpiPsuData;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponse;
import de.adorsys.psd2.xs2a.spi.service.PaymentCancellationSpi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PisCancellationDecoupledScaStartAuthorisationStageTest {
    private static final String PAYMENT_PRODUCT = "Test payment product";
    private static final String PAYMENT_ID = "Test payment id";
    private static final String PSU_ID = "Test psuId";
    private static final String PASSWORD = "Test password";
    private static final String AUTHORISATION_ID = "Test authorisation";
    private static final PaymentType SINGLE_PAYMENT_TYPE = PaymentType.SINGLE;
    private static final ServiceType PIS_SERVICE_TYPE = ServiceType.PIS;
    private static final ErrorType PIS_400_ERROR_TYPE = ErrorType.PIS_400;
    private static final ScaStatus FAILED_SCA_STATUS = ScaStatus.FAILED;
    private static final TransactionStatus ACCP_TRANSACTION_STATUS = TransactionStatus.ACCP;
    private static final SpiAuthorisationStatus SUCCESS_SPI_AUTHORISATION_STATUS = SpiAuthorisationStatus.SUCCESS;
    private static final PsuIdData PSU_ID_DATA = new PsuIdData(PSU_ID, null, null, null);
    private static final SpiPsuData SPI_PSU_DATA = new SpiPsuData(PSU_ID, null, null, null, null);
    private static final SpiContextData SPI_CONTEXT_DATA = new SpiContextData(SPI_PSU_DATA, new TppInfo(), UUID.randomUUID(), UUID.randomUUID());
    private static final byte[] PAYMENT_DATA = "Test payment data".getBytes();
    private static final PisPaymentInfo PAYMENT_INFO = buildPisPaymentInfo();
    private static final SpiPaymentInfo SPI_PAYMENT_INFO = buildSpiPaymentInfo();
    private static final GetPisAuthorisationResponse PIS_AUTHORISATION_RESPONSE = new GetPisAuthorisationResponse();

    @InjectMocks
    private PisCancellationDecoupledScaReceivedAuthorisationStage pisCancellationDecoupledScaReceivedAuthorisationStage;

    @Mock
    private PaymentCancellationSpi paymentCancellationSpi;
    @Mock
    private PisCommonDecoupledService pisCommonDecoupledService;
    @Mock
    private SpiContextDataProvider spiContextDataProvider;
    @Mock
    private Xs2aToSpiPsuDataMapper xs2aToSpiPsuDataMapper;
    @Mock
    private SpiErrorMapper spiErrorMapper;
    @Mock
    private RequestProviderService requestProviderService;
    @Mock
    private Xs2aUpdatePisCommonPaymentPsuDataRequest request;
    @Mock
    private GetPisAuthorisationResponse response;
    @Mock
    private Xs2aUpdatePisCommonPaymentPsuDataResponse mockedExpectedResponse;
    @Mock
    private SpiAspspConsentDataProviderFactory aspspConsentDataProviderFactory;
    @Mock
    private SpiAspspConsentDataProvider spiAspspConsentDataProvider;
    @Mock
    private PisAuthorisationServiceEncrypted pisAuthorisationServiceEncrypted;

    @BeforeEach
    void setUp() {
        PIS_AUTHORISATION_RESPONSE.setPsuIdData(PSU_ID_DATA);

        when(response.getPaymentType())
            .thenReturn(SINGLE_PAYMENT_TYPE);

        when(response.getPaymentProduct())
            .thenReturn(PAYMENT_PRODUCT);

        when(response.getPayments())
            .thenReturn(Collections.emptyList());

        when(response.getPaymentInfo())
            .thenReturn(PAYMENT_INFO);

        when(xs2aToSpiPsuDataMapper.mapToSpiPsuData(PSU_ID_DATA))
            .thenReturn(SPI_PSU_DATA);

        when(spiContextDataProvider.provideWithPsuIdData(PSU_ID_DATA))
            .thenReturn(SPI_CONTEXT_DATA);

        when(xs2aToSpiPsuDataMapper.mapToSpiPsuDataList(Collections.singletonList(PSU_ID_DATA)))
            .thenReturn(Collections.singletonList(SPI_PSU_DATA));
    }

    @Test
    void apply_Failure_spiResponseHasError() {
        // Given
        when(aspspConsentDataProviderFactory.getSpiAspspDataProviderFor(PAYMENT_ID)).thenReturn(spiAspspConsentDataProvider);

        when(request.getPassword()).thenReturn(PASSWORD);
        when(request.getPsuData()).thenReturn(PSU_ID_DATA);
        when(request.getPaymentId()).thenReturn(PAYMENT_ID);

        SpiResponse<SpiAuthorisationStatus> expectedResponse = buildErrorSpiResponse();

        when(paymentCancellationSpi.authorisePsu(SPI_CONTEXT_DATA, SPI_PSU_DATA, PASSWORD, SPI_PAYMENT_INFO, spiAspspConsentDataProvider))
            .thenReturn(expectedResponse);

        when(spiErrorMapper.mapToErrorHolder(expectedResponse, PIS_SERVICE_TYPE))
            .thenReturn(ErrorHolder
                            .builder(PIS_400_ERROR_TYPE)
                            .tppMessages(TppMessageInformation.of(MessageErrorCode.FORMAT_ERROR))
                            .build());

        // When
        Xs2aUpdatePisCommonPaymentPsuDataResponse actualResponse = pisCancellationDecoupledScaReceivedAuthorisationStage.apply(request, response);

        // Then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getScaStatus()).isEqualTo(FAILED_SCA_STATUS);
        assertThat(actualResponse.getErrorHolder().getErrorType()).isEqualTo(PIS_400_ERROR_TYPE);
    }

    @Test
    void apply_Success() {
        // Given
        when(aspspConsentDataProviderFactory.getSpiAspspDataProviderFor(PAYMENT_ID)).thenReturn(spiAspspConsentDataProvider);

        when(request.getPassword()).thenReturn(PASSWORD);
        when(request.getPsuData()).thenReturn(PSU_ID_DATA);
        when(request.getPaymentId()).thenReturn(PAYMENT_ID);

        SpiResponse<SpiAuthorisationStatus> expectedResponse = buildSuccessSpiResponse();

        when(paymentCancellationSpi.authorisePsu(SPI_CONTEXT_DATA, SPI_PSU_DATA, PASSWORD, SPI_PAYMENT_INFO, spiAspspConsentDataProvider))
            .thenReturn(expectedResponse);

        when(pisCommonDecoupledService.proceedDecoupledCancellation(request, SPI_PAYMENT_INFO))
            .thenReturn(mockedExpectedResponse);

        // When
        Xs2aUpdatePisCommonPaymentPsuDataResponse actualResponse = pisCancellationDecoupledScaReceivedAuthorisationStage.apply(request, response);

        // Then
        assertThat(actualResponse).isNotNull();
        verify(pisCommonDecoupledService).proceedDecoupledCancellation(request, SPI_PAYMENT_INFO);
    }

    @Test
    void apply_Success_withoutHeaders() {
        // Given
        when(aspspConsentDataProviderFactory.getSpiAspspDataProviderFor(PAYMENT_ID)).thenReturn(spiAspspConsentDataProvider);

        Xs2aUpdatePisCommonPaymentPsuDataRequest request = new Xs2aUpdatePisCommonPaymentPsuDataRequest();
        request.setPaymentId(PAYMENT_ID);
        request.setAuthorisationId(AUTHORISATION_ID);
        request.setPassword(PASSWORD);

        SpiResponse<SpiAuthorisationStatus> expectedResponse = buildSuccessSpiResponse();

        when(paymentCancellationSpi.authorisePsu(SPI_CONTEXT_DATA, SPI_PSU_DATA, PASSWORD, SPI_PAYMENT_INFO, spiAspspConsentDataProvider))
            .thenReturn(expectedResponse);

        when(pisAuthorisationServiceEncrypted.getPisCancellationAuthorisationById(AUTHORISATION_ID))
            .thenReturn(Optional.of(PIS_AUTHORISATION_RESPONSE));

        when(pisCommonDecoupledService.proceedDecoupledCancellation(request, SPI_PAYMENT_INFO))
            .thenReturn(mockedExpectedResponse);

        ArgumentCaptor<Xs2aUpdatePisCommonPaymentPsuDataRequest> captor = ArgumentCaptor.forClass(Xs2aUpdatePisCommonPaymentPsuDataRequest.class);

        // When
        Xs2aUpdatePisCommonPaymentPsuDataResponse actualResponse = pisCancellationDecoupledScaReceivedAuthorisationStage.apply(request, response);

        // Then
        assertThat(actualResponse).isNotNull();
        verify(pisCommonDecoupledService).proceedDecoupledCancellation(captor.capture(), ArgumentMatchers.eq(SPI_PAYMENT_INFO));

        Xs2aUpdatePisCommonPaymentPsuDataRequest captured = captor.getValue();
        assertThat(captured).isNotNull();
        assertThat(captured.getPsuData().isEmpty()).isFalse();
        assertThat(captured.getPsuData()).isEqualTo(PSU_ID_DATA);
    }

    private static PisPaymentInfo buildPisPaymentInfo() {
        PisPaymentInfo paymentInfo = new PisPaymentInfo();
        paymentInfo.setPaymentData(PAYMENT_DATA);
        paymentInfo.setPaymentId(PAYMENT_ID);
        paymentInfo.setPaymentProduct(PAYMENT_PRODUCT);
        paymentInfo.setPaymentType(SINGLE_PAYMENT_TYPE);
        paymentInfo.setTransactionStatus(ACCP_TRANSACTION_STATUS);
        paymentInfo.setPsuDataList(Collections.singletonList(PSU_ID_DATA));
        return paymentInfo;
    }

    private static SpiPaymentInfo buildSpiPaymentInfo() {
        SpiPaymentInfo paymentInfo = new SpiPaymentInfo(PAYMENT_PRODUCT);
        paymentInfo.setPaymentData(PAYMENT_DATA);
        paymentInfo.setPaymentId(PAYMENT_ID);
        paymentInfo.setPaymentType(SINGLE_PAYMENT_TYPE);
        paymentInfo.setStatus(ACCP_TRANSACTION_STATUS);
        paymentInfo.setPsuDataList(Collections.singletonList(SPI_PSU_DATA));
        return paymentInfo;
    }

    // Needed because SpiResponse is final, so it's impossible to mock it
    private SpiResponse<SpiAuthorisationStatus> buildSuccessSpiResponse() {
        return SpiResponse.<SpiAuthorisationStatus>builder()
                   .payload(SUCCESS_SPI_AUTHORISATION_STATUS)
                   .build();
    }

    // Needed because SpiResponse is final, so it's impossible to mock it
    private <T> SpiResponse<T> buildErrorSpiResponse() {
        return SpiResponse.<T>builder()
                   .error(new TppMessage(MessageErrorCode.FORMAT_ERROR))
                   .build();
    }
}
