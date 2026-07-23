package com.workbilling.model;

import java.util.ArrayList;
import java.util.List;

public class WorkArea {
    private Long id;
    private Long workEntryId;
    private String name;
    private int sortOrder;
    private List<WorkItem> items = new ArrayList<>();

    public WorkArea() {
    }

    public WorkArea(String name) {
        this.name = name;
    }

    public double getTotal() {
        return items.stream().mapToDouble(WorkItem::getAmount).sum();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWorkEntryId() {
        return workEntryId;
    }

    public void setWorkEntryId(Long workEntryId) {
        this.workEntryId = workEntryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public List<WorkItem> getItems() {
        return items;
    }

    public void setItems(List<WorkItem> items) {
        this.items = items;
    }
}
