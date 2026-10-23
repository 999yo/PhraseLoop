package com.phraseloop.controller;

import com.phraseloop.dto.DashboardResponse;
import com.phraseloop.security.CurrentUser;
import com.phraseloop.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentUser currentUser;

    public DashboardController(DashboardService dashboardService, CurrentUser currentUser) {
        this.dashboardService = dashboardService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public DashboardResponse dashboard() {
        return dashboardService.dashboard(currentUser.require().getId());
    }
}
