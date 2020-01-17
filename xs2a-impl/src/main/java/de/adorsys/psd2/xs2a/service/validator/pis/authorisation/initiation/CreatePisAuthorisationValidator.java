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

package de.adorsys.psd2.xs2a.service.validator.pis.authorisation.initiation;

import de.adorsys.psd2.consent.api.pis.proto.PisCommonPaymentResponse;
import de.adorsys.psd2.xs2a.core.pis.TransactionStatus;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.service.validator.ValidationResult;
import de.adorsys.psd2.xs2a.service.validator.authorisation.AuthorisationPsuDataChecker;
import de.adorsys.psd2.xs2a.service.validator.authorisation.PisAuthorisationStatusChecker;
import de.adorsys.psd2.xs2a.service.validator.pis.AbstractPisValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

import static de.adorsys.psd2.xs2a.core.error.ErrorType.*;
import static de.adorsys.psd2.xs2a.core.error.MessageErrorCode.*;

/**
 * Validator to be used for validating create PIS authorisation request according to some business rules
 */
@Slf4j
@Component
public class CreatePisAuthorisationValidator extends AbstractPisValidator<CreatePisAuthorisationObject> {

    private final AuthorisationPsuDataChecker authorisationPsuDataChecker;
    private final PisAuthorisationStatusChecker pisAuthorisationStatusChecker;

    public CreatePisAuthorisationValidator(AuthorisationPsuDataChecker authorisationPsuDataChecker,
                                           PisAuthorisationStatusChecker pisAuthorisationStatusChecker) {
        this.authorisationPsuDataChecker = authorisationPsuDataChecker;
        this.pisAuthorisationStatusChecker = pisAuthorisationStatusChecker;
    }

    /**
     * Validates create PIS authorisation request by checking whether:
     * <ul>
     * <li>payment authorisation PSU data is the same as initial request PSU data</li>
     * <li>payment authorisation is already finalised for this payment and for this PSU ID</li>
     * <li>payment is not expired</li>
     * </ul>
     *
     * @param createPisAuthorisationObject create payment authorisation information object
     * @return valid result if the payment is valid, invalid result with appropriate error otherwise
     */
    @Override
    protected ValidationResult executeBusinessValidation(CreatePisAuthorisationObject createPisAuthorisationObject) {

        PsuIdData psuDataFromRequest = createPisAuthorisationObject.getPsuDataFromRequest();
        List<PsuIdData> psuDataFromDb = createPisAuthorisationObject.getPisCommonPaymentResponse().getPsuData();
        PisCommonPaymentResponse pisCommonPaymentResponse = createPisAuthorisationObject.getPisCommonPaymentResponse();

        if (authorisationPsuDataChecker.isPsuDataWrong(
            pisCommonPaymentResponse.isMultilevelScaRequired(),
            psuDataFromDb,
            psuDataFromRequest)) {

            return ValidationResult.invalid(PIS_401, PSU_CREDENTIALS_INVALID);
        }

        // If the authorisation for this payment ID and for this PSU ID has status FINALISED or EXEMPTED - return error.
        boolean isFinalised = pisAuthorisationStatusChecker.isFinalised(psuDataFromRequest, pisCommonPaymentResponse.getAuthorisations());

        if (isFinalised) {
            return ValidationResult.invalid(PIS_409, STATUS_INVALID);
        }

        if (pisCommonPaymentResponse.getTransactionStatus() == TransactionStatus.RJCT) {
            log.info("Payment ID: [{}]. Creation of PIS authorisation has failed: payment has been rejected", pisCommonPaymentResponse.getExternalId());
            return ValidationResult.invalid(PIS_403, RESOURCE_EXPIRED_403);
        }

        return ValidationResult.valid();
    }

}
