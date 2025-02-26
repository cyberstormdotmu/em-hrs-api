package uk.gov.hmcts.reform.em.hrs.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.idam.client.IdamClient;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import java.util.Map;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;

@Component
public class SecurityServiceImpl implements SecurityService {

    static final String DUMMY_NAME = "dummyName";
    static final String SERVICE_AUTH = "serviceauthorization";
    static final String USER_AUTH = "authorization";
    private static final String HRS_INGESTOR = "hrsIngestor";
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityServiceImpl.class);
    public static final String CLIENTIP = "x-azure-clientip";
    private final IdamClient idamClient;
    private final AuthTokenGenerator authTokenGenerator;
    private final AuthTokenValidator authTokenValidator;
    private final String systemUsername;
    private final String systemUserPassword;

    @Autowired
    public SecurityServiceImpl(final IdamClient idamClient,
                               final AuthTokenGenerator authTokenGenerator,
                               final AuthTokenValidator authTokenValidator,
                               final @Value("${idam.system-user.username}") String systemUsername,
                               final @Value("${idam.system-user.password}") String systemUserPassword) {
        this.idamClient = idamClient;
        this.authTokenGenerator = authTokenGenerator;
        this.authTokenValidator = authTokenValidator;
        this.systemUsername = systemUsername;
        this.systemUserPassword = systemUserPassword;
    }

    @Override
    public Map<String, String> getTokens() {
        final String token = getUserToken();
        return Map.of("user", token,
                      "userId", getUserId(token),
                      "service", authTokenGenerator.generate());
    }

    @Override
    public String getUserToken() {
        LOGGER.info("retrieving access token with these credentials ({}/{})",
                    systemUsername, systemUserPassword.substring(0, 4).concat("*****"));
        return idamClient.getAccessToken(systemUsername, systemUserPassword);
    }

    @Override
    public String getUserId() {
        return getUserId(getUserToken());
    }

    @Override
    public String getUserId(String userAuthorization) {
        return idamClient.getUserDetails(userAuthorization).getId();
    }

    @Override
    public String getUserEmail() {
        return getUserEmail(getUserToken());
    }

    @Override
    public String getUserEmail(String userAuthorization) {
        return idamClient.getUserDetails(userAuthorization).getEmail();
    }

    @Override
    public UserInfo getUserInfo(String jwtToken) {
        return idamClient.getUserInfo(jwtToken);
    }

    private String getServiceName(final String token) {
        return authTokenValidator.getServiceName(token);
    }

    @Override
    public String getCurrentlyAuthenticatedServiceName() {

        HttpServletRequest request = getCurrentRequest();
        if (Objects.isNull(request)) {
            LOGGER.warn("Using dummy serviceName because request is null");
            return DUMMY_NAME;
        }
        String s2sToken = request.getHeader(SERVICE_AUTH);
        if (StringUtils.isBlank(s2sToken)) {
            LOGGER.warn("Using dummy serviceName because token is blank");
            return DUMMY_NAME;
        }
        LOGGER.info("Using token ({}) to get serviceName", s2sToken.substring(0, 15));
        return getServiceName(s2sToken);
    }

    @Override
    public String getAuditUserEmail() {
        HttpServletRequest request = getCurrentRequest();
        if (Objects.isNull(request)) {
            return HRS_INGESTOR;
        }
        String jwt = request.getHeader(USER_AUTH);

        return getUserEmail(jwt);
    }

    @Override
    public String getClientIp() {
        HttpServletRequest request = getCurrentRequest();
        if (Objects.isNull(request)) {
            return null;
        }
        return request.getHeader(CLIENTIP);
    }

    private HttpServletRequest getCurrentRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            return ((ServletRequestAttributes) requestAttributes).getRequest();
        }
        return null;
    }

}
