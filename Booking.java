package com.bhairavnath.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bhairavnath.Repository.UserRepository;
import com.bhairavnath.entity.User;

import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepo;

    // ================= LOGIN =================
    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpSession session) {

        User user = userRepo.findByEmailAndPassword(email, password);

        if (user != null) {
            session.setAttribute("user", user);
            return "redirect:/";
        }

        return "redirect:/?loginError";
    }

    // ================= REGISTER =================
    @PostMapping("/register")
    public String register(@ModelAttribute User user,
                           RedirectAttributes redirectAttributes) {

        User existingUser = userRepo.findByEmail(user.getEmail());

        if (existingUser != null) {
            redirectAttributes.addFlashAttribute(
                "registerError",
                "Email already registered"
            );
            return "redirect:/";
        }

        // ❗ only if field exists
        // user.setActive(true);

        userRepo.save(user);

        redirectAttributes.addFlashAttribute(
            "registerSuccess",
            "Registration successful"
        );

        return "redirect:/";
    }

    // ================= UPDATE PROFILE =================
    @PostMapping("/update-profile")
    public String updateProfile(@ModelAttribute User user,
                                HttpSession session) {

        User loggedUser = (User) session.getAttribute("user");

        loggedUser.setName(user.getName());
        loggedUser.setEmail(user.getEmail());

        userRepo.save(loggedUser);
        session.setAttribute("user", loggedUser);

        return "redirect:/my-bookings";
    }
}
