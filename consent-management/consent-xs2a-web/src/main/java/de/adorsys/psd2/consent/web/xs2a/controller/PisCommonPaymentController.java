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

package de.adorsys.psd2.consent.web.xs2a.controller;

import de.adorsys.psd2.consent.api.CmsResponse;
import de.adorsys.psd2.consent.api.CmsScaMethod;
import de.adorsys.psd2.consent.api.authorisation.*;
import de.adorsys.psd2.consent.api.pis.CreatePisCommonPaymentResponse;
import de.adorsys.psd2.consent.api.pis.PisCommonPaymentDataStatusResponse;
import de.adorsys.psd2.consent.api.pis.proto.PisCommonPaymentResponse;
import de.adorsys.psd2.consent.api.pis.proto.PisPaymentInfo;
import de.adorsys.psd2.consent.api.service.AuthorisationServiceEncrypted;
import de.adorsys.psd2.consent.api.service.PisCommonPaymentServiceEncrypted;
import de.adorsys.psd2.consent.web.xs2a.config.InternalCmsXs2aApiTagName;
import de.adorsys.psd2.xs2a.core.authorisation.Authorisation;
import de.adorsys.psd2.xs2a.core.pis.TransactionStatus;
import de.adorsys.psd2.xs2a.core.profile.ScaApproach;
import de.adorsys.psd2.xs2a.core.sca.AuthorisationScaApproachResponse;
import de.adorsys.psd2.xs2a.core.sca.ScaStatus;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "api/v1/pis/common-payments")
@Api(value = "api/v1/pis/common-payments", tags = InternalCmsXs2aApiTagName.PIS_COMMON_PAYMENT)
public class PisCommonPaymentController {
    private final PisCommonPaymentServiceEncrypted pisCommonPaymentServiceEncrypted;
    private final AuthorisationServiceEncrypted authorisationServiceEncrypted;

