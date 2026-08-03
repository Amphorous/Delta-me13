package org.hoyo.celestia.user.repository;

import org.hoyo.celestia.user.DTOs.NoRefreshUserDTO;

public interface UserRepositoryCustom {
    NoRefreshUserDTO findUserCardByUid(String uid);
    NoRefreshUserDTO findRandomUserCard();
}
