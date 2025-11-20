package com.example.youth.db;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Housing {

    @Id
    @Column(length = 50)
    private String housingId;

    @Column(length = 50, nullable = false)
    private String name;

    @Column(length = 255)
    private String address;

    private Double supplyArea;
    private java.sql.Date completeDate;

    @Column(length = 20)
    private String organization;

    private java.sql.Date applicationStart;
    private java.sql.Date applicationEnd;

    private String heatingType;
    private Boolean elevator;
    private Integer parkingSpaces;
    private Integer deposit;
    private Integer monthlyRent;
    private Integer totalUnits;
    private String link;
}
