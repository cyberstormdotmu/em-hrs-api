package uk.gov.hmcts.reform.em.hrs.storage;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.em.hrs.componenttests.config.TestAzureStorageConfig;
import uk.gov.hmcts.reform.em.hrs.helper.AzureIntegrationTestOperations;

import java.io.InputStream;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(classes = {
    TestAzureStorageConfig.class,
    BlobstoreClientImpl.class,
    AzureIntegrationTestOperations.class
})
class BlobstoreClientImplTest {
    private static final String ONE_ITEM_FOLDER = "one-item-folder";
    private static final String TEST_DATA = "Hello World!";
    @Autowired
    private AzureIntegrationTestOperations azureIntegrationTestOperations;
    @Autowired
    private BlobstoreClientImpl underTest;

    @BeforeEach
    void setup() {
        azureIntegrationTestOperations.clearContainer();
    }

    //    @Ignore("Disabling to enable prototyping in frontdoor enabled environment")
    //    @Test
    //    void testShouldDownloadFile() throws Exception {
    //        final String filePath = ONE_ITEM_FOLDER + "/" + UUID.randomUUID() + ".txt";
    //        azureIntegrationTestOperations.populateHrsContainer(filePath, TEST_DATA);
    //
    //        try (final PipedInputStream pipedInput = new PipedInputStream();
    //             final PipedOutputStream output = new PipedOutputStream(pipedInput)) {
    //
    //            //            underTest.downloadFile(filePath, , output);
    //
    //            assertThat(pipedInput).satisfies(this::assertStreamContent);
    //        }
    //    }

    private void assertStreamContent(final InputStream input) {
        final StringBuilder sb = new StringBuilder();
        try {
            await().atMost(Duration.ofSeconds(10)).until(() -> {
                while (true) {
                    sb.append((char) input.read());
                    final String s = sb.toString();
                    if (s.contains(TEST_DATA)) {
                        break;
                    }
                }
                return true;
            });
        } finally {
            assertThat(sb.toString()).isEqualTo(TEST_DATA);
        }
    }

}
