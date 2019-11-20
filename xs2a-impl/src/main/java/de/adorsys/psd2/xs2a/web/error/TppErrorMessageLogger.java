package de.adorsys.psd2.xs2a.web.error;

import de.adorsys.psd2.mapper.Xs2aObjectMapper;
import de.adorsys.psd2.xs2a.core.error.MessageErrorCode;
import de.adorsys.psd2.xs2a.domain.TppMessageInformation;
import de.adorsys.psd2.xs2a.exception.MessageError;
import de.adorsys.psd2.xs2a.service.discovery.ServiceTypeDiscoveryService;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorMapperContainer;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ServiceType;
import de.adorsys.psd2.xs2a.web.filter.TppErrorMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TppErrorMessageLogger {
    private final ServiceTypeDiscoveryService serviceTypeDiscoveryService;
    private final ErrorMapperContainer errorMapperContainer;
    private final Xs2aObjectMapper xs2aObjectMapper;

    public void error(HttpServletResponse response, int status, TppErrorMessage tppErrorMessage) throws IOException {
        response.setStatus(status);
        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        ServiceType serviceType = serviceTypeDiscoveryService.getServiceType();
        MessageErrorCode messageErrorCode = tppErrorMessage.getCode();
        Optional<ErrorType> byServiceTypeAndErrorCode = ErrorType.getByServiceTypeAndErrorCode(serviceType, messageErrorCode.getCode());

        if( !byServiceTypeAndErrorCode.isPresent() ){
            throw new IllegalArgumentException( "ErrorCode is not correct for given service type." );
        }

        MessageError messageError = new MessageError(byServiceTypeAndErrorCode.get(), TppMessageInformation.of(tppErrorMessage.getCategory(), messageErrorCode));
        ErrorMapperContainer.ErrorBody errorBody = errorMapperContainer.getErrorBody(messageError);
        xs2aObjectMapper.writeValue(response.getWriter(), errorBody.getBody());
    }
}
