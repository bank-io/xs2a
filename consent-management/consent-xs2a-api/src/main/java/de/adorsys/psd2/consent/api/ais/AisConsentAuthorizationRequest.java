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

package de.adorsys.psd2.consent.api.ais;

import de.adorsys.psd2.xs2a.core.profile.ScaApproach;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.sca.ScaStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(description = "AIS consent authorization", value = "AisConsentAuthorization")
public class AisConsentAuthorizationRequest {

    @ApiModelProperty(value = "Corresponding PSU", required = true)
    private PsuIdData psuData;

    @ApiModelProperty(value = "The following code values are permitted 'received', 'psuIdentified', 'psuAuthenticated', 'scaMethodSelected', 'started', 'finalised' 'failed' 'exempted'.", required = true, example = "STARTED")
    private ScaStatus scaStatus;

    @ApiModelProperty(value = "An identification provided by the ASPSP for the later identification of the authentication method selection.")
    private String authenticationMethodId;

    @ApiModelProperty(value = "Password")
    private String password;

    @ApiModelProperty(value = "SCA authentication data")
    private String scaAuthenticationData;

    @ApiModelProperty(value = "SCA approach")
    private ScaApproach scaApproach;

    @ApiModelProperty(value="TPP redirect URI")
    private String tppRedirectURI;

    @ApiModelProperty(value="TPP NOK redirect URI")
    private String tppNokRedirectURI;
}
