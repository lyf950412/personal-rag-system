package com.rag.controller;

import com.rag.dto.ApiResponse;
import com.rag.dto.DashboardStatsDTO;
import com.rag.service.DashboardService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;
    
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }
    
    @GetMapping("/stats")
    public ApiResponse<DashboardStatsDTO> getDashboardStats() {
        return ApiResponse.success(dashboardService.getDashboardStats());
    }
}
