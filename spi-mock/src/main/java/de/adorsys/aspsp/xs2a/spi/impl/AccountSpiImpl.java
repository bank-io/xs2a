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

package de.adorsys.aspsp.xs2a.spi.impl;

import de.adorsys.aspsp.xs2a.spi.config.RemoteSpiUrls;
import de.adorsys.aspsp.xs2a.spi.domain.ObjectHolder;
import de.adorsys.aspsp.xs2a.spi.domain.account.SpiAccountDetails;
import de.adorsys.aspsp.xs2a.spi.domain.account.SpiBalances;
import de.adorsys.aspsp.xs2a.spi.domain.account.SpiTransaction;
import de.adorsys.aspsp.xs2a.spi.service.AccountSpi;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;


@Component
@AllArgsConstructor
@Profile("mockspi")
public class AccountSpiImpl implements AccountSpi {
    private final RemoteSpiUrls remoteSpiUrls;
    private final RestTemplate restTemplate;

    /**
     * @param iban String representation of Account IBAN
     * @return List<SpiAccountDetails>
     * Queries ASPSP to (GET) List of AccountDetails by IBAN
     */
    @Override
    public List<SpiAccountDetails> readAccountDetailsByIban(String iban) {
        return Optional.ofNullable(restTemplate.exchange(
            remoteSpiUrls.getAccountDetailsByIban(), HttpMethod.GET, new HttpEntity<>(null), new ParameterizedTypeReference<List<SpiAccountDetails>>() {
            }, iban).getBody())
                   .orElse(Collections.emptyList());
    }

    /**
     * @param accountId String representation of ASPSP account identifier
     * @return List<SpiBalances>
     * Queries ASPSP to (GET) a list of balances of a sertain account by its primary id
     */
    @Override
    public List<SpiBalances> readBalances(String accountId) {
        return Optional.ofNullable(restTemplate.exchange(
            remoteSpiUrls.getBalancesByAccountId(), HttpMethod.GET, null, new ParameterizedTypeReference<List<SpiBalances>>() {
            }, accountId).getBody())
                   .orElse(Collections.emptyList());
    }

    /**
     * @param transaction Prepared at xs2a transaction object
     * @return String transactionId
     * Queries (POST) ASPSP to save a new Transaction, as a response receives a string representing the ASPSP primary identifier of saved transaction
     */
    @Override
    public String saveTransaction(SpiTransaction transaction) {
        return restTemplate.postForEntity(remoteSpiUrls.createTransaction(), transaction, String.class).getBody();
    }

    /**
     * @param accountId String representation of ASPSP account primary identifier
     * @param dateFrom  Date representing the beginning of the search period
     * @param dateTo    Date representing the ending of the search period
     * @return List<SpiTransaction>
     * Queries ASPSP to get List of transactions dependant on period and accountId
     */
    @Override
    public List<SpiTransaction> readTransactionsByPeriod(String accountId, Date dateFrom, Date dateTo) {
        Map<String, String> uriParams = new ObjectHolder<String, String>()
                                            .addValue("account-id", accountId)
                                            .getValues();

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(remoteSpiUrls.readTransactionsByPeriod())
                                           .queryParam("dateFrom", dateFrom.toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                                           .queryParam("dateTo", dateTo.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());

        return restTemplate.exchange(
            builder.buildAndExpand(uriParams).toUriString(), HttpMethod.GET, null, new ParameterizedTypeReference<List<SpiTransaction>>() {
            }).getBody();
    }

    /**
     * @param transactionId String representation of ASPSP primary identifier of transaction
     * @param accountId     String representation of ASPSP account primary identifier
     * @return SpiTransaction
     * Queries ASPSP to (GET) transaction by its primary identifier and account identifier
     */
    @Override
    public Optional<SpiTransaction> readTransactionsById(String transactionId, String accountId) {
        return Optional.ofNullable(restTemplate.getForObject(remoteSpiUrls.readTransactionById(), SpiTransaction.class, transactionId, accountId));
    }

    /**
     * @param accountId String representation of ASPSP account primary identifier
     * @return SpiAccountDetails
     * Queries ASPSP to (GET) AccountDetails by primary ASPSP account identifier
     */
    @Override
    public SpiAccountDetails readAccountDetails(String accountId) {
        return restTemplate.getForObject(remoteSpiUrls.getAccountDetailsById(), SpiAccountDetails.class, accountId);
    }

    /**
     * @param psuId String representing ASPSP`s primary identifier of PSU
     * @return List<SpiAccountDetails>
     * Queries ASPSP to (GET) a list of account details of a certain PSU by identifier
     */
    @Override
    public List<SpiAccountDetails> readAccountsByPsuId(String psuId) {
        return Optional.ofNullable(restTemplate.exchange(
            remoteSpiUrls.getAccountDetailsByPsuId(), HttpMethod.GET, null, new ParameterizedTypeReference<List<SpiAccountDetails>>() {
            }, psuId).getBody())
                   .orElse(Collections.emptyList());
    }

    /**
     * @param ibans a collection of Strings representing account IBANS
     * @return List<SpiAccountDetails>
     * Queries ASPSP to (GET) list of account details with certain account IBANS
     */
    @Override
    public List<SpiAccountDetails> readAccountDetailsByIbans(Collection<String> ibans) {
        return ibans.stream()
                   .map(this::readAccountDetailsByIban)
                   .flatMap(Collection::stream)
                   .collect(Collectors.toList());
    }
}
