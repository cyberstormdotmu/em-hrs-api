package uk.gov.hmcts.reform.em.hrs.service;

import com.azure.storage.blob.models.BlobRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.em.hrs.domain.AuditActions;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegmentAuditEntry;
import uk.gov.hmcts.reform.em.hrs.exception.InvalidRangeRequestException;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentRepository;
import uk.gov.hmcts.reform.em.hrs.storage.BlobstoreClient;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private Enumeration<String> generateEmptyHeaders() {
        // define the headers you want to be returned
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = Collections.enumeration(headers.keySet());
        return headerNames;
    }

    @BeforeEach
    void before() {
        segment = new HearingRecordingSegment();
        segment.setFilename("XYZ");
        HearingRecording hr = new HearingRecording();
        hr.setCcdCaseId(1234L);
        segment.setHearingRecording(hr);
    }

    @Test
    void testDownload() throws IOException {


        BlobRange blobRange = null;//new BlobRange(0, 1l);

        doReturn(segment).when(segmentRepository).findByFilename(segment.getFilename());
        doReturn(hearingRecordingSegmentAuditEntry)
            .when(auditEntryService).createAndSaveEntry(segment, AuditActions.USER_DOWNLOAD_OK);
        doNothing().when(blobstoreClient).downloadFile(segment.getFilename(), null, null);
        when(request.getHeaderNames()).thenReturn(generateEmptyHeaders());

        segmentDownloadService.download(segment, request, response);

        verify(blobstoreClient, times(1)).downloadFile(segment.getFilename(), null, null);

    }

    @Test
    public void loadsRangedBlobInvalidRangeHeaderStart() {
        Exception exception = assertThrows(InvalidRangeRequestException.class, () -> {
            when(request.getHeader(HttpHeaders.RANGE)).thenReturn("bytes=A-Z");
            when(request.getHeaderNames()).thenReturn(generateEmptyHeaders());
            segmentDownloadService.download(segment, request, response);
        });

    }

    @Test
    public void loadsRangedBlobInvalidRangeHeaderStartGreaterThanEnd() {
        Exception exception = assertThrows(InvalidRangeRequestException.class, () -> {
            when(request.getHeader(HttpHeaders.RANGE)).thenReturn("bytes=1023-0");
            when(request.getHeaderNames()).thenReturn(generateEmptyHeaders());
            segmentDownloadService.download(segment, request, response);
        });
    }

    @Test
    public void loadsRangedBlobTooLargeRangeHeader() throws IOException {

        when(request.getHeader(HttpHeaders.RANGE)).thenReturn("bytes=0-1023");
        when(request.getHeaderNames()).thenReturn(generateEmptyHeaders());

        when(blobstoreClient.getFileSize(anyString())).thenReturn(1000L);
        segmentDownloadService.download(segment, request, response);

        Mockito.verify(response, Mockito.times(1)).setStatus(HttpStatus.PARTIAL_CONTENT
                                                                 .value());//the whole file is returned - not sure if
        // this is an issue if partial content is shown
        Mockito.verify(response, Mockito.times(1)).setHeader(HttpHeaders.CONTENT_RANGE, "bytes 0-999/1000");
        Mockito.verify(response, Mockito.times(1)).setHeader(HttpHeaders.CONTENT_LENGTH, "1000");
    }

    @Test
    public void loadsRangedBlobValidRangeHeader() throws IOException {

        when(request.getHeader(HttpHeaders.RANGE)).thenReturn("bytes=0-1023");
        when(request.getHeaderNames()).thenReturn(generateEmptyHeaders());
        when(blobstoreClient.getFileSize(anyString())).thenReturn(2000L);
        segmentDownloadService.download(segment, request, response);
        Mockito.verify(response, Mockito.times(1)).setStatus(HttpStatus.PARTIAL_CONTENT.value());
        Mockito.verify(response, Mockito.times(1)).setHeader(HttpHeaders.CONTENT_RANGE, "bytes 0-1023/2000");
        Mockito.verify(response, Mockito.times(1)).setHeader(HttpHeaders.CONTENT_LENGTH, "1024");
    }
}
