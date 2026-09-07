package com.deepblue.rescue.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "animals")
public class Animal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "animal_code", nullable = false, unique = true)
    private String animalCode;

    @Column(name = "common_name", nullable = false)
    private String commonName;

    @Column(name = "scientific_name")
    private String scientificName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnimalSex sex;

    @Column(name = "tracking_device_code")
    private String trackingDeviceCode;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rescue_case_id", nullable = false, unique = true)
    private RescueCase rescueCase;

    @OneToOne(mappedBy = "animal", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private MedicalRecord medicalRecord;

    protected Animal() {
    }

    @OneToMany(mappedBy = "animal")
    private java.util.List<Treatment> treatments = new java.util.ArrayList<>();

    public Animal(String animalCode, String commonName, String scientificName, AnimalSex sex) {
        this.animalCode = animalCode;
        this.commonName = commonName;
        this.scientificName = scientificName;
        this.sex = sex;
    }

    public void setRescueCase(RescueCase rescueCase) {
        this.rescueCase = rescueCase;
    }

    public void assignMedicalRecord(MedicalRecord medicalRecord) {
        this.medicalRecord = medicalRecord;
        medicalRecord.setAnimal(this);
    }

    public Long getId() {
        return id;
    }

    public String getAnimalCode() {
        return animalCode;
    }

    public String getCommonName() {
        return commonName;
    }

    public String getScientificName() {
        return scientificName;
    }

    public AnimalSex getSex() {
        return sex;
    }

    public String getTrackingDeviceCode() {
        return trackingDeviceCode;
    }

    public void setTrackingDeviceCode(String trackingDeviceCode) {
        this.trackingDeviceCode = trackingDeviceCode;
    }

    public RescueCase getRescueCase() {
        return rescueCase;
    }

    public MedicalRecord getMedicalRecord() {
        return medicalRecord;
    }

    public java.util.List<Treatment> getTreatments() {
        return treatments;
    }
}