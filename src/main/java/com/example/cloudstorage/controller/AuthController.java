// src/main/java/com/example/cloudstorage/controller/AuthController.java
package com.example.cloudstorage.controller;

import com.example.cloudstorage.model.User;
import com.example.cloudstorage.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

// Controller for user authentication (registration and login).
@Controller
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Displays the login/registration page.
    @GetMapping("/")
    public String index(Model model, HttpSession session) {
        // If a user is already logged in, redirect to dashboard
        if (session.getAttribute("loggedInUser") != null) {
            return "redirect:/dashboard";
        }
        model.addAttribute("user", new User()); // For registration form
        return "index";
    }

    // Handles user registration.
    @PostMapping("/register")
    public String registerUser(@RequestParam String username,
                               @RequestParam String password,
                               RedirectAttributes redirectAttributes) {
        // Check if username already exists
        if (userRepository.findByUsername(username).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Username already exists!");
            return "redirect:/";
        }

        // In a real application, hash the password before saving!
        User newUser = new User(username, password);
        userRepository.save(newUser);
        redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please login.");
        return "redirect:/";
    }

    // Handles user login.
    @PostMapping("/login")
    public String loginUser(@RequestParam String username,
                            @RequestParam String password,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            // In a real application, compare hashed passwords!
            if (user.getPassword().equals(password)) {
                session.setAttribute("loggedInUser", user); // Store user in session
                return "redirect:/dashboard";
            }
        }
        redirectAttributes.addFlashAttribute("errorMessage", "Invalid username or password.");
        return "redirect:/";
    }

    // Handles user logout.
    @GetMapping("/logout")
    public String logoutUser(HttpSession session) {
        session.invalidate(); // Invalidate the session
        return "redirect:/";
    }
}