package com.bhairavnath.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.bhairavnath.Repository.AdminRepository;
import com.bhairavnath.Repository.BookingRepository;
import com.bhairavnath.Repository.BusRepository;
import com.bhairavnath.Repository.UserRepository;
import com.bhairavnath.entity.Admin;
import com.bhairavnath.entity.Booking;
import com.bhairavnath.entity.Bus;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private BusRepository busRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private BookingRepository bookingRepo;

    @Autowired
    private AdminRepository adminRepo;

    // ================= ADMIN DASHBOARD =================
    @GetMapping("/dashboard")
    public String adminDashboard(HttpSession session, Model model) {

        if (session.getAttribute("admin") == null) {
            return "redirect:/";
        }

        model.addAttribute("totalBuses", busRepo.count());
        model.addAttribute("totalBookings", bookingRepo.count());
        model.addAttribute(
                "todayBookings",
                bookingRepo.countByJourneyDate(LocalDate.now())
        );
        model.addAttribute(
                "totalRevenue",
                bookingRepo.sumRevenue()
        );

        // ✅ ONLY LAST 3 BOOKINGS
        model.addAttribute(
                "recentBookings",
                bookingRepo.findTop3ByOrderByIdDesc()
        );

        return "admin-dashboard";
    }




    // ================= BUS LIST =================
    @GetMapping("/buses")
    public String buses(HttpSession session, Model model) {

        if (session.getAttribute("admin") == null) {
            return "redirect:/";
        }

        model.addAttribute("buses", busRepo.findAll());
        return "admin-buses";
    }

    // ================= ADD BUS =================
    @GetMapping("/add-bus")
    public String addBusPage(HttpSession session, Model model) {

        if (session.getAttribute("admin") == null) {
            return "redirect:/";
        }

        model.addAttribute("bus", new Bus());
        return "admin-add-bus";
    }

    @PostMapping("/save-bus")
    public String saveBus(@ModelAttribute Bus bus) {
        busRepo.save(bus);
        return "redirect:/admin/buses";
    }

    // ================= DELETE BUS =================
    @GetMapping("/delete-bus/{id}")
    public String deleteBus(@PathVariable Long id) {
        busRepo.deleteById(id);
        return "redirect:/admin/buses";
    }

    // ================= ADMIN LOGIN =================
    @PostMapping("/login")
    public String adminLogin(
            @RequestParam String username,
            @RequestParam String password,
            HttpSession session) {

        Admin admin =
                adminRepo.findByUsernameAndPassword(username, password);

        if (admin == null) {
            return "redirect:/?adminError";
        }

        session.setAttribute("admin", admin);
        return "redirect:/admin/dashboard";
    }

    // ================= ADMIN LOGOUT =================
    @GetMapping("/logout")
    public String adminLogout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
 // ================= VIEW USERS =================
    @GetMapping("/users")
    public String viewUsers(HttpSession session, Model model) {

        if (session.getAttribute("admin") == null) {
            return "redirect:/";
        }

        model.addAttribute("users", userRepo.findAll());
        return "admin-users";
    }
 // ================= DELETE USER =================
    @GetMapping("/delete-user/{id}")
    public String deleteUser(
            @PathVariable Long id,
            HttpSession session) {

        if (session.getAttribute("admin") == null) {
            return "redirect:/";
        }

        userRepo.deleteById(id);
        return "redirect:/admin/users";
    }
    @GetMapping("/admin/bookings")
    public String viewAllBookings(Model model) {

        List<Booking> bookings = bookingRepo.findAllByOrderByIdDesc();
        model.addAttribute("bookings", bookings);

        return "admin-bookings";
    }
 // ================= UPDATE BUS =================
    @PostMapping("/update-bus")
    public String updateBus(
            @ModelAttribute Bus bus,
            HttpSession session) {

        if (session.getAttribute("admin") == null) {
            return "redirect:/";
        }

        // 🔥 This will UPDATE because id is present
        busRepo.save(bus);

        return "redirect:/admin/buses?updated";
    }
    
    @GetMapping("/admin/buses")
    public String adminBuses(Model model) {

        List<Bus> buses = busRepo.findAll();

        Map<Long, Long> bookedSeatsMap = new HashMap<>();
        Map<Long, Long> remainingSeatsMap = new HashMap<>();

        for (Bus bus : buses) {

            long bookedSeats =
                    bookingRepo.countBookedSeatsByBusId(bus.getId());

            long remainingSeats =
                    bus.getTotalSeats() - bookedSeats;

            bookedSeatsMap.put(bus.getId(), bookedSeats);
            remainingSeatsMap.put(bus.getId(), remainingSeats);
        }

        // ⭐⭐⭐ VERY IMPORTANT ⭐⭐⭐
        model.addAttribute("buses", buses);
        model.addAttribute("bookedSeatsMap", bookedSeatsMap);
        model.addAttribute("remainingSeatsMap", remainingSeatsMap);

        return "admin-buses";
    }


}
