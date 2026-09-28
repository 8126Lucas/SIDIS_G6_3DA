package com.sidis.maintenanceservice;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class LandingController {

    @GetMapping("/welcome")
    public String welcome() {
        return "Welcome to the AISafePSOFT_26";
    }
}