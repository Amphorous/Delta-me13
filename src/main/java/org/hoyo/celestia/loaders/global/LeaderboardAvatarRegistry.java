package org.hoyo.celestia.loaders.global;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;

// Which avatarIds Immercalc currently has damage-calc code for. Kept in sync
// two ways - this service pulls it once at its own startup
// (LeaderboardStartupSync), and Immercalc pushes it here on its own startup
// too (LoaderController) - so whichever side (re)started most recently is
// the one that leaves this accurate, without needing to restart the other.
@Component
public class LeaderboardAvatarRegistry {

    private volatile Set<String> avatarIdsWithLeaderboard = Set.of();

    public boolean hasLeaderboard(String avatarId) {
        return avatarIdsWithLeaderboard.contains(avatarId);
    }

    public void setAvatarIds(Collection<String> avatarIds) {
        avatarIdsWithLeaderboard = Set.copyOf(avatarIds);
    }
}
