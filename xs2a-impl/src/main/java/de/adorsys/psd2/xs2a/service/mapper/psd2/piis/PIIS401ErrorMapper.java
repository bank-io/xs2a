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

package de.adorsys.psd2.xs2a.service.mapper.psd2.piis;

import de.adorsys.psd2.model.Error401NGPIIS;
import de.adorsys.psd2.model.TppMessage401PIIS;
import de.adorsys.psd2.model.TppMessageCategory;
import de.adorsys.psd2.xs2a.core.domain.TppMessageInformation;
import de.adorsys.psd2.xs2a.core.error.MessageError;
import de.adorsys.psd2.xs2a.service.mapper.psd2.Psd2ErrorMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PIIS401ErrorMapper extends Psd2ErrorMapper<MessageError, Error401NGPIIS> {

    @Override
    public Function<MessageError, Error401NGPIIS> getMapper() {
        return this::mapToPsd2Error;
    }

    @Override
    public HttpStatus getErrorStatus() {
        return HttpStatus.UNAUTHORIZED;
    }

    private Error401NGPIIS mapToPsd2Error(MessageError messageError) {
        return new Error401NGPIIS().tppMessages(mapToTppMessage401PIIS(messageError.getTppMessages()));
    }

    private List<TppMessage401PIIS> mapToTppMessage401PIIS(Set<TppMessageInformation> tppMessages) {
        return tppMessages.stream()
                   .map(m -> new TppMessage401PIIS()
                                 .category(TppMessageCategory.fromValue(m.getCategory().name()))
                                 .code(m.getMessageErrorCode().getName())
                                 .path(m.getPath())
                                 .text(getErrorText(m))
                   ).collect(Collectors.toList());
    }
}
