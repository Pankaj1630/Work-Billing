package com.workbilling.service;

import com.workbilling.model.WorkEntry;
import javafx.application.Platform;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.util.Duration;

import java.util.function.Consumer;

public class AutoSaveService {
    private final WorkEntryService workEntryService;
    private ScheduledService<Void> scheduledService;
    private WorkEntry currentEntry;
    private Consumer<String> statusCallback;

    public AutoSaveService(WorkEntryService workEntryService) {
        this.workEntryService = workEntryService;
    }

    public void setStatusCallback(Consumer<String> callback) {
        this.statusCallback = callback;
    }

    public void track(WorkEntry entry) {
        this.currentEntry = entry;
    }

    public void start() {
        stop();
        scheduledService = new ScheduledService<>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        if (currentEntry != null && !currentEntry.getAreas().isEmpty()) {
                            workEntryService.save(currentEntry);
                            Platform.runLater(() -> {
                                if (statusCallback != null) {
                                    statusCallback.accept("Auto-saved at " + java.time.LocalTime.now().withNano(0));
                                }
                            });
                        }
                        return null;
                    }
                };
            }
        };
        scheduledService.setPeriod(Duration.seconds(60));
        scheduledService.setDelay(Duration.seconds(60));
        scheduledService.start();
    }

    public void stop() {
        if (scheduledService != null) {
            scheduledService.cancel();
            scheduledService = null;
        }
    }
}
