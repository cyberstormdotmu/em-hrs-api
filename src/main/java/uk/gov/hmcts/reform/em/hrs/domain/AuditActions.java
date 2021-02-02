package uk.gov.hmcts.reform.em.hrs.domain;

public enum AuditActions {

    INGEST_FILE_DOWNLOAD_OK,
    INGEST_FILE_DOWNLOAD_FAIL,

    INGEST_FILE_AV_CHECK_OK,
    INGEST_FILE_AV_CHECK_FAIL,

    INGEST_METADATA_PATTERNS_MATCH_ALL,
    INGEST_METADATA_PATTERNS_MATCH_NONE,
    INGEST_METADATA_PATTERNS_MATCH_FAIL,

    INGEST_FILE_STORE_OK,
    INGEST_FILE_STORE_FAIL,

    INGEST_CCD_CREATE_CASE_FOUND_EXISTING,
    INGEST_CCD_CREATE_CASE_OK,
    INGEST_CCD_CREATE_CASE_FAIL,

    INGEST_CCD_ATTACH_HEARING_RECORDING_FOUND_EXISTING,
    INGEST_CCD_ATTACH_HEARING_RECORDING_OK,
    INGEST_CCD_ATTACH_HEARING_RECORDING_FAIL,

    INGEST_OK,
    INGEST_FAIL,
    INGEST_RETRIED,
    INGEST_TOO_MANY_FAILURES,
    INGEST_IGNORE,

    SHARE_GRANT_OK,
    SHARE_GRANT_FAIL,

    SHARE_REVOKE_OK,
    SHARE_REVOKE_FAIL,

    NOTIFY_OK,
    NOTIFY_FAIL,

    USER_DOWNLOAD_OK,
    USER_DOWNLOAD_UNAUTHORIZED,
    USER_DOWNLOAD_FAIL,


    DATA_LIFECYCLE_CREATED,
    DATA_LIFECYCLE_UPDATED,
    DATA_LIFECYCLE_DELETED,
    DATA_LIFECYCLE_HARD_DELETED,


    TTL_UPDATED;
}
