package com.metertracking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "meter_devices")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeterDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meter_number", unique = true, nullable = false)
    private String meterNumber;

    @Column(name = "meter_type", nullable = false)
    private String meterType;

    @Column(name = "sim_card_number")
    private String simCardNumber;

    @Column(name = "iccid_number")
    private String iccidNumber;

    @Column(name = "seal_number")
    private String sealNumber;

    @Column(name = "phases")
    private Integer phases;

    @Column(name = "amperage")
    private Integer amperage;

    @Column(name = "znch")
    private Integer znch;

    /** Напряжение из справочника 1С: "220В", "3х220/380В" */
    @Column(name = "voltage", length = 50)
    private String voltage;

    @Column(nullable = false)
    private boolean available = true;

    /**
     * Ссылка на модель из справочника 1С.
     * Заполняется при регистрации через новую форму.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "meter_model_id")
    private MeterModel meterModel;
}
