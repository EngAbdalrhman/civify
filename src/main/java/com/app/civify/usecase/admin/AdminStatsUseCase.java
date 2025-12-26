package com.app.civify.usecase.admin;

import com.app.civify.dto.admin.AdminStatsDTO;
import com.app.civify.enums.StatsPeriod;

public interface AdminStatsUseCase {
    AdminStatsDTO getStats(StatsPeriod period);
}
