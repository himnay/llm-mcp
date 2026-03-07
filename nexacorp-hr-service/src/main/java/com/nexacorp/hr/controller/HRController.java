package com.nexacorp.hr.controller;

import com.nexacorp.hr.model.LeaveRecord;
import com.nexacorp.hr.service.HRService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/hr")
@RequiredArgsConstructor
class HRController {

    private final HRService hrService;

    @PostMapping("/leave")
    public LeaveRecord applyLeave(@RequestParam String username,
                                  @RequestParam
                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                  LocalDate date) {
        return hrService.applyLeave(username, date);
    }

    @GetMapping("/leave/{username}")
    public boolean isOnLeave(@PathVariable String username,
                             @RequestParam
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                             LocalDate date) {

        return hrService.isOnLeave(username, date);
    }

    @GetMapping("/replacement/{username}")
    public String findReplacement(@PathVariable String username,
                                  @RequestParam
                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                  LocalDate date) {

        return hrService.findReplacement(username, date);
    }

}
