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

package de.adorsys.psd2.xs2a.service.validator.pis.payment;

import de.adorsys.psd2.consent.api.pis.proto.PisCommonPaymentResponse;
import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import de.adorsys.psd2.xs2a.core.tpp.TppInfo;
import de.adorsys.psd2.xs2a.domain.TppMessageInformation;
import de.adorsys.psd2.xs2a.domain.pis.PaymentInitiationParameters;
import de.adorsys.psd2.xs2a.exception.MessageError;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType;
import de.adorsys.psd2.xs2a.service.validator.GetCommonPaymentByIdResponseValidator;
import de.adorsys.psd2.xs2a.service.validator.PaymentTypeAndProductValidator;
import de.adorsys.psd2.xs2a.service.validator.ValidationResult;
import de.adorsys.psd2.xs2a.service.validator.tpp.PisTppInfoValidator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static de.adorsys.psd2.xs2a.domain.MessageErrorCode.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GetPaymentByIdValidatorTest {
    private static final PaymentType PAYMENT_TYPE = PaymentType.SINGLE;
    private static final PaymentType INVALID_PAYMENT_TYPE = PaymentType.BULK;
    private static final String PAYMENT_PRODUCT = "payment product";
    private static final String INVALID_PAYMENT_PRODUCT = "invalid payment product";
    private static final TppInfo TPP_INFO = buildTppInfo("authorisation number");
    private static final TppInfo INVALID_TPP_INFO = buildTppInfo("invalid authorisation number");

    private static final MessageError PAYMENT_PRODUCT_VALIDATION_ERROR =
        new MessageError(ErrorType.PIS_404, TppMessageInformation.of(PRODUCT_UNKNOWN));
    private static final MessageError TPP_VALIDATION_ERROR =
        new MessageError(ErrorType.PIS_401, TppMessageInformation.of(UNAUTHORIZED));
    private static final MessageError GET_COMMON_PAYMENT_VALIDATION_ERROR =
        new MessageError(ErrorType.PIS_405, TppMessageInformation.of(SERVICE_INVALID_405));
    private static final PaymentInitiationParameters CORRECT_PAYMENT_TYPE_AND_PRODUCT = buildCorrectPaymentTypeAndProduct();
    private static final PaymentInitiationParameters WRONG_PAYMENT_TYPE_AND_PRODUCT = buildWrongPaymentTypeAndProduct();

    @Mock
    private PisTppInfoValidator pisTppInfoValidator;

    @Mock
    private GetCommonPaymentByIdResponseValidator getCommonPaymentByIdResponseValidator;

    @Mock
    PaymentTypeAndProductValidator paymentProductAndTypeValidator;

    @InjectMocks
    private GetPaymentByIdValidator getPaymentByIdValidator;

    @Before
    public void setUp() {
        // Inject pisTppInfoValidator via setter
        getPaymentByIdValidator.setPisTppInfoValidator(pisTppInfoValidator, paymentProductAndTypeValidator);

        when(pisTppInfoValidator.validateTpp(TPP_INFO))
            .thenReturn(ValidationResult.valid());
        when(pisTppInfoValidator.validateTpp(INVALID_TPP_INFO))
            .thenReturn(ValidationResult.invalid(TPP_VALIDATION_ERROR));

        when(getCommonPaymentByIdResponseValidator.validateRequest(buildPisCommonPaymentResponse(TPP_INFO), PAYMENT_TYPE, PAYMENT_PRODUCT))
            .thenReturn(ValidationResult.valid());

        when(getCommonPaymentByIdResponseValidator.validateRequest(buildPisCommonPaymentResponse(TPP_INFO), INVALID_PAYMENT_TYPE, INVALID_PAYMENT_PRODUCT))
            .thenReturn(ValidationResult.invalid(GET_COMMON_PAYMENT_VALIDATION_ERROR));
        when(getCommonPaymentByIdResponseValidator.validateRequest(buildPisCommonPaymentResponse(INVALID_TPP_INFO), INVALID_PAYMENT_TYPE, INVALID_PAYMENT_PRODUCT))
            .thenReturn(ValidationResult.invalid(GET_COMMON_PAYMENT_VALIDATION_ERROR));
        when(paymentProductAndTypeValidator.validate(CORRECT_PAYMENT_TYPE_AND_PRODUCT))
            .thenReturn(ValidationResult.valid());
        when(paymentProductAndTypeValidator.validate(WRONG_PAYMENT_TYPE_AND_PRODUCT))
            .thenReturn(ValidationResult.invalid(PAYMENT_PRODUCT_VALIDATION_ERROR));
    }

    @Test
    public void validate_withValidPaymentObject_shouldReturnValid() {
        // Given
        PisCommonPaymentResponse commonPaymentResponse = buildPisCommonPaymentResponse(TPP_INFO);

        // When
        ValidationResult validationResult = getPaymentByIdValidator.validate(new GetPaymentByIdPO(commonPaymentResponse, PAYMENT_TYPE, PAYMENT_PRODUCT));

        // Then
        verify(pisTppInfoValidator).validateTpp(commonPaymentResponse.getTppInfo());
        verify(getCommonPaymentByIdResponseValidator).validateRequest(commonPaymentResponse, PAYMENT_TYPE, PAYMENT_PRODUCT);

        assertNotNull(validationResult);
        assertTrue(validationResult.isValid());
        assertNull(validationResult.getMessageError());
    }

    @Test
    public void validate_withInvalidTppInPayment_shouldReturnTppValidationError() {
        // Given
        PisCommonPaymentResponse commonPaymentResponse = buildPisCommonPaymentResponse(INVALID_TPP_INFO);

        // When
        ValidationResult validationResult = getPaymentByIdValidator.validate(new GetPaymentByIdPO(commonPaymentResponse, PAYMENT_TYPE, PAYMENT_PRODUCT));

        // Then
        verify(pisTppInfoValidator).validateTpp(commonPaymentResponse.getTppInfo());

        assertNotNull(validationResult);
        assertTrue(validationResult.isNotValid());
        assertEquals(TPP_VALIDATION_ERROR, validationResult.getMessageError());
    }

    @Test
    public void validate_withInvalidPaymentObject_shouldReturnGetCommonPaymentValidationError() {
        // Given
        PisCommonPaymentResponse commonPaymentResponse = buildPisCommonPaymentResponse(TPP_INFO);
        when(paymentProductAndTypeValidator.validate(WRONG_PAYMENT_TYPE_AND_PRODUCT))
            .thenReturn(ValidationResult.valid());

        // When
        ValidationResult validationResult = getPaymentByIdValidator.validate(new GetPaymentByIdPO(commonPaymentResponse, INVALID_PAYMENT_TYPE, INVALID_PAYMENT_PRODUCT));

        // Then
        assertNotNull(validationResult);
        assertTrue(validationResult.isNotValid());
        assertEquals(GET_COMMON_PAYMENT_VALIDATION_ERROR, validationResult.getMessageError());
    }

    @Test
    public void validate_withInvalidTppAndPaymentObject_shouldReturnTppValidationErrorFirst() {
        // Given
        PisCommonPaymentResponse commonPaymentResponse = buildPisCommonPaymentResponse(INVALID_TPP_INFO);

        // When
        ValidationResult validationResult = getPaymentByIdValidator.validate(new GetPaymentByIdPO(commonPaymentResponse, PAYMENT_TYPE, PAYMENT_PRODUCT));

        // Then
        verify(pisTppInfoValidator).validateTpp(commonPaymentResponse.getTppInfo());

        assertNotNull(validationResult);
        assertTrue(validationResult.isNotValid());
        assertEquals(TPP_VALIDATION_ERROR, validationResult.getMessageError());
    }

    private static TppInfo buildTppInfo(String authorisationNumber) {
        TppInfo tppInfo = new TppInfo();
        tppInfo.setAuthorisationNumber(authorisationNumber);
        return tppInfo;
    }

    private PisCommonPaymentResponse buildPisCommonPaymentResponse(TppInfo tppInfo) {
        PisCommonPaymentResponse pisCommonPaymentResponse = new PisCommonPaymentResponse();
        pisCommonPaymentResponse.setTppInfo(tppInfo);
        return pisCommonPaymentResponse;
    }

    private static PaymentInitiationParameters buildCorrectPaymentTypeAndProduct() {
        PaymentInitiationParameters parameters = new PaymentInitiationParameters();
        parameters.setPaymentType(PaymentType.SINGLE);
        parameters.setPaymentProduct(PAYMENT_PRODUCT);
        return parameters;
    }

    private static PaymentInitiationParameters buildWrongPaymentTypeAndProduct() {
        PaymentInitiationParameters parameters = new PaymentInitiationParameters();
        parameters.setPaymentType(PaymentType.BULK);
        parameters.setPaymentProduct(INVALID_PAYMENT_PRODUCT);
        return parameters;
    }

}
