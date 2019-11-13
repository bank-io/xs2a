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

package de.adorsys.psd2.xs2a.service.validator.pis.authorisation.initiation;

import de.adorsys.psd2.consent.api.pis.proto.PisCommonPaymentResponse;
import de.adorsys.psd2.xs2a.core.pis.TransactionStatus;
import de.adorsys.psd2.xs2a.service.RequestProviderService;
import de.adorsys.psd2.xs2a.service.validator.ValidationResult;
import de.adorsys.psd2.xs2a.service.validator.pis.AbstractPisValidator;
import de.adorsys.psd2.xs2a.service.validator.pis.CommonPaymentObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static de.adorsys.psd2.xs2a.core.error.MessageErrorCode.RESOURCE_EXPIRED_403;
import static de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType.PIS_403;

/**
 * Validator to be used for validating create PIS authorisation request according to some business rules
 */
@Slf4j
@Component
public class CreatePisAuthorisationValidator extends AbstractPisValidator<CommonPaymentObject> {

    public CreatePisAuthorisationValidator(RequestProviderService requestProviderService) {
        super(requestProviderService);
    }

    /**
     * Validates create PIS authorisation request by checking whether:
     * <ul>
     * <li>payment is not expired</li>
     * </ul>
     *
     * @param paymentObject payment information object
     * @return valid result if the payment is valid, invalid result with appropriate error otherwise
     */
    @Override
    protected ValidationResult executeBusinessValidation(CommonPaymentObject paymentObject) {
        PisCommonPaymentResponse pisCommonPaymentResponse = paymentObject.getPisCommonPaymentResponse();
        if (pisCommonPaymentResponse.getTransactionStatus() == TransactionStatus.RJCT) {
            log.info("InR-ID: [{}], X-Request-ID: [{}], Payment ID: [{}]. Creation of PIS authorisation has failed: payment has been rejected",
                     getRequestProviderService().getInternalRequestId(), getRequestProviderService().getRequestId(), pisCommonPaymentResponse.getExternalId());
            return ValidationResult.invalid(PIS_403, RESOURCE_EXPIRED_403);
        }

        return ValidationResult.valid();
    }
}
