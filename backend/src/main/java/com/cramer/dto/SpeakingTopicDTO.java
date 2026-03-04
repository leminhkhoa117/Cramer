package com.cramer.dto;

/**
 * DTO for SpeakingTopic responses.
 */
public class SpeakingTopicDTO {

    private Long id;
    private String code;
    private String nameVi;
    private String nameEn;
    private String icon;
    private String color;

    // Constructors
    public SpeakingTopicDTO() {
    }

    public SpeakingTopicDTO(Long id, String code, String nameVi, String nameEn, String icon, String color) {
        this.id = id;
        this.code = code;
        this.nameVi = nameVi;
        this.nameEn = nameEn;
        this.icon = icon;
        this.color = color;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNameVi() {
        return nameVi;
    }

    public void setNameVi(String nameVi) {
        this.nameVi = nameVi;
    }

    public String getNameEn() {
        return nameEn;
    }

    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
