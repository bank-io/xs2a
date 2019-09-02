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

package de.adorsys.psd2.xs2a.core.piis;

import de.adorsys.psd2.xs2a.core.consent.ConsentStatus;
import de.adorsys.psd2.xs2a.core.profile.AccountReference;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.tpp.TppInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PiisConsent {
    private String id;
    private boolean recurringIndicator;
    private OffsetDateTime requestDateTime;
    private LocalDate lastActionDate;
    private LocalDate expireDate;
    private PsuIdData psuData;
    private ConsentStatus consentStatus;
    private AccountReference account;
    private PiisConsentTppAccessType tppAccessType;
    // TODO: Remove the field in scope of https://git.adorsys.de/adorsys/xs2a/aspsp-xs2a/issues/970
    @Deprecated
    private int allowedFrequencyPerDay;
    private OffsetDateTime creationTimestamp;
    private String instanceId;
    private String cardNumber;
    private LocalDate cardExpiryDate;
    private String cardInformation;
    private String registrationInformation;
    private OffsetDateTime statusChangeTimestamp;
    private String tppAuthorisationNumber;
}
