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


package de.adorsys.psd2.xs2a.service.consent;

import de.adorsys.psd2.consent.api.ActionStatus;
import de.adorsys.psd2.consent.api.CmsError;
import de.adorsys.psd2.consent.api.CmsResponse;
import de.adorsys.psd2.consent.api.WrongChecksumException;
import de.adorsys.psd2.consent.api.ais.AisConsentActionRequest;
import de.adorsys.psd2.consent.api.ais.CmsConsent;
import de.adorsys.psd2.consent.api.authorisation.CreateAuthorisationRequest;
import de.adorsys.psd2.consent.api.authorisation.CreateAuthorisationResponse;
import de.adorsys.psd2.consent.api.authorisation.UpdateAuthorisationRequest;
import de.adorsys.psd2.consent.api.consent.CmsCreateConsentResponse;
import de.adorsys.psd2.consent.api.service.AisConsentServiceEncrypted;
import de.adorsys.psd2.consent.api.service.ConsentServiceEncrypted;
import de.adorsys.psd2.core.data.AccountAccess;
import de.adorsys.psd2.core.data.ais.AisConsent;
import de.adorsys.psd2.logger.context.LoggingContextService;
import de.adorsys.psd2.xs2a.core.authorisation.AuthorisationType;
import de.adorsys.psd2.xs2a.core.consent.ConsentStatus;
import de.adorsys.psd2.xs2a.core.consent.ConsentTppInformation;
import de.adorsys.psd2.xs2a.core.profile.NotificationSupportedMode;
import de.adorsys.psd2.xs2a.core.profile.ScaApproach;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.sca.ScaStatus;
import de.adorsys.psd2.xs2a.core.tpp.TppInfo;
import de.adorsys.psd2.xs2a.core.tpp.TppRole;
import de.adorsys.psd2.xs2a.domain.account.Xs2aCreateAisConsentResponse;
import de.adorsys.psd2.xs2a.domain.consent.CreateConsentReq;
import de.adorsys.psd2.xs2a.domain.consent.UpdateConsentPsuDataReq;
import de.adorsys.psd2.xs2a.service.RequestProviderService;
import de.adorsys.psd2.xs2a.service.ScaApproachResolver;
import de.adorsys.psd2.xs2a.service.authorization.Xs2aAuthorisationService;
import de.adorsys.psd2.xs2a.service.mapper.consent.Xs2aAisConsentAuthorisationMapper;
import de.adorsys.psd2.xs2a.service.mapper.consent.Xs2aAisConsentMapper;
import de.adorsys.psd2.xs2a.service.profile.FrequencyPerDateCalculationService;
import de.adorsys.xs2a.reader.JsonReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Xs2aAisConsentServiceTest {
    private static final String CONSENT_ID = "f2c43cad-6811-4cb6-bfce-31050095ed5d";
    private static final String AUTHORISATION_ID = "a01562ea-19ff-4b5a-8188-c45d85bfa20a";
    private static final String TPP_ID = "Test TppId";
    private static final String REQUEST_URI = "request/uri";
    private static final String REDIRECT_URI = "request/redirect_uri";
    private static final String NOK_REDIRECT_URI = "request/nok_redirect_uri";
    private static final ScaStatus SCA_STATUS = ScaStatus.RECEIVED;
    private static final ScaApproach SCA_APPROACH = ScaApproach.DECOUPLED;
    private static final CreateConsentReq CREATE_CONSENT_REQ = buildCreateConsentReq();
    private static final PsuIdData PSU_DATA = new PsuIdData("psuId", "psuIdType", "psuCorporateId", "psuCorporateIdType", "psuIpAddress");
    private static final TppInfo TPP_INFO = buildTppInfo();
    private static final CmsConsent CMS_CONSENT = new CmsConsent();
    private static final ConsentStatus CONSENT_STATUS = ConsentStatus.VALID;
    private static final CreateAuthorisationRequest AIS_CONSENT_AUTHORISATION_REQUEST = buildAisConsentAuthorisationRequest();

    @InjectMocks
    private Xs2aAisConsentService xs2aAisConsentService;

    @Mock
    private ConsentServiceEncrypted consentServiceEncrypted;
    @Mock
    private AisConsentServiceEncrypted aisConsentServiceEncrypted;
    @Mock
    private Xs2aAuthorisationService authorisationService;
    @Mock
    private Xs2aAisConsentMapper aisConsentMapper;
    @Mock
    private Xs2aAisConsentAuthorisationMapper aisConsentAuthorisationMapper;
    @Mock
    private FrequencyPerDateCalculationService frequencyPerDateCalculationService;
    @Mock
    private ScaApproachResolver scaApproachResolver;
    @Mock
    private RequestProviderService requestProviderService;
    @Mock
    private LoggingContextService loggingContextService;

    private JsonReader jsonReader = new JsonReader();
    private AisConsent aisConsent;

    @BeforeEach
    void init() {
        aisConsent = jsonReader.getObjectFromFile("json/service/ais-consent.json", AisConsent.class);
    }

    @Test
    void createConsent_success() throws WrongChecksumException {
        // Given
        when(frequencyPerDateCalculationService.getMinFrequencyPerDay(CREATE_CONSENT_REQ.getFrequencyPerDay()))
            .thenReturn(1);
        when(aisConsentMapper.mapToCmsConsent(CREATE_CONSENT_REQ, PSU_DATA, TPP_INFO, 1))
            .thenReturn(CMS_CONSENT);
        when(consentServiceEncrypted.createConsent(any()))
            .thenReturn(CmsResponse.<CmsCreateConsentResponse>builder()
                            .payload(new CmsCreateConsentResponse(CONSENT_ID, getCmsConsentWithNotifications()))
                            .build());
        when(aisConsentMapper.mapToAisConsent(any()))
            .thenReturn(aisConsent);

        Xs2aCreateAisConsentResponse expected = new Xs2aCreateAisConsentResponse(CONSENT_ID, aisConsent, null);

        // When
        Optional<Xs2aCreateAisConsentResponse> actualResponse = xs2aAisConsentService.createConsent(CREATE_CONSENT_REQ, PSU_DATA, TPP_INFO);

        // Then
        assertTrue(actualResponse.isPresent());
        assertEquals(expected, actualResponse.get());
    }

    @Test
    void createConsent_WrongChecksumException() throws WrongChecksumException {
        // Given
        when(frequencyPerDateCalculationService.getMinFrequencyPerDay(CREATE_CONSENT_REQ.getFrequencyPerDay()))
            .thenReturn(1);
        when(aisConsentMapper.mapToCmsConsent(CREATE_CONSENT_REQ, PSU_DATA, TPP_INFO, 1))
            .thenReturn(CMS_CONSENT);
        when(consentServiceEncrypted.createConsent(any())).thenThrow(new WrongChecksumException());

        // When
        Optional<Xs2aCreateAisConsentResponse> actualResponse = xs2aAisConsentService.createConsent(CREATE_CONSENT_REQ, PSU_DATA, TPP_INFO);

        // Then
        assertTrue(actualResponse.isEmpty());
    }

    @Test
    void createConsent_failed() throws WrongChecksumException {
        // Given
        when(frequencyPerDateCalculationService.getMinFrequencyPerDay(CREATE_CONSENT_REQ.getFrequencyPerDay()))
            .thenReturn(1);
        when(aisConsentMapper.mapToCmsConsent(CREATE_CONSENT_REQ, PSU_DATA, TPP_INFO, 1))
            .thenReturn(CMS_CONSENT);
        when(consentServiceEncrypted.createConsent(any(CmsConsent.class)))
            .thenReturn(CmsResponse.<CmsCreateConsentResponse>builder().error(CmsError.TECHNICAL_ERROR).build());

        // When
        Optional<Xs2aCreateAisConsentResponse> actualResponse = xs2aAisConsentService.createConsent(CREATE_CONSENT_REQ, PSU_DATA, TPP_INFO);

        // Then
        assertFalse(actualResponse.isPresent());
    }

    @Test
    void getAccountConsentById_success() {
        // Given
        when(consentServiceEncrypted.getConsentById(CONSENT_ID))
            .thenReturn(CmsResponse.<CmsConsent>builder().payload(CMS_CONSENT).build());
        when(aisConsentMapper.mapToAisConsent(CMS_CONSENT))
            .thenReturn(aisConsent);

        // When
        Optional<AisConsent> actualResponse = xs2aAisConsentService.getAccountConsentById(CONSENT_ID);

        // Then
        assertThat(actualResponse.isPresent()).isTrue();
        assertThat(actualResponse.get()).isEqualTo(aisConsent);
    }

    @Test
    void getAccountConsentById_failed() {
        // Given
        when(consentServiceEncrypted.getConsentById(CONSENT_ID))
            .thenReturn(CmsResponse.<CmsConsent>builder().error(CmsError.TECHNICAL_ERROR).build());

        // When
        Optional<AisConsent> actualResponse = xs2aAisConsentService.getAccountConsentById(CONSENT_ID);

        // Then
        assertThat(actualResponse.isPresent()).isFalse();
    }

    @Test
    void findAndTerminateOldConsentsByNewConsentId_success() {
        // Given
        when(consentServiceEncrypted.findAndTerminateOldConsentsByNewConsentId(CONSENT_ID))
            .thenReturn(CmsResponse.<Boolean>builder().payload(true).build());

        // When
        boolean actualResponse = xs2aAisConsentService.findAndTerminateOldConsentsByNewConsentId(CONSENT_ID);

        // Then
        assertThat(actualResponse).isTrue();
    }

    @Test
    void findAndTerminateOldConsentsByNewConsentId_false() {
        // Given
        when(consentServiceEncrypted.findAndTerminateOldConsentsByNewConsentId(CONSENT_ID))
            .thenReturn(CmsResponse.<Boolean>builder().payload(false).build());

        // When
        boolean actualResponse = xs2aAisConsentService.findAndTerminateOldConsentsByNewConsentId(CONSENT_ID);

        // Then
        assertThat(actualResponse).isFalse();
    }

    @Test
    void createAisConsentAuthorization_success() {
        // Given
        when(scaApproachResolver.resolveScaApproach())
            .thenReturn(SCA_APPROACH);
        when(aisConsentAuthorisationMapper.mapToAuthorisationRequest(SCA_STATUS, PSU_DATA, SCA_APPROACH, REDIRECT_URI, NOK_REDIRECT_URI))
            .thenReturn(AIS_CONSENT_AUTHORISATION_REQUEST);
        when(authorisationService.createAuthorisation(AIS_CONSENT_AUTHORISATION_REQUEST, CONSENT_ID, AuthorisationType.AIS))
            .thenReturn(Optional.of(buildCreateAisConsentAuthorizationResponse()));
        when(requestProviderService.getTppRedirectURI())
            .thenReturn(REDIRECT_URI);
        when(requestProviderService.getTppNokRedirectURI())
            .thenReturn(NOK_REDIRECT_URI);

        // When
        Optional<CreateAuthorisationResponse> actualResponse = xs2aAisConsentService.createAisConsentAuthorisation(CONSENT_ID, SCA_STATUS, PSU_DATA);

        // Then
        assertThat(actualResponse.isPresent()).isTrue();
        assertThat(actualResponse.get()).isEqualTo(buildCreateAisConsentAuthorizationResponse());
    }

    @Test
    void createAisConsentAuthorization_false() {
        // Given
        when(requestProviderService.getTppRedirectURI()).thenReturn("ok.uri");
        when(requestProviderService.getTppNokRedirectURI()).thenReturn("nok.uri");

        when(scaApproachResolver.resolveScaApproach()).thenReturn(SCA_APPROACH);
        CreateAuthorisationRequest request = new CreateAuthorisationRequest();
        when(aisConsentAuthorisationMapper.mapToAuthorisationRequest(SCA_STATUS, PSU_DATA, SCA_APPROACH, "ok.uri", "nok.uri"))
            .thenReturn(request);
        when(authorisationService.createAuthorisation(request, CONSENT_ID, AuthorisationType.AIS))
            .thenReturn(Optional.empty());

        // When
        Optional<CreateAuthorisationResponse> actualResponse = xs2aAisConsentService.createAisConsentAuthorisation(CONSENT_ID, SCA_STATUS, PSU_DATA);

        // Then
        assertThat(actualResponse.isPresent()).isFalse();
    }

    @Test
    void updateConsentStatus_shouldStoreConsentStatusInLoggingContext() throws WrongChecksumException {
        // Given
        when(consentServiceEncrypted.updateConsentStatusById(CONSENT_ID, CONSENT_STATUS))
            .thenReturn(CmsResponse.<Boolean>builder().payload(true).build());

        // When
        xs2aAisConsentService.updateConsentStatus(CONSENT_ID, CONSENT_STATUS);

        // Then
        verify(consentServiceEncrypted).updateConsentStatusById(CONSENT_ID, CONSENT_STATUS);
        verify(loggingContextService).storeConsentStatus(CONSENT_STATUS);
    }

    @Test
    void updateConsentStatus_WrongChecksumException() throws WrongChecksumException {
        // Given
        when(consentServiceEncrypted.updateConsentStatusById(CONSENT_ID, CONSENT_STATUS))
            .thenThrow(new WrongChecksumException());

        // When
        xs2aAisConsentService.updateConsentStatus(CONSENT_ID, CONSENT_STATUS);

        // Then
        verify(consentServiceEncrypted, times(1)).updateConsentStatusById(CONSENT_ID, CONSENT_STATUS);
        verify(loggingContextService, never()).storeConsentStatus(any(ConsentStatus.class));
    }

    @Test
    void updateConsentStatus_failure_shouldNotStoreConsentStatusInLoggingContext() throws WrongChecksumException {
        // Given
        when(consentServiceEncrypted.updateConsentStatusById(CONSENT_ID, CONSENT_STATUS))
            .thenReturn(CmsResponse.<Boolean>builder().payload(false).build());

        // When
        xs2aAisConsentService.updateConsentStatus(CONSENT_ID, CONSENT_STATUS);

        // Then
        verify(consentServiceEncrypted).updateConsentStatusById(CONSENT_ID, CONSENT_STATUS);
        verify(loggingContextService, never()).storeConsentStatus(any());
    }

    @Test
    void consentActionLog() throws WrongChecksumException {
        // Given
        ActionStatus actionStatus = ActionStatus.SUCCESS;
        ArgumentCaptor<AisConsentActionRequest> argumentCaptor = ArgumentCaptor.forClass(AisConsentActionRequest.class);

        // When
        xs2aAisConsentService.consentActionLog(TPP_ID, CONSENT_ID, actionStatus, REQUEST_URI, true, null, null);

        // Then
        verify(aisConsentServiceEncrypted).checkConsentAndSaveActionLog(argumentCaptor.capture());

        AisConsentActionRequest aisConsentActionRequest = argumentCaptor.getValue();
        assertThat(aisConsentActionRequest.getTppId()).isEqualTo(TPP_ID);
        assertThat(aisConsentActionRequest.getConsentId()).isEqualTo(CONSENT_ID);
        assertThat(aisConsentActionRequest.getActionStatus()).isEqualTo(actionStatus);
        assertThat(aisConsentActionRequest.getRequestUri()).isEqualTo(REQUEST_URI);
        assertThat(aisConsentActionRequest.isUpdateUsage()).isTrue();
    }

    @Test
    void consentActionLog_WrongChecksumException() throws WrongChecksumException {
        // Given
        ActionStatus actionStatus = ActionStatus.SUCCESS;
        ArgumentCaptor<AisConsentActionRequest> argumentCaptor = ArgumentCaptor.forClass(AisConsentActionRequest.class);
        when(aisConsentServiceEncrypted.checkConsentAndSaveActionLog(any(AisConsentActionRequest.class)))
            .thenThrow(WrongChecksumException.class);

        // When
        xs2aAisConsentService.consentActionLog(TPP_ID, CONSENT_ID, actionStatus, REQUEST_URI, true, null, null);

        // Then
        verify(aisConsentServiceEncrypted).checkConsentAndSaveActionLog(argumentCaptor.capture());

        AisConsentActionRequest aisConsentActionRequest = argumentCaptor.getValue();
        assertThat(aisConsentActionRequest.getTppId()).isEqualTo(TPP_ID);
        assertThat(aisConsentActionRequest.getConsentId()).isEqualTo(CONSENT_ID);
        assertThat(aisConsentActionRequest.getActionStatus()).isEqualTo(actionStatus);
        assertThat(aisConsentActionRequest.getRequestUri()).isEqualTo(REQUEST_URI);
        assertThat(aisConsentActionRequest.isUpdateUsage()).isTrue();
    }

    @Test
    void createConsentCheckInternalRequestId() throws WrongChecksumException {
        // Given
        ArgumentCaptor<CmsConsent> argumentCaptor = ArgumentCaptor.forClass(CmsConsent.class);
        when(frequencyPerDateCalculationService.getMinFrequencyPerDay(CREATE_CONSENT_REQ.getFrequencyPerDay()))
            .thenReturn(1);
        when(consentServiceEncrypted.createConsent(any()))
            .thenReturn(CmsResponse.<CmsCreateConsentResponse>builder().error(CmsError.TECHNICAL_ERROR).build());
        when(aisConsentMapper.mapToCmsConsent(CREATE_CONSENT_REQ, PSU_DATA, TPP_INFO, 1))
            .thenReturn(CMS_CONSENT);

        // When
        xs2aAisConsentService.createConsent(CREATE_CONSENT_REQ, PSU_DATA, TPP_INFO);

        // Then
        verify(consentServiceEncrypted).createConsent(argumentCaptor.capture());
    }

    @Test
    void updateConsentAuthorization() {
        // Given
        UpdateConsentPsuDataReq updateConsentPsuDataReq = new UpdateConsentPsuDataReq();
        updateConsentPsuDataReq.setAuthorizationId(AUTHORISATION_ID);
        UpdateAuthorisationRequest request = new UpdateAuthorisationRequest();

        when(aisConsentAuthorisationMapper.mapToAuthorisationRequest(updateConsentPsuDataReq))
            .thenReturn(request);

        // When
        xs2aAisConsentService.updateConsentAuthorisation(updateConsentPsuDataReq);

        // Then
        verify(authorisationService, times(1)).updateAuthorisation(request, AUTHORISATION_ID);
    }

    @Test
    void updateConsentAuthorization_nullValue() {
        // When
        xs2aAisConsentService.updateConsentAuthorisation(null);

        // Then
        verify(authorisationService, never()).updateAuthorisation(any(), any());
    }

    @Test
    void updateMultilevelScaRequired() throws WrongChecksumException {
        // When
        xs2aAisConsentService.updateMultilevelScaRequired(CONSENT_ID, true);

        // Then
        verify(consentServiceEncrypted, times(1)).updateMultilevelScaRequired(CONSENT_ID, true);
    }

    @Test
    void updateMultilevelScaRequired_WrongChecksumException() throws WrongChecksumException {
        // When
        doThrow(new WrongChecksumException()).when(consentServiceEncrypted).updateMultilevelScaRequired(CONSENT_ID, true);

        // Then
        xs2aAisConsentService.updateMultilevelScaRequired(CONSENT_ID, true);
        verify(consentServiceEncrypted, times(1)).updateMultilevelScaRequired(CONSENT_ID, true);
    }

    @Test
    void getAuthorisationScaStatus() {
        xs2aAisConsentService.getAuthorisationScaStatus(CONSENT_ID, AUTHORISATION_ID);
        verify(authorisationService, times(1)).getAuthorisationScaStatus(AUTHORISATION_ID, CONSENT_ID, AuthorisationType.AIS);
    }

    @Test
    void getAuthorisationSubResources() {
        xs2aAisConsentService.getAuthorisationSubResources(CONSENT_ID);
        verify(authorisationService, times(1)).getAuthorisationSubResources(CONSENT_ID, AuthorisationType.AIS);
    }

    @Test
    void updateAspspAccountAccess() throws WrongChecksumException {
        AccountAccess accountAccess = jsonReader.getObjectFromFile("json/aspect/account-access.json", AccountAccess.class);

        CmsConsent cmsConsent = new CmsConsent();
        when(aisConsentServiceEncrypted.updateAspspAccountAccess(CONSENT_ID, accountAccess))
            .thenReturn(CmsResponse.<CmsConsent>builder().payload(cmsConsent).build());
        when(aisConsentMapper.mapToAisConsent(cmsConsent)).thenReturn(aisConsent);

        CmsResponse<AisConsent> actual = xs2aAisConsentService.updateAspspAccountAccess(CONSENT_ID, accountAccess);

        assertFalse(actual.hasError());
        assertEquals(aisConsent, actual.getPayload());
    }

    @Test
    void updateAspspAccountAccess_checksumError() throws WrongChecksumException {
        AccountAccess accountAccess = jsonReader.getObjectFromFile("json/aspect/account-access.json", AccountAccess.class);

        when(aisConsentServiceEncrypted.updateAspspAccountAccess(CONSENT_ID, accountAccess))
            .thenThrow(new WrongChecksumException());

        CmsResponse<AisConsent> actual = xs2aAisConsentService.updateAspspAccountAccess(CONSENT_ID, accountAccess);

        assertTrue(actual.hasError());
        assertEquals(CmsError.CHECKSUM_ERROR, actual.getError());
    }

    @Test
    void updateAspspAccountAccess_updateError() throws WrongChecksumException {
        AccountAccess accountAccess = jsonReader.getObjectFromFile("json/aspect/account-access.json", AccountAccess.class);

        when(aisConsentServiceEncrypted.updateAspspAccountAccess(CONSENT_ID, accountAccess))
            .thenReturn(CmsResponse.<CmsConsent>builder().error(CmsError.TECHNICAL_ERROR).build());

        CmsResponse<AisConsent> actual = xs2aAisConsentService.updateAspspAccountAccess(CONSENT_ID, accountAccess);

        assertTrue(actual.hasError());
        assertEquals(CmsError.TECHNICAL_ERROR, actual.getError());
    }

    private static TppInfo buildTppInfo() {
        TppInfo tppInfo = new TppInfo();
        tppInfo.setAuthorisationNumber("registrationNumber");
        tppInfo.setTppName("tppName");
        tppInfo.setTppRoles(Collections.singletonList(TppRole.PISP));
        tppInfo.setAuthorityId("authorityId");
        return tppInfo;
    }

    private static CreateConsentReq buildCreateConsentReq() {
        CreateConsentReq createConsentReq = new CreateConsentReq();
        createConsentReq.setFrequencyPerDay(1);
        return createConsentReq;
    }

    private static CreateAuthorisationRequest buildAisConsentAuthorisationRequest() {
        CreateAuthorisationRequest consentAuthorization = new CreateAuthorisationRequest();
        consentAuthorization.setPsuData(PSU_DATA);
        consentAuthorization.setScaApproach(SCA_APPROACH);
        return consentAuthorization;
    }

    private static CreateAuthorisationResponse buildCreateAisConsentAuthorizationResponse() {
        return new CreateAuthorisationResponse(AUTHORISATION_ID, ScaStatus.RECEIVED, "", null);
    }

    private CmsConsent getCmsConsentWithNotifications() {
        CmsConsent cmsConsent = new CmsConsent();
        ConsentTppInformation consentTppInformation = new ConsentTppInformation();
        consentTppInformation.setTppNotificationSupportedModes(Collections.singletonList(NotificationSupportedMode.SCA));
        cmsConsent.setTppInformation(new ConsentTppInformation());
        return cmsConsent;
    }
}
