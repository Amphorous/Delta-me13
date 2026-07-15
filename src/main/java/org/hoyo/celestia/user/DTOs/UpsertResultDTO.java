package org.hoyo.celestia.user.DTOs;

import lombok.Data;

import java.util.List;

/**
 * Body for GET /user/dashboard/refresh/{uid}. Replaces the old bare Boolean —
 * an object is still truthy to a not-yet-redeployed frontend, and hard
 * failures still arrive as non-2xx statuses, so the change is
 * backward-tolerant. `partial`/`warnings` carry the "some characters use
 * game assets we don't have yet and were skipped" signal to the frontend.
 */
@Data
public class UpsertResultDTO {
    private boolean success;
    private String message;
    private boolean partial;
    private List<String> skippedAvatarIds;
    private List<String> warnings;
}
