package uk.gov.hmcts.reform.em.hrs.service.ccd;

import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecordingSegment;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingRepository;
import uk.gov.hmcts.reform.em.hrs.repository.HearingRecordingSegmentRepository;
import uk.gov.hmcts.reform.em.hrs.service.FolderService;

import java.util.Optional;

@Service
@Transactional
public class CcdUploadServiceImpl implements CcdUploadService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CcdUploadServiceImpl.class);

    private final CcdDataStoreApiClient ccdDataStoreApiClient;
    private final HearingRecordingRepository recordingRepository;
    private final HearingRecordingSegmentRepository segmentRepository;
    private final FolderService folderService;

    @Autowired
    public CcdUploadServiceImpl(final CcdDataStoreApiClient ccdDataStoreApiClient,
                                final HearingRecordingRepository recordingRepository,
                                final HearingRecordingSegmentRepository segmentRepository,
                                final FolderService folderService) {
        this.ccdDataStoreApiClient = ccdDataStoreApiClient;
        this.recordingRepository = recordingRepository;
        this.segmentRepository = segmentRepository;
        this.folderService = folderService;
    }

    @Override
    public void upload(final HearingRecordingDto hearingRecordingDto) {

        final Optional<HearingRecording> checkHearingRecording =
            recordingRepository.findByRecordingRefAndFolderName(
                hearingRecordingDto.getRecordingRef(), hearingRecordingDto.getFolder()
            );

        checkHearingRecording.ifPresentOrElse(
            hearingRecording -> updateCase(hearingRecording, hearingRecordingDto),
            () -> createCaseinCcdAndPersist(hearingRecordingDto)
        );

    }

    private void updateCase(final HearingRecording recording,
                            final HearingRecordingDto recordingDto) {
        if (recording.getCcdCaseId() == null) {
            LOGGER.info(
                "Recording Ref {} in folder {}, has no ccd id, case still being created in CCD or has been rejected",
                recordingDto.getRecordingRef(),
                recordingDto.getFolder()
            );
            return;
        }

        LOGGER.info(
            "adding  recording (ref {}) in folder {} to case (ccdId {})",
            recordingDto.getRecordingRef(),
            recordingDto.getFolder(),
            recording.getCcdCaseId()
        );


        Long caseDetailsId =
            ccdDataStoreApiClient.updateCaseData(recording.getCcdCaseId(), recording.getId(), recordingDto);

        LOGGER.info("Case Details (id {}) updated successfully", caseDetailsId);

        try {
            HearingRecordingSegment segment = createSegment(recording, recordingDto);
            segmentRepository.save(segment);

        } catch (Exception e) {
            LOGGER.info(
                "segment not added to database, probably duplicate entry (ref {}) to case(ccdId {})",
                recordingDto.getRecordingRef(),
                recording.getCcdCaseId()
            );
        }

        LOGGER.info("updateCase end");
    }

    private void createCaseinCcdAndPersist(final HearingRecordingDto recordingDto) {
        LOGGER.info("creating a new case for recording: {}", recordingDto.getRecordingRef());

        var folder = folderService.getFolderByName(recordingDto.getFolder());

        HearingRecording recording = HearingRecording.builder()
            .folder(folder)
            .recordingRef(recordingDto.getRecordingRef())
            .caseRef(recordingDto.getCaseRef())
            .hearingLocationCode(recordingDto.getCourtLocationCode())
            .hearingRoomRef(recordingDto.getHearingRoomRef())
            .hearingSource(recordingDto.getRecordingSource())
            .jurisdictionCode(recordingDto.getJurisdictionCode())
            .serviceCode(recordingDto.getServiceCode())
            .createdOn(recordingDto.getRecordingDateTime())
            .build();

        try {
            recording = recordingRepository.save(recording);

        } catch (ConstraintViolationException e) {
            //the recording has already been persisted by another cluster - do not proceed as waiting for CCD id
            LOGGER
                .info(
                    "create case Hearing Recording already exists in database, not persisting recording, nor segment "
                        + "at this time");
        } catch (Exception e) {
            LOGGER.info(
                "create case Unhandled Exception whilst adding segment to DB (ref {}) to case(ccdId {})",
                recordingDto.getRecordingRef(),
                recording.getCcdCaseId()
            );
        }


        final Long caseId = ccdDataStoreApiClient.createCase(recording.getId(), recordingDto);
        recording.setCcdCaseId(caseId);
        recording = recordingRepository.save(recording);

        HearingRecordingSegment segment = createSegment(recording, recordingDto);
        segmentRepository.save(segment);
    }

    private HearingRecordingSegment createSegment(final HearingRecording recording,
                                                  final HearingRecordingDto recordingDto) {
        return HearingRecordingSegment.builder()
            .filename(recordingDto.getFilename())
            .fileExtension(recordingDto.getFilenameExtension())
            .fileSizeMb(recordingDto.getFileSize())
            .fileMd5Checksum(recordingDto.getCheckSum())
            .ingestionFileSourceUri(recordingDto.getCvpFileUrl())
            .recordingSegment(recordingDto.getSegment())
            .hearingRecording(recording)
            .build();
    }


}
