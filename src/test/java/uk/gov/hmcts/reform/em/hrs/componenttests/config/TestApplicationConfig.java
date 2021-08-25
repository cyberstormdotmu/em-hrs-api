package uk.gov.hmcts.reform.em.hrs.componenttests.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import uk.gov.hmcts.reform.em.hrs.dto.HearingRecordingDto;
import uk.gov.hmcts.reform.em.hrs.util.Snooper;

import java.util.concurrent.LinkedBlockingQueue;

import static uk.gov.hmcts.reform.em.hrs.componenttests.TestUtil.INGESTION_QUEUE_SIZE;

@TestConfiguration
public class TestApplicationConfig {
    @Bean
    @Primary
    public Snooper provideSnooper() {
        return Mockito.spy(Snooper.class);
    }

    @Bean
    public LinkedBlockingQueue<HearingRecordingDto> ingestionQueue() {
        return new LinkedBlockingQueue<HearingRecordingDto>(INGESTION_QUEUE_SIZE);
    }

    @Bean
    public LinkedBlockingQueue<HearingRecordingDto> ccdQueue() {
        return new LinkedBlockingQueue<HearingRecordingDto>(INGESTION_QUEUE_SIZE);
    }


}
