package com.safecarealert.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alert_messages")
public class AlertMessage extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false)
    public Alert alert;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_user_id")
    public User senderUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_device_id")
    public Device senderDevice;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", length = 30)
    public AlertMessageType messageType;

    @Column(length = 2000)
    public String payload;           // For JSON data if needed

    @Column(length = 500)
    public String note;              // Human-readable message / comment

    @Column(name = "sent_at")
    public LocalDateTime sentAt;
}