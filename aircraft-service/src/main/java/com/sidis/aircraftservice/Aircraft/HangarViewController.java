package com.sidis.aircraftservice.Aircraft;

import com.sidis.aircraftservice.Aircraft.infrastructure.CalculationsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/api/hangar/view")
public class HangarViewController {
    private final CalculationsService calculationsService;

    public HangarViewController(CalculationsService calculationsService) {
        this.calculationsService = calculationsService;
    }

    /**
     * Gets a graph with utilization per Aircraft
     */
/*    @GetMapping("/utilization")
    public String utilizationPage(Model model) {
        List<HangarController.UtilizationInfo> data =
                calculationsService.getUtilizationInfo();
        model.addAttribute("utilizationData", data);
        return "utilization";
    }*/
}