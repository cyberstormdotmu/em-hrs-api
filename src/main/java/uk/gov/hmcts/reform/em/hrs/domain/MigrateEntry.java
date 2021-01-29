package uk.gov.hmcts.reform.em.hrs.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.UUID;

/**
 * A simpler mapping to auditentry table.
 */
@Entity
@Table(name = "auditentry")
@NoArgsConstructor
public class MigrateEntry {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Getter
    private UUID id;

    @Setter
    @Getter
    private String type;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Getter
    @Setter
    private AuditActions action;

    @NotNull
    @Temporal(TemporalType.TIMESTAMP)
    @Setter
    @Getter
    private Date recordedDateTime;

    @Setter
    @Getter
    @Column(name = "storeddocument_id")
    private UUID storeddocumentId;

    @Setter
    @Getter
    @Column(name = "documentcontentversion_id")
    private UUID documentcontentversionId;

    @Setter
    @Getter
    private String servicename;

    public MigrateEntry(String type,
                        AuditActions action,
                        DocumentContentVersion documentcontentversion,
                        String serviceName) {
        this.type = type;
        this.action = action;
        this.storeddocumentId = documentcontentversion.getStoredDocument().getId();
        this.documentcontentversionId = documentcontentversion.getId();
        this.servicename = serviceName;
        this.recordedDateTime = new Date();
    }
}
