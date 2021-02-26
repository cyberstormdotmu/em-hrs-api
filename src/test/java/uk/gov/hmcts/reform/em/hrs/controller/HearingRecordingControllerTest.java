package uk.gov.hmcts.reform.em.hrs.controller;

import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.em.hrs.service.HearingRecordingService;

import javax.inject.Inject;
import java.util.Set;

import static java.util.Collections.emptySet;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.json;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
class HearingRecordingControllerTest {
    @Inject
    private MockMvc mockMvc;

    @MockBean
    private HearingRecordingService hearingRecordingService;

    @Test
    public void testWhenRequestedFolderDoesNotExistOrIsEmpty() throws Exception {
        final String folderName = "folder-1";
        final String path = "/folders/" + folderName + "/hearing-recording-file-names";
        doReturn(emptySet()).when(hearingRecordingService).getStoredFiles(folderName);

        final MvcResult mvcResult = mockMvc.perform(get(path).accept(APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andReturn();

        final String content = mvcResult.getResponse().getContentAsString();

        assertThatJson(content)
            .when(Option.IGNORING_ARRAY_ORDER)
            .and(
                x -> x.node("folder-name").isEqualTo("folder-1"),
                x -> x.node("filenames").isArray().isEmpty()
            );
        verify(hearingRecordingService, times(1)).getStoredFiles(folderName);
    }

    @Test
    public void testWhenRequestedFolderHasStoredFiles() throws Exception {
        final String folderName = "folder-1";
        final String path = "/folders/" + folderName + "/hearing-recording-file-names";
        doReturn(Set.of("file-1.mp4", "file-2.mp4")).when(hearingRecordingService).getStoredFiles(folderName);

        final MvcResult mvcResult = mockMvc.perform(get(path).accept(APPLICATION_JSON_VALUE))
            .andExpect(status().isOk())
            .andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andReturn();

        final String content = mvcResult.getResponse().getContentAsString();

        assertThatJson(content)
            .when(Option.IGNORING_ARRAY_ORDER)
            .and(
                x -> x.node("folder-name").isEqualTo("folder-1"),
                x -> x.node("filenames").isArray().isEqualTo(json("[\"file-1.mp4\",\"file-2.mp4\"]"))
            );
        verify(hearingRecordingService, times(1)).getStoredFiles(folderName);
    }
}
