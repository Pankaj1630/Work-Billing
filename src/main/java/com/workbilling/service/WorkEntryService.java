package com.workbilling.service;

import com.workbilling.model.WorkArea;
import com.workbilling.model.WorkEntry;
import com.workbilling.model.WorkItem;
import com.workbilling.model.enums.RecordStatus;
import com.workbilling.repository.WorkEntryRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class WorkEntryService {
    private final WorkEntryRepository repository = new WorkEntryRepository();

    public long save(WorkEntry entry) throws SQLException {
        if (entry.getStatus() == null) {
            entry.setStatus(RecordStatus.DRAFT);
        }
        return repository.save(entry);
    }

    public void markSaved(WorkEntry entry) throws SQLException {
        entry.setStatus(RecordStatus.SAVED);
        repository.save(entry);
    }

    public Optional<WorkEntry> findById(long id) throws SQLException {
        return repository.findById(id);
    }

    public List<WorkEntry> search(Integer companyId, LocalDate fromDate, LocalDate toDate,
                                  String areaName, String description, boolean includeBilled) throws SQLException {
        return repository.search(companyId, fromDate, toDate, areaName, description, includeBilled);
    }

    public void delete(long id) throws SQLException {
        repository.delete(id);
    }

    public WorkEntry duplicate(long sourceId, LocalDate newDate) throws SQLException {
        WorkEntry source = repository.findById(sourceId)
                .orElseThrow(() -> new IllegalArgumentException("Source entry not found"));

        WorkEntry copy = new WorkEntry();
        copy.setCompanyId(source.getCompanyId());
        copy.setCompanyName(source.getCompanyName());
        copy.setWorkDate(newDate);
        copy.setStatus(RecordStatus.DRAFT);

        for (WorkArea sourceArea : source.getAreas()) {
            WorkArea area = new WorkArea(sourceArea.getName());
            for (WorkItem sourceItem : sourceArea.getItems()) {
                WorkItem item = new WorkItem(
                        sourceItem.getDescription(),
                        sourceItem.getSqFt(),
                        sourceItem.getNos(),
                        sourceItem.getRate()
                );
                item.setManualAmount(sourceItem.isManualAmount());
                item.setStoredAmount(sourceItem.getStoredAmount());
                area.getItems().add(item);
            }
            copy.getAreas().add(area);
        }
        return copy;
    }
}
