package com.dmtryii.entity;

import com.dmtryii.entity.enums.UserStates;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@EqualsAndHashCode(exclude = "id")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long telegramUserId;
    @CreationTimestamp
    private LocalDateTime firstLoginData;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private Boolean isActive;
    @Enumerated(EnumType.STRING)
    private UserStates state;
}
