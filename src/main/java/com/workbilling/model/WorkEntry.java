package com.workbilling.model;

import com.workbilling.model.enums.RecordStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WorkEntry {
    private Long id;
    private int companyId;
    private String companyName;
    private LocalDate workDate;
    private RecordStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<WorkArea> areas = new ArrayList<>();

    public double getDateTotal() {
        return areas.stream().mapToDouble(WorkArea::getTotal).sum();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getCompanyId() {
        return companyId;
    }

    public void setCompanyId(int companyId) {
        this.companyId = companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
    }

    public RecordStatus getStatus() {
        return status;
    }

    public void setStatus(RecordStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<WorkArea> getAreas() {
        return areas;
    }

    public void setAreas(List<WorkArea> areas) {
        this.areas = areas;
    }
}
