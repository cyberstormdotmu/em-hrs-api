package uk.gov.hmcts.reform.em.hrs.auditlog;


import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.em.hrs.domain.AuditEntry;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Component
public class AuditLogFormatter {

    private static final String TAG = "HRS-API";

    private static final String COMMA = ",";
    private static final String COLON = ":";

    public String format(AuditEntry entry) {
        return new StringBuilder(TAG)
            .append(" ")
            .append(getFirstPair("dateTime", entry.getEventDateTime()))
            .append(getPair("action", entry.getAction().toString()))
            .append(getPair("clientIp", entry.getIpAddress()))
            .append(getPair("service", entry.getServiceName()))
            .append(getPair("user", entry.getUsername()))
            .append(getPair("caseId", entry.getCaseId()))
            .toString();
    }

    private String getPair(String label, String value) {
        return isNotBlank(value) ? COMMA + label + COLON + value : "";
    }

    private String getFirstPair(String label, Date value) {
        LocalDateTime java8Date = value.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        String formattedDate = java8Date.format(ISO_LOCAL_DATE_TIME);
        return
            label + COLON + formattedDate;
    }

}
