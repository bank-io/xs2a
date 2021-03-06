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

package de.adorsys.psd2.core.data.piis.v1;

import de.adorsys.psd2.core.data.AccountAccess;
import de.adorsys.psd2.core.data.Consent;
import de.adorsys.psd2.xs2a.core.authorisation.AuthorisationTemplate;
import de.adorsys.psd2.xs2a.core.consent.ConsentStatus;
import de.adorsys.psd2.xs2a.core.consent.ConsentTppInformation;
import de.adorsys.psd2.xs2a.core.consent.ConsentType;
import de.adorsys.psd2.xs2a.core.profile.AccountReference;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import org.apache.commons.collections4.CollectionUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

public class PiisConsent extends Consent<PiisConsentData> {

    public PiisConsent() {
    }

    public PiisConsent(PiisConsentData consentData, String id, String internalRequestId, ConsentStatus consentStatus, boolean recurringIndicator,
                       LocalDate expireDate, LocalDate lastActionDate, OffsetDateTime creationTimestamp, ConsentTppInformation consentTppInformation,
                       List<PsuIdData> psuIdDataList, AccountAccess aspspAccountAccess) {

        super(consentData, id, internalRequestId, consentStatus, 0, recurringIndicator, false,
              null, expireDate, lastActionDate, creationTimestamp, null, consentTppInformation,
              new AuthorisationTemplate(), psuIdDataList, Collections.emptyList(), Collections.emptyMap(), AccountAccess.EMPTY_ACCESS, aspspAccountAccess);
    }

    @Override
    public ConsentType getConsentType() {
        return ConsentType.PIIS_ASPSP;
    }

    public AccountReference getAccountReference() {
        List<AccountReference> accounts = getAspspAccountAccesses().getAccounts();

        if (CollectionUtils.isNotEmpty(accounts)) {
            return accounts.get(0);
        }

        return null;
    }

    public PsuIdData getPsuIdData() {
        List<PsuIdData> psuIdDataList = getPsuIdDataList();

        if (CollectionUtils.isNotEmpty(psuIdDataList)) {
            return psuIdDataList.get(0);
        }

        return null;
    }
}
