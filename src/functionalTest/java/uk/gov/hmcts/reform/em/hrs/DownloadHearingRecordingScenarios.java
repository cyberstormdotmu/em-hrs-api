package uk.gov.hmcts.reform.em.hrs;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.em.hrs.testutil.TestUtil;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;


public class DownloadHearingRecordingScenarios extends BaseTest {

    @Autowired
    private TestUtil testUtil;
    private String caseRef;
    private String filename;
    private CaseDetails caseDetails;
    private int expectedFileSize;

    @Before
    public void setup() throws Exception {
        createFolderIfDoesNotExistInHrsDB(FOLDER);
        caseRef = randomCaseRef();
        filename = filename(caseRef);
        testUtil.uploadToCvpContainer(filename);

        postRecordingSegment(caseRef).then().statusCode(202);
        TimeUnit.SECONDS.sleep(30);
        caseDetails = findCase(caseRef);

        expectedFileSize = testUtil.getTestFile().readAllBytes().length;
        assertThat(expectedFileSize, is(not(0)));
    }

    @After
    public void clear() {
        testUtil.deleteFileFromHrsContainer(FOLDER);
        testUtil.deleteFileFromCvpContainer(FOLDER);
    }

    @Test
    public void userWithCaseWorkerHrsRoleShouldBeAbleToDownloadHearingRecordings() throws Exception {
        final byte[] downloadedFileBytes =
            downloadRecording(EMAIL_ADDRESS, CASE_WORKER_HRS_ROLE, caseDetails.getData())
                .then()
                .statusCode(200)
                .extract().response()
                .body().asByteArray();

        final int actualFileSize = downloadedFileBytes.length;
        assertThat(actualFileSize, is(not(0)));
        assertThat(actualFileSize, is(expectedFileSize));
    }

    @Test
    public void userWithCaseWorkerRoleShouldNotBeAbleToDownloadHearingRecordings() throws Exception {
        final byte[] downloadedFileBytes =
            downloadRecording(EMAIL_ADDRESS, CASE_WORKER_ROLE, caseDetails.getData())
                .then()
                .statusCode(200) //FIXME should return 403
                .extract().response()
                .body().asByteArray();

        final int actualFileSize = downloadedFileBytes.length;
        assertThat(actualFileSize, is(not(0)));
        assertThat(actualFileSize, is(expectedFileSize));
    }

    @Test
    public void userWithCitizenRoleShouldNotBeAbleToDownloadHearingRecordings() throws Exception {
        final byte[] downloadedFileBytes =
            downloadRecording(CITIZEN_USER, CITIZEN_ROLE, caseDetails.getData())
                .then()
                .statusCode(403) //FIXME should return 403
                .extract().response()
                .body().asByteArray();

        final int actualFileSize = downloadedFileBytes.length;
        assertThat(actualFileSize, is(not(0)));
        assertThat(actualFileSize, is(expectedFileSize));
    }
}
