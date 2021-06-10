package uk.gov.hmcts.reform.em.hrs.service;


import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSharee;
import uk.gov.hmcts.reform.em.hrs.repository.ShareesRepository;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@Component
public class PermissionEvaluatorImpl implements PermissionEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionEvaluatorImpl.class);

    @Value("#{'${hrs.allowed-roles}'.split(',')}")
    List<String> allowedRoles;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private ShareesRepository shareesRepository;

    @Override
    public boolean hasPermission(@NotNull Authentication authentication,
                                 @NotNull Object targetDomainObject,
                                 @NotNull Object permissionString) {

        var jwtAuthenticationToken = (JwtAuthenticationToken) authentication;
        String token = "Bearer " + jwtAuthenticationToken.getToken().getTokenValue();

        var userInfo = securityService.getUserInfo(token);

        LOGGER.info("User with roles ({}) attempting to access recording", userInfo.getRoles());

        if (CollectionUtils.isNotEmpty(userInfo.getRoles())) {
            Optional<String> userRole = userInfo.getRoles().stream()
                .filter(role -> allowedRoles.contains(role))
                .findFirst();
            if (userRole.isPresent()) {
                LOGGER.info("User granted access with allowed role ({})", userRole);
                return true;
            }
        }

        if (targetDomainObject instanceof UUID) {
            var recordingId = (UUID) targetDomainObject;
            String shareeEmail = securityService.getUserEmail(token);
            LOGGER.info("User attempting to access recording with email ({})", shareeEmail);
            List<HearingRecordingSharee> sharedRecordings = shareesRepository.findByShareeEmail(shareeEmail);
            LOGGER.info(
                "recordings that are shared with the user: ({})",
                sharedRecordings.stream()
                    .map(recording -> recording.getHearingRecording().getCaseRef())
                    .collect(Collectors.toList())
            );
            if (CollectionUtils.isNotEmpty(sharedRecordings)) {
                Optional<HearingRecordingSharee> hearingRecording = sharedRecordings.stream()
                    .filter(hearingRecordingSharee -> hearingRecordingSharee.getHearingRecording().getId().equals(recordingId))
                    .findFirst();
                if (hearingRecording.isPresent()) {
                    LOGGER.info("User granted access through shared email ({})", shareeEmail);
                    return true;
                }
            }
        }


        return false;
    }

    @Override
    public boolean hasPermission(@NotNull Authentication authentication,
                                 @NotNull Serializable serializable,
                                 @NotNull String className,
                                 @NotNull Object permissions) {
        LOGGER.error("Should not be invoking this method");
        return false;
    }
}
