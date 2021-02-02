package uk.gov.hmcts.reform.em.hrs.componenttests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import uk.gov.hmcts.reform.em.hrs.domain.Folder;
import uk.gov.hmcts.reform.em.hrs.domain.HearingRecording;


import javax.sql.rowset.serial.SerialBlob;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class TestUtil {

    public static final String BLOB_DATA = "data";

//    public static final DocumentContent DOCUMENT_CONTENT;
//
//    static {
//        try {
//            DOCUMENT_CONTENT = new DocumentContent(new SerialBlob(BLOB_DATA.getBytes(StandardCharsets.UTF_8)));
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }

    public static final MockMultipartFile TEST_FILE;

    static {
        try {
            TEST_FILE = new MockMultipartFile("file", "filename.txt", "text/plain", "some xml".getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static final MockMultipartFile TEST_FILE_EXE;

    static {
        try {
            TEST_FILE_EXE = new MockMultipartFile("file", "filename.exe", "application/octet-stream", "some xml".getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static final MockMultipartFile TEST_FILE_WITH_FUNNY_NAME;

    static {
        try {
            TEST_FILE_WITH_FUNNY_NAME = new MockMultipartFile("file", "filename!@£$%^&*()<>.txt", "text/plain", "some xml".getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static final MediaType APPLICATION_JSON_UTF8 = new MediaType(MediaType.APPLICATION_JSON.getType(), MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));
    public static final MediaType MULTIPART_FORM_DATA = new MediaType(MediaType.MULTIPART_FORM_DATA.getType(), MediaType.MULTIPART_FORM_DATA.getSubtype());

    public static final UUID RANDOM_UUID = UUID.randomUUID();

    public static final Folder TEST_FOLDER =
            new Folder(RANDOM_UUID, "name", null, null, null, null, null);


    public static final Folder folder = Folder.builder()
        .id(RANDOM_UUID)
        .hearingRecordings(
            Stream.of(HearingRecording.builder().id(RANDOM_UUID).build()).collect(Collectors.toList()))
        .build();


    public static final HearingRecording HEARING_RECORDING = HearingRecording.builder()
        .id(RANDOM_UUID)
        .folder(Folder.builder().id(RANDOM_UUID).build())
        .build();

    public static final HearingRecording DELETED_HEARING_RECORDING = HearingRecording.builder()
        .id(RANDOM_UUID)
        .deleted(true)
        .folder(Folder.builder().id(RANDOM_UUID).build())
        .build();

    public static final HearingRecording HARD_DELETED_HEARING_RECORDING = HearingRecording.builder()
        .id(RANDOM_UUID)
        .deleted(true)
        .hardDeleted(true)
        .folder(Folder.builder().id(RANDOM_UUID).build())
        .build();

    private TestUtil() {
    }

    public static byte[] convertObjectToJsonBytes(Object object) throws IOException {
        ObjectMapper om = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return om.writeValueAsBytes(object);
    }

    public static String convertObjectToJsonString(Object object) throws IOException {
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        return ow.writeValueAsString(object);
    }

}