    @PostMapping(path = "/")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK", response = CreatePisCommonPaymentResponse.class),
        @ApiResponse(code = 400, message = "Bad request")})
    public ResponseEntity<CreatePisCommonPaymentResponse> createCommonPayment(@RequestBody PisPaymentInfo request) {
        CmsResponse<CreatePisCommonPaymentResponse> response = pisCommonPaymentServiceEncrypted.createCommonPayment(request);

        if (response.hasError()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(response.getPayload(), HttpStatus.CREATED);
    }

    @GetMapping(path = "/{payment-id}/status")
    @ApiOperation(value = "")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK", response = PisCommonPaymentDataStatusResponse.class),
        @ApiResponse(code = 400, message = "Bad request")})
    public ResponseEntity<PisCommonPaymentDataStatusResponse> getPisCommonPaymentStatusById(
        @ApiParam(name = "payment-id",
            value = "The payment identification assigned to the created payment.",
            example = "bf489af6-a2cb-4b75-b71d-d66d58b934d7",
            required = true)
        @PathVariable("payment-id") String paymentId) {
        CmsResponse<TransactionStatus> response = pisCommonPaymentServiceEncrypted.getPisCommonPaymentStatusById(paymentId);

        if (response.hasError()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(new PisCommonPaymentDataStatusResponse(response.getPayload()), HttpStatus.OK);
    }

    @GetMapping(path = "/{payment-id}")
    @ApiOperation(value = "")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK", response = PisCommonPaymentResponse.class),
        @ApiResponse(code = 400, message = "Bad request")})
    public ResponseEntity<PisCommonPaymentResponse> getCommonPaymentById(
        @ApiParam(name = "payment-id",
            value = "The payment identification assigned to the created payment.",
            example = "bf489af6-a2cb-4b75-b71d-d66d58b934d7",
            required = true)
        @PathVariable("payment-id") String paymentId) {
        CmsResponse<PisCommonPaymentResponse> response = pisCommonPaymentServiceEncrypted.getCommonPaymentById(paymentId);

        if (response.hasError()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(response.getPayload(), HttpStatus.OK);
    }

    @PutMapping(path = "/{payment-id}/status/{status}")
    @ApiOperation(value = "")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 400, message = "Bad request")})
    public ResponseEntity<Void> updateCommonPaymentStatus(
        @ApiParam(name = "payment-id",
            value = "The payment identification assigned to the created payment.",
            example = "bf489af6-a2cb-4b75-b71d-d66d58b934d7",
            required = true)
        @PathVariable("payment-id") String paymentId,
        @ApiParam(value = "The following code values are permitted 'ACCC', 'ACCP', 'ACSC', 'ACSP', 'ACTC', 'ACWC', 'ACWP', 'PDNG', 'RJCT', 'RCVD', 'CANC', 'ACFC', 'PATC'. These values might be extended by ASPSP by more values.",
            allowableValues = "AcceptedSettlementCompletedCreditor, AcceptedCustomerProfile, AcceptedSettlementCompleted, AcceptedSettlementInProcess, AcceptedTechnicalValidation, AcceptedWithChange, AcceptedWithoutPosting, Received, Pending, Rejected, Canceled, AcceptedFundsChecked, PartiallyAcceptedTechnicalCorrect",
            required = true)
        @PathVariable("status") String status) {
        CmsResponse<Boolean> response = pisCommonPaymentServiceEncrypted.updateCommonPaymentStatusById(paymentId, TransactionStatus.getByValue(status));

        if (response.isSuccessful() && BooleanUtils.isTrue(response.getPayload())) {
            return new ResponseEntity<>(HttpStatus.OK);
        }

        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @PostMapping(path = "/{payment-id}/authorisations")
    @ApiOperation(value = "Create authorisation for given id.")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Created"),
        @ApiResponse(code = 404, message = "Not Found")})
    public ResponseEntity<CreateAuthorisationResponse> createAuthorisation(
        @ApiParam(name = "payment-id",
            value = "The payment identification assigned to the created authorisation.",
            example = "bf489af6-a2cb-4b75-b71d-d66d58b934d7",
            required = true)
        @PathVariable("payment-id") String paymentId,
        @RequestBody CreateAuthorisationRequest request) {
        CmsResponse<CreateAuthorisationResponse> response = authorisationServiceEncrypted.createAuthorisation(new PisAuthorisationParentHolder(paymentId), request);

        if (response.hasError()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(response.getPayload(), HttpStatus.CREATED);
    }

    @PostMapping(path = "/{payment-id}/cancellation-authorisations")
    @ApiOperation(value = "Create payment authorisation cancellation for given payment id.")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Created"),
        @ApiResponse(code = 404, message = "Not Found")})
    public ResponseEntity<CreateAuthorisationResponse> createAuthorisationCancellation(
        @ApiParam(name = "payment-id",
            value = "The payment identification of the related payment.",
            example = "bf489af6-a2cb-4b75-b71d-d66d58b934d7",
            required = true)
        @PathVariable("payment-id") String paymentId,
        @RequestBody CreateAuthorisationRequest request) {
        CmsResponse<CreateAuthorisationResponse> response = authorisationServiceEncrypted.createAuthorisation(new PisCancellationAuthorisationParentHolder(paymentId), request);

        if (response.hasError()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(response.getPayload(), HttpStatus.CREATED);
    }

    @PutMapping(path = "/authorisations/{authorisation-id}")
    @ApiOperation(value = "Update pis authorisation.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 404, message = "Not Found")})
    public ResponseEntity<Authorisation> updateAuthorisation(
        @ApiParam(name = "authorisation-id",
            value = "The authorisation identification assigned to the created authorisation.",
            example = "bf489af6-a2cb-4b75-b71d-d66d58b934d7",
            required = true)
        @PathVariable("authorisation-id") String authorisationId,
        @RequestBody UpdateAuthorisationRequest request) {
        CmsResponse<Authorisation> response = authorisationServiceEncrypted.updateAuthorisation(authorisationId, request);

        if (response.hasError()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(response.getPayload(), HttpStatus.OK);
    }

    @PutMapping(path = "authorisations/{authorisation-id}/status/{status}")
    @ApiOperation(value = "Update status for PIS authorisation.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 404, message = "Not Found")
    })
    public ResponseEntity<Void> updateAuthorisationStatus(
        @ApiParam(name = "authorisation-id",
            value = "The authorisation identification assigned to the created authorisation.",
            example = "bf489af6-a2cb-4b75-b71d-d66d58b934d7",
            required = true)
        @PathVariable("authorisation-id") String authorisationId,
        @ApiParam(name = "status",
            value = "The authorisation status.",
            example = "ScaStatus.FAILED",
            required = true)
        @PathVariable("status") String authorisationStatus) {
        CmsResponse<Boolean> response = authorisationServiceEncrypted.updateAuthorisationStatus(authorisationId, ScaStatus.fromValue(authorisationStatus));

        if (response.isSuccessful() && BooleanUtils.isTrue(response.getPayload())) {
            return new ResponseEntity<>(HttpStatus.OK);
        }

        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping(path = "/authorisations/{authorisation-id}")
    @ApiOperation(value = "Getting pis authorisation.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 404, message = "Not Found")})
    public ResponseEntity<Authorisation> getAuthorisation(
        @ApiParam(name = "authorisation-id",
            value = "The authorisation identification assigned to the created authorisation.",
            example = "bf489af6-a2cb-4b75-b71d-d66d58b934d7",
            required = true)
        @PathVariable("authorisation-id") String authorisationId) {
        CmsResponse<Authorisation> response = authorisationServiceEncrypted.getAuthorisationById(authorisationId);

        if (response.hasError()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(response.getPayload(), HttpStatus.OK);
    }

    @GetMapping(path = "/{payment-id}/authorisations/{authorisation-id}/status")
    @ApiOperation(value = "Gets SCA status of pis consent authorisation.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 404, message = "Not Found")})
    public ResponseEntity<ScaStatus> getAuthorisationScaStatus(
        @ApiParam(name = "payment-id",
            value = "Identification of the payment.",
            example = "bf489af6-a2cb-4b75-b71d-d66d58b934d7",
            required = true)
        @PathVariable("payment-id") String paymentId,
        @ApiParam(name = "authorisation-id",
            value = "The consent authorisation identification",
            example = "bf489af6-a2cb-4b75-b71d-d66d58b934d7",
            required = true)
        @PathVariable("authorisation-id") String authorisationId) {
        CmsResponse<ScaStatus> response = authorisationServiceEncrypted.getAuthorisationScaStatus(authorisationId, new PisAuthorisationParentHolder(paymentId));

        if (response.hasError()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(response.getPayload(), HttpStatus.OK);
    }

    @PutMapping(path = "/cancellation-authorisations/{authorisation-id}")
    @ApiOperation(value = "Update pis cancellation authorisation.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 404, message = "Not Found")})
    public ResponseEntity<Authorisation> updateCancellationAuthorisation(
        @ApiParam(name = "cancellation-id",
            value = "The cancellation authorisation identification assigned to the created cancellation authorisation.",
            example = "bf489af6-a2cb-4b75-b71d-d66d58b934d7",
            required = true)
        @PathVariable("authorisation-id") String authorisationId,
        @RequestBody UpdateAuthorisationRequest request) {
        CmsResponse<Authorisation> response = authorisationServiceEncrypted.updateAuthorisation(authorisationId, request);

        if (response.hasError()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(response.getPayload(), HttpStatus.OK);
    }

    @GetMapping(path = "/cancellation-authorisations/{authorisation-id}")
    @ApiOperation(value = "Getting pis cancellation authorisation.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 404, message = "Not Found")})
    public ResponseEntity<List<String>> getAuthorisationCancellation(
        @ApiParam(name = "cancellation-id",
            value = "The cancellation authorisation identification assigned to the created cancellation authorisation.",
            example = "bf489af6-a2cb-4b75-b71d-d66d58b934d7",
            required = true)
        @PathVariable("authorisation-id") String authorisationId) {
        CmsResponse<List<String>> response = authorisationServiceEncrypted.getAuthorisationsByParentId(new PisCancellationAuthorisationParentHolder(authorisationId));

        if (response.hasError()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(response.getPayload(), HttpStatus.OK);
    }

    @GetMapping(path = "/{payment-id}/cancellation-authorisations")
    @ApiOperation(value = "Gets list of payment cancellation authorisation IDs by payment ID")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 404, message = "Not Found")})
    public ResponseEntity<List<String>> getAuthorisationsCancellation(
        @ApiParam(name = "payment-id",
            value = "The payment identification of the related payment.",
            example = "bf489af6-a2cb-4b75-b71d-d66d58b934d7",
            required = true)
        @PathVariable("payment-id") String paymentId) {
        CmsResponse<List<String>> response = authorisationServiceEncrypted.getAuthorisationsByParentId(new PisCancellationAuthorisationParentHolder(paymentId));

        if (response.hasError()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(response.getPayload(), HttpStatus.OK);
    }

    @GetMapping(path = "/{payment-id}/cancellation-authorisations/{authorisation-id}/status")
    @ApiOperation(value = "Gets SCA status of pis consent cancellation authorisation.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 404, message = "Not Found")})
    public ResponseEntity<ScaStatus> getCancellationAuthorisationScaStatus(
        @ApiParam(name = "payment-id",
            value = "Identification of the payment.",
            example = "bf489af6-a2cb-4b75-b71d-d66d58b934d7",
            required = true)
        @PathVariable("payment-id") String paymentId,
        @ApiParam(name = "cancellation-id",
            value = "Identification of the consent cancellation authorisation",
            example = "bf489af6-a2cb-4b75-b71d-d66d58b934d7",
            required = true)
        @PathVariable("authorisation-id") String authorisationId) {
        CmsResponse<ScaStatus> response = authorisationServiceEncrypted.getAuthorisationScaStatus(authorisationId, new PisCancellationAuthorisationParentHolder(paymentId));

        if (response.hasError()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(response.getPayload(), HttpStatus.OK);
    }

    @GetMapping(path = "/{payment-id}/authorisations")
    @ApiOperation(value = "Gets list of payment authorisation IDs by payment ID")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 404, message = "Not Found")})
    public ResponseEntity<List<String>> getAuthorisations(
        @ApiParam(name = "payment-id",
            value = "The payment identification of the related payment.",
            example = "vOHy6fj2f5IgxHk-kTlhw6sZdTXbRE3bWsu2obq54beYOChP5NvRmfh06nrwumc2R01HygQenchEcdGOlU-U0A==_=_iR74m2PdNyE",
            required = true)
        @PathVariable("payment-id") String paymentId) {
        CmsResponse<List<String>> response = authorisationServiceEncrypted.getAuthorisationsByParentId(new PisAuthorisationParentHolder(paymentId));

        if (response.hasError()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(response.getPayload(), HttpStatus.OK);
    }

    @GetMapping(path = "/authorisations/{authorisation-id}/authentication-methods/{authentication-method-id}")
    @ApiOperation(value = "Checks if requested authentication method is decoupled")
    @ApiResponse(code = 200, message = "OK")
    public ResponseEntity<Boolean> isAuthenticationMethodDecoupled(
        @ApiParam(name = "authorisation-id",
            value = "Common payment authorisation identification",
            example = "bf489af6-a2cb-4b75-b71d-d66d58b934d7",
            required = true)
        @PathVariable("authorisation-id") String authorisationId,
        @ApiParam(name = "authentication-method-id",
            value = "Authentication method identification",
            example = "sms",
            required = true)
        @PathVariable("authentication-method-id") String authenticationMethodId) {
        CmsResponse<Boolean> response = authorisationServiceEncrypted.isAuthenticationMethodDecoupled(authorisationId, authenticationMethodId);
        return new ResponseEntity<>(response.isSuccessful() && response.getPayload(), HttpStatus.OK);
    }

    @PostMapping(path = "/authorisations/{authorisation-id}/authentication-methods")
    @ApiOperation(value = "Saves authentication methods in authorisation")
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "No Content"),
        @ApiResponse(code = 404, message = "Not Found")})
    public ResponseEntity<Void> saveAuthenticationMethods(
        @ApiParam(name = "authorisation-id",
            value = "The common payment authorisation identification.",
            example = "bf489af6-a2cb-4b75-b71d-d66d58b934d7",
            required = true)
        @PathVariable("authorisation-id") String authorisationId,
        @RequestBody List<CmsScaMethod> methods) {
        CmsResponse<Boolean> response = authorisationServiceEncrypted.saveAuthenticationMethods(authorisationId, methods);

        if (response.isSuccessful() && BooleanUtils.isTrue(response.getPayload())) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PutMapping(path = "/authorisations/{authorisation-id}/sca-approach/{sca-approach}")
    @ApiOperation(value = "Updates pis sca approach.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 404, message = "Not Found")})
    public ResponseEntity<Boolean> updateScaApproach(
        @ApiParam(name = "authorisation-id",
            value = "The authorisation identification assigned to the created authorisation.",
            example = "bf489af6-a2cb-4b75-b71d-d66d58b934d7",
            required = true)
        @PathVariable("authorisation-id") String authorisationId,
        @ApiParam(name = "sca-approach",
            value = "Chosen SCA approach.",
            example = "REDIRECT",
            required = true)
        @PathVariable("sca-approach") ScaApproach scaApproach) {
        CmsResponse<Boolean> response = authorisationServiceEncrypted.updateScaApproach(authorisationId, scaApproach);

        if (response.isSuccessful() && BooleanUtils.isTrue(response.getPayload())) {
            return new ResponseEntity<>(HttpStatus.OK);
        }

        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping(path = "/authorisations/{authorisation-id}/sca-approach")
    @ApiOperation(value = "Gets SCA approach of the payment initiation authorisation by its ID")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 404, message = "Not Found")})
    public ResponseEntity<AuthorisationScaApproachResponse> getAuthorisationScaApproach(
        @ApiParam(name = "authorisation-id",
            value = "Identification of the payment initiation authorisation.",
            example = "bf489af6-a2cb-4b75-b71d-d66d58b934d7",
            required = true)
        @PathVariable("authorisation-id") String authorisationId) {
        CmsResponse<AuthorisationScaApproachResponse> response = authorisationServiceEncrypted.getAuthorisationScaApproach(authorisationId);

        if (response.hasError()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(response.getPayload(), HttpStatus.OK);
    }

    @GetMapping(path = "/cancellation-authorisations/{authorisation-id}/sca-approach")
    @ApiOperation(value = "Gets SCA approach of the payment cancellation authorisation by its ID")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 404, message = "Not Found")})
    public ResponseEntity<AuthorisationScaApproachResponse> getCancellationAuthorisationScaApproach(
        @ApiParam(name = "authorisation-id",
            value = "Identification of the payment cancellation authorisation.",
            example = "bf489af6-a2cb-4b75-b71d-d66d58b934d7",
            required = true)
        @PathVariable("authorisation-id") String authorisationId) {
        CmsResponse<AuthorisationScaApproachResponse> response = authorisationServiceEncrypted.getAuthorisationScaApproach(authorisationId);

        if (response.hasError()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(response.getPayload(), HttpStatus.OK);
    }

    @PutMapping(path = "/{payment-id}/multilevel-sca")
    @ApiOperation(value = "Updates multilevel sca required by payment ID")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 404, message = "Bad Request")})
    public ResponseEntity<Boolean> updateMultilevelScaRequired(
        @ApiParam(name = "payment-id",
            value = "The payment identification of the related payment.",
            example = "bf489af6-a2cb-4b75-b71d-d66d58b934d7",
            required = true)
        @PathVariable(name = "payment-id")
            String paymentId,
        @ApiParam(name = "multilevel-sca", value = "Multilevel SCA.", example = "false")
        @RequestParam(value = "multilevel-sca", defaultValue = "false") boolean multilevelSca) {
        CmsResponse<Boolean> response = pisCommonPaymentServiceEncrypted.updateMultilevelSca(paymentId, multilevelSca);

        if (response.isSuccessful() && BooleanUtils.isTrue(response.getPayload())) {
            return new ResponseEntity<>(response.getPayload(), HttpStatus.OK);

        }

        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}
