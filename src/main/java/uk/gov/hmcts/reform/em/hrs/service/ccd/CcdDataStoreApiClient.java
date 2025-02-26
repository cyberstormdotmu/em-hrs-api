package uk.gov.hmcts.reform.em.hrs.service.ccd;

import com.microsoft.applicationinsights.core.dependencies.google.gson.Gson;
import com.microsoft.applicationinsights.core.dependencies.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.exception.CcdUploadException;
import uk.gov.hmcts.reform.em.hrs.service.SecurityService;

import java.util.Map;
import java.util.UUID;

@Service
public class CcdDataStoreApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CcdDataStoreApiClient.class);
    private static final String JURISDICTION = "HRS";
    private static final String SERVICE = "service";
    private static final String USER = "user";
    private static final String USER_ID = "userId";
    private static final String CASE_TYPE = "HearingRecordings";
    private static final String EVENT_CREATE_CASE = "createCase";
    private static final String EVENT_MANAGE_FILES = "manageFiles";
    private final SecurityService securityService;
    private final CaseDataContentCreator caseDataCreator;
    private final CoreCaseDataApi coreCaseDataApi;

    public CcdDataStoreApiClient(SecurityService securityService,
                                 CaseDataContentCreator caseDataCreator,
                                 CoreCaseDataApi coreCaseDataApi) {
        this.securityService = securityService;
        this.caseDataCreator = caseDataCreator;
        this.coreCaseDataApi = coreCaseDataApi;
    }

    public Long createCase(final UUID recordingId, final HearingRecordingDto hearingRecordingDto) {
        Map<String, String> tokens = securityService.getTokens();

        StartEventResponse startEventResponse =
            coreCaseDataApi.startCase(tokens.get(USER), tokens.get(SERVICE), CASE_TYPE, EVENT_CREATE_CASE);

        CaseDataContent caseData = CaseDataContent.builder()
            .event(Event.builder().id(startEventResponse.getEventId()).build())
            .eventToken(startEventResponse.getToken())
            .data(caseDataCreator.createCaseStartData(hearingRecordingDto, recordingId))
            .build();

        CaseDetails caseDetails = coreCaseDataApi
            .submitForCaseworker(tokens.get(USER), tokens.get(SERVICE), tokens.get(USER_ID),
                                 JURISDICTION, CASE_TYPE, false, caseData
            );

        LOGGER.info("created a new case({}) for recording ({})",
                    caseDetails.getId(), hearingRecordingDto.getRecordingRef()
        );
        return caseDetails.getId();
    }


    public synchronized Long updateCaseData(final Long caseId, final UUID recordingId,
                                            final HearingRecordingDto hearingRecordingDto) {
        Map<String, String> tokens = securityService.getTokens();

        StartEventResponse startEventResponse = coreCaseDataApi.startEvent(tokens.get(USER), tokens.get(SERVICE),
                                                                           caseId.toString(), EVENT_MANAGE_FILES
        );

        CaseDataContent caseData = CaseDataContent.builder()
            .event(Event.builder().id(startEventResponse.getEventId()).build())
            .eventToken(startEventResponse.getToken())
            .data(caseDataCreator.createCaseUpdateData(
                startEventResponse.getCaseDetails().getData(), recordingId, hearingRecordingDto)
            ).build();


        LOGGER.info(
            "updating ccd case (id {}) with new recording (ref {})",
            caseId,
            hearingRecordingDto.getRecordingRef()
        );

        try {
            CaseDetails caseDetails =
                coreCaseDataApi.submitEventForCaseWorker(tokens.get(USER), tokens.get(SERVICE), tokens.get(USER_ID),
                                                         JURISDICTION, CASE_TYPE, caseId.toString(), false, caseData
                );

            return caseDetails.getId();
        } catch (Exception e) {
            //CCD has rejected, so log payload to assist with debugging (no sensitive information is exposed)
            String caseReference = caseData.getCaseReference();
            Event event = caseData.getEvent();
            String eventDescription = event.getDescription();
            String eventId = event.getId();
            String eventSummary = event.getSummary();

            LOGGER.info(
                "caseReference: {}, eventId: {}, eventDescription: {}, eventSummary: {}",
                caseReference,
                eventId,
                eventDescription,
                eventSummary
            );
            Object jsonData = caseData.getData();
            LOGGER.info("caseData Raw: " + jsonData.toString());
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonOutput = gson.toJson(jsonData);
            LOGGER.info("caseData Pretty: " + jsonOutput);


            throw new CcdUploadException("Error Uploading Segment", e);
        }

    }
}


