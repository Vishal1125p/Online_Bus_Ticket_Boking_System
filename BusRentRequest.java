package com.bhairavnath.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.bhairavnath.Repository.BusRentRepository;
import com.bhairavnath.entity.BusRentRequest;
import com.bhairavnath.entity.User;

import jakarta.servlet.http.HttpSession;

@Controller
public class BusRentController {

    @Autowired
    private BusRentRepository rentRepo;

    // ================= USER SIDE =================

    // OPEN RENT PAGE (IMPORTANT FIX)
    @GetMapping("/rent-bus")
    public String rentPage(HttpSession session, Model model) {

        User user = (User) session.getAttribute("user");

        if (user != null) {
            List<BusRentRequest> rents =
                    rentRepo.findByUserId(user.getId());
            model.addAttribute("rents", rents);
        }

        return "rent-bus"; // user UI page
    }

    // SUBMIT RENT REQUEST
    @PostMapping("/rent-bus")
    public String submitRent(
            @RequestParam String rentType,
            @RequestParam String fromCity,
            @RequestParam String toCity,
            @RequestParam LocalDate journeyDate,
            @RequestParam int days,
            @RequestParam(required = false) String purpose,
            HttpSession session
    ) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/login";
        }

        BusRentRequest rent = new BusRentRequest();
        rent.setRentType(rentType);
        rent.setFromCity(fromCity);
        rent.setToCity(toCity);
        rent.setJourneyDate(journeyDate);
        rent.setDays(days);
        rent.setPurpose(purpose);
        rent.setStatus("REQUESTED");
        rent.setUser(user);

        rentRepo.save(rent);

        // 🔥 SAME PAGE → MODAL WORKS
        return "redirect:/rent-bus?success";
    }

    // ================= ADMIN SIDE =================

    @GetMapping("/admin/rent-requests")
    public String adminRent(Model model) {
        model.addAttribute("rents", rentRepo.findAll());
        return "admin-rent-requests";
    }

    @PostMapping("/admin/approve-rent")
    public String approveRent(
            @RequestParam Long id,
            @RequestParam Double amount
    ) {
        BusRentRequest rent = rentRepo.findById(id).orElseThrow();

        rent.setStatus("APPROVED");
        rent.setAmount(amount);

        rentRepo.save(rent);

        return "redirect:/admin/rent-requests";
    }

    @GetMapping("/admin/reject-rent/{id}")
    public String reject(@PathVariable Long id) {
        BusRentRequest rent = rentRepo.findById(id).orElseThrow();

        rent.setStatus("REJECTED");
        rentRepo.save(rent);

        return "redirect:/admin/rent-requests";
    }
}
