package com.org.hr.model;

import jakarta.persistence.*;
import lombok.*;

@Table(name = "employee")
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    private String fullName;

    @Column(nullable = false)
    private String team;
}
