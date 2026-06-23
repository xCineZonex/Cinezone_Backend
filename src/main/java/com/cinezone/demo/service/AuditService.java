package com.cinezone.demo.service;

public interface AuditService {
    void logAction(String entityName, Long entityId, String action, String modifiedBy, String details);
}
