package com.example.nexus.modules.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "client")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255)
    private String name;

    @OneToMany(mappedBy = "client", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = false)
    @Builder.Default
    private List<AppUser> users = new ArrayList<>();

    @PreRemove
    private void unlinkUsersBeforeRemove() {
        for (AppUser user : users) {
            user.setClient(null);
        }
    }
}
