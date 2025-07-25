/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.storage;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PendingResultManager {

    private final Map<UUID, PendingResult> pending = new ConcurrentHashMap<>();

    public void save(UUID uuid, PendingResult result) {
        pending.put(uuid, result);
    }

    public boolean has(UUID uuid) {
        return pending.containsKey(uuid);
    }

    public PendingResult get(UUID uuid) {
        return pending.get(uuid);
    }

    public void clear(UUID uuid) {
        pending.remove(uuid);
    }

    public Map<UUID, PendingResult> getAll() {
        return pending;
    }
}
