package com.bhairavnath.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.bhairavnath.Repository.BookingRepository;
import com.bhairavnath.Repository.BusRepository;
import com.bhairavnath.entity.Booking;
import com.bhairavnath.entity.Bus;
import com.bhairavnath.entity.User;
import com.bhairavnath.service.EmailService;

import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;

import jakarta.servlet.http.HttpServletResponse;

import jakarta.servlet.http.HttpSession;

@Controller
public class BookingController {

	@Autowired
	private BookingRepository bookingRepo;

	@Autowired
	private BusRepository busRepo;

	@Autowired
	private EmailService emailService;

	// ================= TEMP SAVE BEFORE PAYMENT =================
	@PostMapping("/store-booking-session")
	@ResponseBody
	public void storeBookingSession(@RequestParam Long busId, @RequestParam String seatNo, @RequestParam String gender,
			@RequestParam(required = false) String date, HttpSession session) {

		LocalDate journeyDate;

		if (date == null || date.isBlank()) {
			journeyDate = LocalDate.now(); // ✅ fallback
		} else {
			journeyDate = LocalDate.parse(date);
		}

		session.setAttribute("busId", busId);
		session.setAttribute("seatNo", seatNo + " (" + gender + ")");
		session.setAttribute("journeyDate", journeyDate);
	}

	// ================= PAYMENT SUCCESS =================
	@GetMapping("/payment-success")
	public String paymentSuccess(HttpSession session) {

		User user = (User) session.getAttribute("user");
		if (user == null)
			return "redirect:/?loginRequired";

		Long busId = (Long) session.getAttribute("busId");
		String seatNo = (String) session.getAttribute("seatNo");
		LocalDate date = (LocalDate) session.getAttribute("journeyDate");

		if (busId == null || seatNo == null || date == null) {
			return "redirect:/";
		}

		Bus bus = busRepo.findById(busId).orElseThrow();

		Booking booking = new Booking();
		booking.setUser(user);
		booking.setBus(bus);
		booking.setFromCity(bus.getFromCity());
		booking.setToCity(bus.getToCity());
		booking.setSeatNo(seatNo);
		booking.setJourneyDate(date);
		booking.setPrice(bus.getPrice());
		booking.setStatus("BOOKED");

		bookingRepo.save(booking);

		// 📧 SEND CONFIRMATION EMAIL
		emailService.sendTicketEmail(booking);

		session.removeAttribute("busId");
		session.removeAttribute("seatNo");
		session.removeAttribute("journeyDate");

		return "redirect:/my-bookings";
	}

	// ================= USER BOOKINGS =================
	@GetMapping("/my-bookings")
	public String myBookings(HttpSession session, Model model) {

		User user = (User) session.getAttribute("user");
		if (user == null)
			return "redirect:/?loginRequired";

		model.addAttribute("bookings", bookingRepo.findByUser(user));
		model.addAttribute("loggedIn", true);
		model.addAttribute("user", user);

		return "my-bookings";
	}

	// ================= CANCEL BOOKING =================
	@GetMapping("/cancel-booking/{id}")
	public String cancelBooking(@PathVariable Long id) {

		Booking booking = bookingRepo.findById(id).orElseThrow();
		booking.setStatus("CANCELLED");
		bookingRepo.save(booking);

		return "redirect:/my-bookings";
	}

	// ================= ADMIN → ALL BOOKINGS =================
	@GetMapping("/admin/bookings")
	public String allBookings(Model model) {

		model.addAttribute("bookings", bookingRepo.findAll());
		return "admin-bookings";
	}

	// ================= SEAT CHECK API =================
	@GetMapping("/check-seat")
	@ResponseBody
	public boolean checkSeatAvailability(@RequestParam Long busId, @RequestParam String seatNo,
			@RequestParam(required = false) String date) {

		LocalDate journeyDate = (date == null || date.isBlank()) ? LocalDate.now() : LocalDate.parse(date);

		Bus bus = busRepo.findById(busId).orElseThrow();

		return bookingRepo.existsByBusAndJourneyDateAndSeatNo(bus, journeyDate, seatNo);
	}

	// ================= SEAT DATA API =================
	@GetMapping("/seat-data")
	@ResponseBody
	public Map<String, Object> seatData(@RequestParam Long busId, @RequestParam(required = false) String date) {

		LocalDate journeyDate = (date == null || date.isBlank()) ? LocalDate.now() : LocalDate.parse(date);

		Bus bus = busRepo.findById(busId).orElseThrow();

		List<String> bookedSeats = bookingRepo.findBookedSeats(bus, journeyDate);

		Map<String, Object> res = new HashMap<>();
		res.put("totalSeats", bus.getTotalSeats());
		res.put("bookedSeats", bookedSeats);

		return res;
	}

	// ================= PAYMENT PAGE =================
	@GetMapping("/payment")
	public String paymentPage(HttpSession session, Model model) {

		Long busId = (Long) session.getAttribute("busId");
		String seatNo = (String) session.getAttribute("seatNo");
		LocalDate date = (LocalDate) session.getAttribute("journeyDate");

		if (busId == null || seatNo == null || date == null) {
			return "redirect:/";
		}

		Bus bus = busRepo.findById(busId).orElseThrow();

		model.addAttribute("bus", bus);
		model.addAttribute("seatNo", seatNo);
		model.addAttribute("journeyDate", date);

		return "payment";
	}

	@GetMapping("/download-ticket/{id}")
	public void downloadTicket(@PathVariable Long id, HttpServletResponse response) throws Exception {

		Booking b = bookingRepo.findById(id).orElseThrow();

		response.setContentType("application/pdf");
		response.setHeader("Content-Disposition", "attachment; filename=ticket_" + id + ".pdf");

		PdfWriter writer = new PdfWriter(response.getOutputStream());
		PdfDocument pdf = new PdfDocument(writer);
		Document doc = new Document(pdf);

		doc.add(new Paragraph("BHAIRONATH BUS TICKET").setBold().setFontSize(18));

		doc.add(new Paragraph(" "));
		doc.add(new Paragraph("Passenger: " + b.getUser().getName()));
		doc.add(new Paragraph("Email: " + b.getUser().getEmail()));
		doc.add(new Paragraph("Bus: " + b.getBus().getName()));
		doc.add(new Paragraph("Route: " + b.getFromCity() + " → " + b.getToCity()));
		doc.add(new Paragraph("Journey Date: " + b.getJourneyDate()));
		doc.add(new Paragraph("Seat No: " + b.getSeatNo()));
		doc.add(new Paragraph("Amount: ₹ " + b.getPrice()));
		doc.add(new Paragraph("Status: " + b.getStatus()));

		doc.close();
	}

	@GetMapping("/booking-details/{id}")
	public String bookingDetails(@PathVariable Long id, Model model) {

		Booking booking = bookingRepo.findById(id).orElse(null);
		model.addAttribute("b", booking);

		return "booking-details";
	}

@GetMapping("/admin/manage-buses")
public String manageBuses(Model model) {

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

    model.addAttribute("buses", buses);
    model.addAttribute("bookedSeatsMap", bookedSeatsMap);
    model.addAttribute("remainingSeatsMap", remainingSeatsMap);

    return "admin-buses";
    }
}
