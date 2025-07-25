/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.storage;

import java.util.UUID;

/**
 * Stores the pending reward for an offline coinflip winner.
 */
public record PendingResult(UUID player, long amount, long beforeTax, long taxedAmount, String provider, UUID loser) {
}
