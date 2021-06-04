package uk.gov.hmcts.reform.em.hrs.service;

import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.BlobRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.em.hrs.domain.AuditActions;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegmentAuditEntry;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentRepository;
import uk.gov.hmcts.reform.em.hrs.storage.BlobstoreClient;

import java.io.IOException;
import java.util.UUID;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = {SegmentDownloadServiceImpl.class})
class SegmentDownloadServiceImplTest {

    private static final UUID RECORDING_ID = UUID.randomUUID();
    private static final Integer SEGMENT_NO = Integer.valueOf(10);
    @MockBean
    private HearingRecordingSegmentRepository segmentRepository;
    @MockBean
    private BlobstoreClient blobstoreClient;
    @MockBean
    private AuditEntryService auditEntryService;

    @MockBean
    private HttpServletRequest request;
    @MockBean
    private HttpServletResponse response;

    @MockBean
    private HearingRecordingSegmentAuditEntry hearingRecordingSegmentAuditEntry;
    private HearingRecordingSegment segment;
    @Inject
    private SegmentDownloadServiceImpl segmentDownloadService;

    @BeforeEach
    void before() {
        segment = new HearingRecordingSegment();
        segment.setFilename("XYZ");
    }

    @Test
    void testGetDownloadInfo() {
        BlobProperties blobProperties = new BlobProperties(
            null, null, null, 1234L, "video/mp4",null,
            null, null, null, null,null,
            null, null, null, null,null, null, null,
            null, null,null, null, null,
            null, null,null, null, null,
            null, null, null);

        doReturn(segment).when(segmentRepository).findByHearingRecordingIdAndRecordingSegment(RECORDING_ID, SEGMENT_NO);
        doReturn(hearingRecordingSegmentAuditEntry)
            .when(auditEntryService).createAndSaveEntry(segment, AuditActions.USER_DOWNLOAD_REQUESTED);
        doReturn(blobProperties).when(blobstoreClient).getBlobProperties(segment.getFilename());

        segmentDownloadService.getDownloadInfo(RECORDING_ID, SEGMENT_NO);

        verify(segmentRepository, times(1))
            .findByHearingRecordingIdAndRecordingSegment(RECORDING_ID, SEGMENT_NO);
        verify(auditEntryService, times(1))
            .createAndSaveEntry(segment, AuditActions.USER_DOWNLOAD_REQUESTED);
        verify(blobstoreClient, times(1)).getBlobProperties(segment.getFilename());
    }

    @Test
    void testDownload() throws IOException {

        BlobRange blobRange = null;//new BlobRange(0, 1l);

        doReturn(segment).when(segmentRepository).findByFilename(segment.getFilename());
        doReturn(hearingRecordingSegmentAuditEntry)
            .when(auditEntryService).createAndSaveEntry(segment, AuditActions.USER_DOWNLOAD_OK);
        doNothing().when(blobstoreClient).downloadFile(segment.getFilename(), null, null);

        segmentDownloadService.download(segment.getFilename(), request, response);

        verify(segmentRepository, times(1))
            .findByFilename(segment.getFilename());
        verify(blobstoreClient, times(1)).downloadFile(segment.getFilename(), null, null);
        verify(auditEntryService, times(1))
            .createAndSaveEntry(segment, AuditActions.USER_DOWNLOAD_OK);
    }
}
