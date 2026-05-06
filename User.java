package com.bhairavnath.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.bhairavnath.Repository.BusRepository;
import com.bhairavnath.entity.Bus;
import com.bhairavnath.entity.User;

import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

    @Autowired
    private BusRepository busRepo;

    @GetMapping("/")
    public String home(Model model, HttpSession session) {

        User user = (User) session.getAttribute("user");

        model.addAttribute("loggedIn", user != null);
        model.addAttribute("user", user);

        model.addAttribute("cities",
                List.of("Pune", "Mumbai", "Satara", "Patan"));
        model.addAttribute("passengers",
                List.of(1,2,3,4,5));

        return "index";
    }

    @GetMapping("/search")
    public String search(@RequestParam String from,
                         @RequestParam String to,
                         @RequestParam String date,
                         @RequestParam int passenger,
                         Model model,
                         HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            return "redirect:/?loginRequired";
        }

        List<Bus> buses = busRepo.findByFromCityAndToCity(from, to);

        model.addAttribute("buses", buses);
        model.addAttribute("loggedIn", true);
        model.addAttribute("user", user);

        return "bus-list";
    }


    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
    @GetMapping("/services")
    public String services() {
        return "services";
    }
    @Controller
    public class PageController {

        @GetMapping("/plan-journey")
        public String planJourneyPage() {
            return "plan-journey"; // templates/plan-journey.html
        }
    }


}
