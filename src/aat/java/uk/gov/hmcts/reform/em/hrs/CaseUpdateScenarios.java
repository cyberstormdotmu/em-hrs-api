package uk.gov.hmcts.reform.em.hrs;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Ignore;
import org.junit.Test;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;

import static uk.gov.hmcts.reform.em.hrs.testutil.ExtendedCcdHelper.HRS_TESTER;

public class CaseUpdateScenarios extends BaseTest {

    private static final String FOLDER = "functionaltest001";
    private static final String JURISDICTION_CODE = "FT";
    private static final String LOCATION_CODE = "0111";
    private static final String CASE_REF = "functionalTestFile71Mb";
    private static final String RECORDING_TIME = "2020-05-21-11.40.00.000";
    //functionaltest001/FT-0111-functionalTestFile71Mb_2020-05-21-11.40.00.000-UTC_0.mp4

    @Test
    public void testCcdCaseUpdate() {
        getFilenames(FOLDER)
            .then()
            .statusCode(200);

        JsonNode reqBody = createRecordingSegment(FOLDER, JURISDICTION_CODE, LOCATION_CODE, CASE_REF,
                                                  RECORDING_TIME, 1, "mp4");

        postRecordingSegment(reqBody)
            .then()
            .statusCode(202);
    }

    @Ignore
    @Test
    public void testDocumentShare() {
        CaseDetails caseDetails = searchForCase(CASE_REF).orElseThrow();

        shareRecording("sharee@email.com", caseDetails)
            .then()
            .statusCode(200);
    }

    @Ignore
    @Test
    public void testRecordingDownload() {
        CaseDetails caseDetails = searchForCase(CASE_REF).orElseThrow();

        downloadRecording(HRS_TESTER, caseDetails.getData())
            .then()
            .statusCode(200);
    }
}
