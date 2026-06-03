package com.org.hr.controller;

import com.org.hr.model.LeaveRecord;
import com.org.hr.service.HRService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/hr")
@RequiredArgsConstructor
@Validated
class HRController {

    private final HRService hrService;

    @PostMapping("/leave")
    public LeaveRecord applyLeave(@RequestParam @NotBlank String username,
                                  @RequestParam
                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                  @NotNull LocalDate date) {
        return hrService.applyLeave(username, date);
    }

    @GetMapping("/leave/{username}")
    public boolean isOnLeave(@PathVariable @NotBlank String username,
                             @RequestParam
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                             @NotNull LocalDate date) {

        return hrService.isOnLeave(username, date);
    }

    @GetMapping("/replacement/{username}")
    public String findReplacement(@PathVariable @NotBlank String username,
                                  @RequestParam
                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                  @NotNull LocalDate date) {

        return hrService.findReplacement(username, date);
    }

}
