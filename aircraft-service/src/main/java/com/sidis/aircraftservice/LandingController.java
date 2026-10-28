package com.sidis.aircraftservice;

import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api")
public class LandingController {

    @GetMapping("/welcome")
    public String welcome() {
        return "Welcome to the SIDIS_G6_3DA";
    }
}