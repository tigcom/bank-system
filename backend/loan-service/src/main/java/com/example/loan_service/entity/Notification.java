package com.example.loan_service.entity;


import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Data
@NoArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notif_id")
    private Long notifId;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Column(name = "send_date", nullable = false)
    private LocalDateTime sendDate = LocalDateTime.now();

    @Column(nullable = false, length = 30)
    private String type;

    @Column(nullable = false, length = 255)
    private String message;
}