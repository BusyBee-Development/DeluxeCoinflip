/*
 * DeluxeCoinflip Plugin
 * Copyright (c) 2021 - 2025 Zithium Studios. All rights reserved.
 */

package net.zithium.deluxecoinflip.storage.handler;

/**
 * Provides shutdown operations for coinflip games.
 */
public interface GameShutdownProvider {

    /**
     * Shuts down all coinflip games, including both active games and non-active listings.
     */
    void shutdownAll();

    /**
     * Shuts down all active coinflip games.
     * Active games should have their animations stopped, participants refunded,
     * and references removed from memory and storage.
     */
    void shutdownActiveGames();

    /**
     * Shuts down all non-active coinflip listings.
     * Non-active listings are games that have been created but are not yet active.
     * The game creator should be refunded, and the listing removed from storage.
     */
    void shutdownNonActiveListings();
}
