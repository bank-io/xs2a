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

package de.adorsys.psd2.core.data.ais;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.adorsys.psd2.core.data.AccountAccess;
import de.adorsys.psd2.core.data.Consent;
import de.adorsys.psd2.xs2a.core.ais.AccountAccessType;
import de.adorsys.psd2.xs2a.core.authorisation.AccountConsentAuthorization;
import de.adorsys.psd2.xs2a.core.authorisation.AuthorisationTemplate;
import de.adorsys.psd2.xs2a.core.consent.AisConsentRequestType;
import de.adorsys.psd2.xs2a.core.consent.ConsentStatus;
import de.adorsys.psd2.xs2a.core.consent.ConsentTppInformation;
import de.adorsys.psd2.xs2a.core.consent.ConsentType;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Stream;

public class AisConsent extends Consent<AisConsentData> {

    public AisConsent() {
    }

    public AisConsent(AisConsentData consentData, String id, String internalRequestId, ConsentStatus consentStatus, Integer frequencyPerDay, boolean recurringIndicator, boolean multilevelScaRequired,
                      LocalDate validUntil, LocalDate expireDate, LocalDate lastActionDate, OffsetDateTime creationTimestamp, OffsetDateTime statusChangeTimestamp, ConsentTppInformation consentTppInformation,
                      AuthorisationTemplate authorisationTemplate, List<PsuIdData> psuIdDataList, List<AccountConsentAuthorization> authorisations, Map<String, Integer> usages, AccountAccess tppAccountAccess, AccountAccess aspspAccountAccess) {

        super(consentData, id, internalRequestId, consentStatus, frequencyPerDay, recurringIndicator, multilevelScaRequired,
              validUntil, expireDate, lastActionDate, creationTimestamp, statusChangeTimestamp, consentTppInformation,
              authorisationTemplate, psuIdDataList, authorisations, usages, tppAccountAccess, aspspAccountAccess);
    }

    @Override
    public ConsentType getConsentType() {
        return ConsentType.AIS;
    }

    @JsonIgnore
    public AccountAccess getAccess() {
        Optional<AccountAccessType> allPsd2Optional = Optional.ofNullable(getConsentData())
                                              .map(AisConsentData::getAllPsd2);

        if (allPsd2Optional.isPresent()) {
            return getTppAccountAccesses();
        }

        AccountAccess aspspAccountAccesses = getAspspAccountAccesses();
        if (aspspAccountAccesses.isNotEmpty(getConsentData())) {
            return aspspAccountAccesses;
        }

        return getTppAccountAccesses();
    }

    public boolean isWithBalance() {
        return CollectionUtils.isNotEmpty(getTppAccountAccesses().getBalances());
    }

    @JsonIgnore
    public boolean isOneAccessType() {
        return !isRecurringIndicator();
    }

    @JsonIgnore
    public boolean isGlobalConsent() {
        return getConsentRequestType() == AisConsentRequestType.GLOBAL;
    }

    @JsonIgnore
    public boolean isConsentForAllAvailableAccounts() {
        return getConsentRequestType() == AisConsentRequestType.ALL_AVAILABLE_ACCOUNTS;
    }

    @JsonIgnore
    public boolean isConsentForDedicatedAccounts() {
        return getConsentRequestType() == AisConsentRequestType.DEDICATED_ACCOUNTS;
    }

    public Optional<AccountConsentAuthorization> findAuthorisationInConsent(String authorisationId) {
        return getAuthorisations().stream()
                   .filter(auth -> auth.getId().equals(authorisationId))
                   .findFirst();
    }

    public boolean isConsentWithNotIbanAccount() {
        AccountAccess access = getAccess();
        if (access == null) {
            return false;
        }

        return Stream.of(access.getAccounts(), access.getBalances(), access.getTransactions())
                   .filter(Objects::nonNull)
                   .flatMap(Collection::stream)
                   .allMatch(acc -> StringUtils.isAllBlank(acc.getIban(), acc.getBban(), acc.getMsisdn()));
    }

    public boolean isConsentWithNotCardAccount() {
        AccountAccess access = getAccess();
        if (access == null) {
            return false;

        }

        return Stream.of(access.getAccounts(), access.getBalances(), access.getTransactions())
                   .filter(Objects::nonNull)
                   .flatMap(Collection::stream)
                   .allMatch(acc -> StringUtils.isAllBlank(acc.getMaskedPan(), acc.getPan()));
    }

    @JsonIgnore
    public boolean isExpired() {
        return getConsentStatus() == ConsentStatus.EXPIRED;
    }

    public Map<String, Integer> getUsageCounterMap() {
        return getUsages();
    }

    public AisConsentRequestType getAisConsentRequestType() {
        return getConsentRequestType();
    }

    @JsonIgnore
    public AisConsentRequestType getConsentRequestType() {
        AccountAccess usedAccess = getAccess();
        return getRequestType(getConsentData().getAllPsd2(),
                              getConsentData().getAvailableAccounts(),
                              getConsentData().getAvailableAccountsWithBalance(),
                              !usedAccess.isNotEmpty(getConsentData()));
    }

    private AisConsentRequestType getRequestType(AccountAccessType allPsd2,
                                                 AccountAccessType availableAccounts,
                                                 AccountAccessType availableAccountsWithBalance,
                                                 boolean isAccessesEmpty) {

        List<AccountAccessType> allAccountsType = Arrays.asList(AccountAccessType.ALL_ACCOUNTS, AccountAccessType.ALL_ACCOUNTS_WITH_OWNER_NAME);

        if (allAccountsType.contains(allPsd2)) {
            return AisConsentRequestType.GLOBAL;
        } else if (allAccountsType.contains(availableAccounts)) {
            return AisConsentRequestType.ALL_AVAILABLE_ACCOUNTS;
        } else if (allAccountsType.contains(availableAccountsWithBalance)) {
            return AisConsentRequestType.ALL_AVAILABLE_ACCOUNTS;
        } else if (isAccessesEmpty) {
            return AisConsentRequestType.BANK_OFFERED;
        }
        return AisConsentRequestType.DEDICATED_ACCOUNTS;
    }
}
