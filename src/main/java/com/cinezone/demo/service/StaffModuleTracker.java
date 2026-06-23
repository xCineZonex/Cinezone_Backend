package com.cinezone.demo.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

@Service
public class StaffModuleTracker {
    private final ConcurrentHashMap<UUID, String> activeStaffModules = new ConcurrentHashMap<>();

    public void setModule(UUID userId, String module) {
        if (module == null || module.equalsIgnoreCase("NONE")) {
            activeStaffModules.remove(userId);
        } else {
            activeStaffModules.put(userId, module.toUpperCase());
        }
    }

    public void removeModule(UUID userId) {
        activeStaffModules.remove(userId);
    }

    public boolean isPortero(UUID userId) {
        return "PORTERO".equals(activeStaffModules.get(userId));
    }
}
