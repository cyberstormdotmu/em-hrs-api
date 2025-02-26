package uk.gov.hmcts.reform.em.hrs.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HearingRecordingRepository extends PagingAndSortingRepository<HearingRecording, UUID> {

    Optional<HearingRecording> findByRecordingRefAndFolderName(String recordingReference, String folderName);

    Optional<HearingRecording> findByCcdCaseId(Long caseId);

    @Modifying
    @Query("delete from HearingRecording s where s.createdOn < :#{#createddate} and s.ccdCaseId is null")
    void deleteStaleRecordsWithNullCcdCaseId(@Param("createddate") LocalDateTime createddate);
}
