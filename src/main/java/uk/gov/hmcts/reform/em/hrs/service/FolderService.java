package uk.gov.hmcts.reform.em.hrs.service;

import uk.gov.hmcts.reform.em.hrs.domain.Folder;

import java.util.Set;
import javax.validation.constraints.NotNull;

public interface FolderService {
    Set<String> getStoredFiles(String folderName);

    String getFolderNameFromFilePath(@NotNull String path);

    Folder getFolderByName(@NotNull String folderName);
}
