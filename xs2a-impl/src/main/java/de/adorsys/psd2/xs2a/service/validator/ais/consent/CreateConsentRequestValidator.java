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

package de.adorsys.psd2.xs2a.service.validator.ais.consent;

import de.adorsys.psd2.core.data.AccountAccess;
import de.adorsys.psd2.xs2a.core.ais.AccountAccessType;
import de.adorsys.psd2.xs2a.core.domain.TppMessageInformation;
import de.adorsys.psd2.xs2a.core.error.ErrorType;
import de.adorsys.psd2.xs2a.core.profile.AdditionalInformationAccess;
import de.adorsys.psd2.xs2a.domain.consent.CreateConsentReq;
import de.adorsys.psd2.xs2a.service.ScaApproachResolver;
import de.adorsys.psd2.xs2a.service.profile.AspspProfileServiceWrapper;
import de.adorsys.psd2.xs2a.service.validator.BusinessValidator;
import de.adorsys.psd2.xs2a.service.validator.PsuDataInInitialRequestValidator;
import de.adorsys.psd2.xs2a.service.validator.SupportedAccountReferenceValidator;
import de.adorsys.psd2.xs2a.service.validator.ValidationResult;
import de.adorsys.psd2.xs2a.service.validator.ais.consent.dto.CreateConsentRequestObject;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static de.adorsys.psd2.xs2a.core.ais.AccountAccessType.ALL_ACCOUNTS;
import static de.adorsys.psd2.xs2a.core.ais.AccountAccessType.ALL_ACCOUNTS_WITH_OWNER_NAME;
import static de.adorsys.psd2.xs2a.core.error.MessageErrorCode.*;
import static de.adorsys.psd2.xs2a.core.profile.ScaApproach.EMBEDDED;

/**
 * Validator to be used for validating create consent request according to some business rules
 */
@Component
@RequiredArgsConstructor
public class CreateConsentRequestValidator implements BusinessValidator<CreateConsentRequestObject> {

    private final AspspProfileServiceWrapper aspspProfileService;
    private final ScaApproachResolver scaApproachResolver;
    private final PsuDataInInitialRequestValidator psuDataInInitialRequestValidator;
    private final SupportedAccountReferenceValidator supportedAccountReferenceValidator;

    /**
     * Validates Create consent request according to:
     * <ul>
     * <li>the presence of PSU Data in the request if it's mandated by the profile</li>
     * <li>support of account reference types</li>
     * <li>support of global consent for All Psd2</li>
     * <li>support of bank offered consent</li>
     * <li>support of available account access</li>
     * <li>support of combined service indicator</li>
     * </ul>
     * If there are new consent requirements, this method has to be updated.
     *
     * @param requestObject create consent request object
     * @return ValidationResult instance, that contains boolean isValid, that shows if request is valid
     * and MessageError for invalid case
     */
    @NotNull
    @Override
    public ValidationResult validate(@NotNull CreateConsentRequestObject requestObject) {
        ValidationResult psuDataValidationResult = psuDataInInitialRequestValidator.validate(requestObject.getPsuIdData());
        if (psuDataValidationResult.isNotValid()) {
            return psuDataValidationResult;
        }

        CreateConsentReq request = requestObject.getCreateConsentReq();

        ValidationResult supportedAccountReferenceValidationResult = supportedAccountReferenceValidator.validate(request.getAccountReferences());
        if (supportedAccountReferenceValidationResult.isNotValid()) {
            return supportedAccountReferenceValidationResult;
        }

        if (isNotSupportedGlobalConsentForAllPsd2(request)) {
            return ValidationResult.invalid(ErrorType.AIS_400, SERVICE_INVALID_400_FOR_GLOBAL_CONSENT);
        }
        if (isNotSupportedBankOfferedConsent(request)) {
            return ValidationResult.invalid(ErrorType.AIS_400, SERVICE_INVALID_400);
        }
        if (isNotSupportedAvailableAccounts(request)) {
            return ValidationResult.invalid(ErrorType.AIS_400, SERVICE_INVALID_400);
        }
        if (isNotSupportedCombinedServiceIndicator(request)) {
            return ValidationResult.invalid(ErrorType.AIS_400, SESSIONS_NOT_SUPPORTED);
        }
        if (isNotSupportedAccountOwnerInformation(request)) {
            return ValidationResult.invalid(ErrorType.AIS_401, TppMessageInformation.buildWithCustomError(CONSENT_INVALID, "An explicit consent of ownerName is not supported."));
        }

        return ValidationResult.valid();
    }

    private boolean isNotSupportedGlobalConsentForAllPsd2(CreateConsentReq request) {
        return isConsentGlobal(request)
                   && !aspspProfileService.isGlobalConsentSupported();
    }

    private boolean isNotSupportedBankOfferedConsent(CreateConsentReq request) {
        if (isNotEmptyAccess(request.getAccess()) || Stream.of(request.getAvailableAccounts(), request.getAllPsd2(), request.getAvailableAccountsWithBalance()).anyMatch(EnumSet.of(ALL_ACCOUNTS, ALL_ACCOUNTS_WITH_OWNER_NAME)::contains)) {
            return false;
        }

        if (scaApproachResolver.resolveScaApproach() == EMBEDDED) {
            return true;
        }

        return !aspspProfileService.isBankOfferedConsentSupported();
    }

    private boolean isConsentGlobal(CreateConsentReq request) {
        return !isNotEmptyAccess(request.getAccess())
                   && EnumSet.of(ALL_ACCOUNTS, ALL_ACCOUNTS_WITH_OWNER_NAME).contains(request.getAllPsd2());
    }

    private boolean isNotEmptyAccess(AccountAccess access) {
        return Optional.ofNullable(access)
                   .map(AccountAccess::isNotEmpty)
                   .orElse(false);
    }

    private boolean isNotSupportedAvailableAccounts(CreateConsentReq request) {
        boolean isConsentWithoutAvailableAccounts = Stream.of(request.getAvailableAccounts(), request.getAvailableAccountsWithBalance())
                                                        .allMatch(Objects::isNull);

        if (isConsentWithoutAvailableAccounts) {
            return false;
        }

        return !aspspProfileService.isAvailableAccountsConsentSupported();
    }

    private boolean isNotSupportedCombinedServiceIndicator(CreateConsentReq request) {
        return request.isCombinedServiceIndicator()
                   && !aspspProfileService.isAisPisSessionsSupported();
    }

    private boolean isNotSupportedAccountOwnerInformation(CreateConsentReq request) {
        AccountAccess access = request.getAccess();

        AccountAccessType allAccountsWithOwnerName = ALL_ACCOUNTS_WITH_OWNER_NAME;
        boolean isConsentWithAdditionalInformation = Stream.of(isConsentWithAdditionalInformationAccess(access),
                                                               request.getAvailableAccounts() == allAccountsWithOwnerName,
                                                               request.getAvailableAccountsWithBalance() == allAccountsWithOwnerName,
                                                               request.getAllPsd2() == allAccountsWithOwnerName)
                                                         .anyMatch(BooleanUtils::isTrue);

        return isConsentWithAdditionalInformation && !aspspProfileService.isAccountOwnerInformationSupported();
    }

    private boolean isConsentWithAdditionalInformationAccess(AccountAccess access) {
        return Optional.ofNullable(access.getAdditionalInformationAccess())
                   .map(AdditionalInformationAccess::getOwnerName)
                   .isPresent();
    }
}
