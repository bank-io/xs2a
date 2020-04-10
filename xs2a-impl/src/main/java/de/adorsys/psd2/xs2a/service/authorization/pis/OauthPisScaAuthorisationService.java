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

package de.adorsys.psd2.xs2a.service.authorization.pis;

import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import de.adorsys.psd2.xs2a.core.profile.ScaApproach;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.sca.ScaStatus;
import de.adorsys.psd2.xs2a.domain.authorisation.UpdateAuthorisationRequest;
import de.adorsys.psd2.xs2a.domain.consent.Xs2aAuthorisationSubResources;
import de.adorsys.psd2.xs2a.domain.consent.Xs2aCreatePisAuthorisationResponse;
import de.adorsys.psd2.xs2a.domain.consent.Xs2aCreatePisCancellationAuthorisationResponse;
import de.adorsys.psd2.xs2a.domain.consent.Xs2aPaymentCancellationAuthorisationSubResource;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aUpdatePisCommonPaymentPsuDataRequest;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aUpdatePisCommonPaymentPsuDataResponse;
import de.adorsys.psd2.xs2a.service.authorization.processor.model.AuthorisationProcessorResponse;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class OauthPisScaAuthorisationService implements PisScaAuthorisationService {
    @Override
    public Optional<Xs2aCreatePisAuthorisationResponse> createCommonPaymentAuthorisation(String paymentId, PaymentType paymentType, PsuIdData psuData) {
        return Optional.empty();
    }

    @Override
    public Xs2aUpdatePisCommonPaymentPsuDataResponse updateCommonPaymentPsuData(Xs2aUpdatePisCommonPaymentPsuDataRequest request) {
        return null;
    }

    @Override
    public void updateAuthorisation(UpdateAuthorisationRequest request, AuthorisationProcessorResponse response) {
    }

    @Override
    public void updateCancellationAuthorisation(UpdateAuthorisationRequest request, AuthorisationProcessorResponse response) {
    }

    @Override
    public Optional<Xs2aCreatePisCancellationAuthorisationResponse> createCommonPaymentCancellationAuthorisation(String paymentId, PaymentType paymentType, PsuIdData psuData) {
        return Optional.empty();
    }

    @Override
    public Optional<Xs2aPaymentCancellationAuthorisationSubResource> getCancellationAuthorisationSubResources(String paymentId) {
        return Optional.empty();
    }

    @Override
    public Xs2aUpdatePisCommonPaymentPsuDataResponse updateCommonPaymentCancellationPsuData(Xs2aUpdatePisCommonPaymentPsuDataRequest request) {
        return null;
    }

    @Override
    public Optional<Xs2aAuthorisationSubResources> getAuthorisationSubResources(String paymentId) {
        return Optional.empty();
    }

    @Override
    public Optional<ScaStatus> getAuthorisationScaStatus(String paymentId, String authorisationId) {
        return Optional.empty();
    }

    @Override
    public Optional<ScaStatus> getCancellationAuthorisationScaStatus(String paymentId, String authorisationId) {
        return Optional.empty();
    }

    @Override
    public ScaApproach getScaApproachServiceType() {
        return ScaApproach.OAUTH;
    }
}
