package com.bhairavnath.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.bhairavnath.Repository.HelpFaqRepository;
import com.bhairavnath.entity.HelpFaq;

@Controller
public class HelpController {

    @Autowired
    private HelpFaqRepository faqRepo;

    // USER HELP PAGE
    @GetMapping("/help")
    public String helpPage(Model model) {
        model.addAttribute("faqs", faqRepo.findAll());
        return "help";
    }

    // ADMIN ADD FAQ
    @PostMapping("/admin/save-faq")
    public String saveFaq(@ModelAttribute HelpFaq faq) {
        faqRepo.save(faq);
        return "redirect:/admin/help";
    }

    // ADMIN PAGE
    @GetMapping("/admin/help")
    public String adminHelp(Model model) {
        model.addAttribute("faqs", faqRepo.findAll());
        model.addAttribute("faq", new HelpFaq());
        return "admin-help";
    }

    // DELETE FAQ
    @GetMapping("/admin/delete-faq/{id}")
    public String deleteFaq(@PathVariable Long id) {
        faqRepo.deleteById(id);
        return "redirect:/admin/help";
    }
}
