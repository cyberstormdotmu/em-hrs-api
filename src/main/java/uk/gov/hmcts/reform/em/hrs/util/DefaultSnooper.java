package uk.gov.hmcts.reform.em.hrs.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DefaultSnooper implements Snooper {

    @Override
    public void snoop(final String message) {
        log.info(message);
    }

    @Override
    public void snoop(final String message, final Throwable throwable) {
        log.error(message, throwable);
    }
}
