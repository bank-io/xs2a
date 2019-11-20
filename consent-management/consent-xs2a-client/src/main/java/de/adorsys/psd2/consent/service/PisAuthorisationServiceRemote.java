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

package de.adorsys.psd2.consent.service;

import de.adorsys.psd2.consent.api.CmsScaMethod;
import de.adorsys.psd2.consent.api.pis.authorisation.*;
import de.adorsys.psd2.consent.api.service.PisAuthorisationServiceEncrypted;
import de.adorsys.psd2.consent.config.CmsRestException;
import de.adorsys.psd2.consent.config.PisCommonPaymentRemoteUrls;
import de.adorsys.psd2.xs2a.core.pis.PaymentAuthorisationType;
import de.adorsys.psd2.xs2a.core.profile.ScaApproach;
import de.adorsys.psd2.xs2a.core.sca.AuthorisationScaApproachResponse;
import de.adorsys.psd2.xs2a.core.sca.ScaStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PisAuthorisationServiceRemote implements PisAuthorisationServiceEncrypted {
    @Qualifier("consentRestTemplate")
    private final RestTemplate consentRestTemplate;
    private final PisCommonPaymentRemoteUrls remotePisCommonPaymentUrls;

    @Override
    public Optional<CreatePisAuthorisationResponse> createAuthorization(String paymentId, CreatePisAuthorisationRequest request) {
        try {
            return Optional.ofNullable(consentRestTemplate.postForEntity(remotePisCommonPaymentUrls.createPisAuthorisation(),
                                                                         request, CreatePisAuthorisationResponse.class, paymentId))
                       .map(ResponseEntity::getBody);
        } catch (CmsRestException cmsRestException) {
            log.warn("No authorisation was created for the paymentId {}", paymentId);
            return Optional.empty();
        }
    }

    @Override
    public Optional<CreatePisAuthorisationResponse> createAuthorizationCancellation(String paymentId, CreatePisAuthorisationRequest request) {
        try {
            return Optional.ofNullable(consentRestTemplate.postForEntity(remotePisCommonPaymentUrls.createPisAuthorisationCancellation(), request, CreatePisAuthorisationResponse.class, paymentId))
                       .map(ResponseEntity::getBody);
        } catch (CmsRestException cmsRestException) {
            log.warn("No cancellation authorisation was created for the paymentId {}", paymentId);
            return Optional.empty();
        }
    }

    @Override
    public Optional<UpdatePisCommonPaymentPsuDataResponse> updatePisAuthorisation(String authorisationId, UpdatePisCommonPaymentPsuDataRequest request) {
        return Optional.ofNullable(consentRestTemplate.exchange(remotePisCommonPaymentUrls.updatePisAuthorisation(), HttpMethod.PUT, new HttpEntity<>(request),
                                                                UpdatePisCommonPaymentPsuDataResponse.class, request.getAuthorizationId()))
                   .map(ResponseEntity::getBody);
    }

    @Override
    public boolean updatePisAuthorisationStatus(String authorisationId, ScaStatus scaStatus) {
        try {
            consentRestTemplate.put(remotePisCommonPaymentUrls.updatePisAuthorisationStatus(), null, authorisationId, scaStatus.getValue());
            return true;
        } catch (CmsRestException cmsRestException) {
            log.info("Couldn't update authorisation status by authorisationId {}", authorisationId);
        }
        return false;
    }

    @Override
    public Optional<UpdatePisCommonPaymentPsuDataResponse> updatePisCancellationAuthorisation(String authorisationId, UpdatePisCommonPaymentPsuDataRequest request) {
        return Optional.ofNullable(consentRestTemplate.exchange(remotePisCommonPaymentUrls.updatePisCancellationAuthorisation(), HttpMethod.PUT, new HttpEntity<>(request),
                                                                UpdatePisCommonPaymentPsuDataResponse.class, request.getAuthorizationId()))
                   .map(ResponseEntity::getBody);
    }

    @Override
    public Optional<GetPisAuthorisationResponse> getPisAuthorisationById(String authorizationId) {
        try {
            return Optional.ofNullable(consentRestTemplate.exchange(remotePisCommonPaymentUrls.getPisAuthorisationById(), HttpMethod.GET, null, GetPisAuthorisationResponse.class, authorizationId))
                       .map(ResponseEntity::getBody);
        } catch (CmsRestException cmsRestException) {
            log.info("Authorisation ID: [{}]. No initiation authorisation could be found by given authorisation ID", authorizationId);
        }

        return Optional.empty();
    }

    @Override
    public Optional<GetPisAuthorisationResponse> getPisCancellationAuthorisationById(String cancellationId) {
        try {
            return Optional.ofNullable(consentRestTemplate.exchange(remotePisCommonPaymentUrls.getPisCancellationAuthorisationById(), HttpMethod.GET, null, GetPisAuthorisationResponse.class, cancellationId))
                       .map(ResponseEntity::getBody);
        } catch (CmsRestException cmsRestException) {
            log.info("Authorisation ID: [{}]. No cancellation authorisation could be found by given cancellation ID", cancellationId);
        }

        return Optional.empty();
    }

    @Override
    public Optional<List<String>> getAuthorisationsByPaymentId(String paymentId, PaymentAuthorisationType authorisationType) {
        String url = getAuthorisationSubResourcesUrl(authorisationType);
        try {
            return Optional.ofNullable(consentRestTemplate.exchange(url, HttpMethod.GET, null,
                                                                    new ParameterizedTypeReference<List<String>>() {
                                                                    }, paymentId))
                       .map(ResponseEntity::getBody);
        } catch (CmsRestException cmsRestException) {
            log.warn("No authorisation found by paymentId {}", paymentId);
        }
        return Optional.empty();
    }

    @Override
    public Optional<ScaStatus> getAuthorisationScaStatus(String paymentId, String authorisationId, PaymentAuthorisationType authorisationType) {
        String url = getAuthorisationScaStatusUrl(authorisationType);
        try {
            return Optional.ofNullable(consentRestTemplate.getForEntity(url, ScaStatus.class,
                                                                        paymentId, authorisationId))
                       .map(ResponseEntity::getBody);
        } catch (CmsRestException cmsRestException) {
            log.warn("Couldn't get authorisation SCA Status by paymentId {} and authorisationId {}", paymentId, authorisationId);
        }
        return Optional.empty();
    }

    @Override
    public boolean isAuthenticationMethodDecoupled(String authorisationId, String authenticationMethodId) {
        return consentRestTemplate.getForEntity(remotePisCommonPaymentUrls.isAuthenticationMethodDecoupled(), Boolean.class, authorisationId, authenticationMethodId)
                   .getBody();
    }

    @Override
    public boolean saveAuthenticationMethods(String authorisationId, List<CmsScaMethod> methods) {
        try {
            ResponseEntity<Void> responseEntity = consentRestTemplate.exchange(remotePisCommonPaymentUrls.saveAuthenticationMethods(), HttpMethod.POST, new HttpEntity<>(methods), Void.class, authorisationId);

            if (responseEntity.getStatusCode() == HttpStatus.NO_CONTENT) {
                return true;
            }
        } catch (CmsRestException cmsRestException) {
            log.warn("Couldn't save authentication methods {} by authorisationId {}", methods, authorisationId);
        }

        return false;
    }

    @Override
    public boolean updateScaApproach(String authorisationId, ScaApproach scaApproach) {
        return consentRestTemplate.exchange(remotePisCommonPaymentUrls.updateScaApproach(), HttpMethod.PUT,
                                            null, Boolean.class, authorisationId, scaApproach)
                   .getBody();
    }

    @Override
    public Optional<AuthorisationScaApproachResponse> getAuthorisationScaApproach(String authorisationId, PaymentAuthorisationType authorisationType) {
        String url = getAuthorisationScaApproachUrl(authorisationType);

        try {
            ResponseEntity<AuthorisationScaApproachResponse> request = consentRestTemplate.getForEntity(
                url, AuthorisationScaApproachResponse.class, authorisationId);
            return Optional.ofNullable(request.getBody());
        } catch (CmsRestException cmsRestException) {
            log.warn("Couldn't get authorisation SCA Approach by authorisationId {}", authorisationId);
        }

        return Optional.empty();
    }

    private String getAuthorisationSubResourcesUrl(PaymentAuthorisationType authorisationType) {
        switch (authorisationType) {
            case CREATED:
                return remotePisCommonPaymentUrls.getAuthorisationSubResources();
            case CANCELLED:
                return remotePisCommonPaymentUrls.getCancellationAuthorisationSubResources();
            default:
                log.error("Unknown payment authorisation type {}", authorisationType);
                throw new IllegalArgumentException("Unknown payment authorisation type " + authorisationType);
        }
    }

    private String getAuthorisationScaApproachUrl(PaymentAuthorisationType authorisationType) {
        switch (authorisationType) {
            case CREATED:
                return remotePisCommonPaymentUrls.getAuthorisationScaApproach();
            case CANCELLED:
                return remotePisCommonPaymentUrls.getCancellationAuthorisationScaApproach();
            default:
                log.error("Unknown payment authorisation type {}", authorisationType);
                throw new IllegalArgumentException("Unknown payment authorisation type " + authorisationType);
        }
    }

    private String getAuthorisationScaStatusUrl(PaymentAuthorisationType authorisationType) {
        switch (authorisationType) {
            case CREATED:
                return remotePisCommonPaymentUrls.getAuthorisationScaStatus();
            case CANCELLED:
                return remotePisCommonPaymentUrls.getCancellationAuthorisationScaStatus();
            default:
                log.error("Unknown payment authorisation type {}", authorisationType);
                throw new IllegalArgumentException("Unknown payment authorisation type " + authorisationType);
        }
    }
}