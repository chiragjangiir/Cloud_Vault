// src/main/java/com/example/cloudstorage/controller/FileController.java
package com.example.cloudstorage.controller;

import com.example.cloudstorage.model.FileMetadata;
import com.example.cloudstorage.model.User;
import com.example.cloudstorage.service.StorageService;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

// Controller for file operations (upload, list, download, delete).
@Controller
public class FileController {

    private final StorageService storageService;

    public FileController(StorageService storageService) {
        this.storageService = storageService;
    }

    // Displays the dashboard with uploaded files.
    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please login to access the dashboard.");
            return "redirect:/"; // Redirect to login if not authenticated
        }
        model.addAttribute("loggedInUser", loggedInUser);
        // Get files owned by the logged-in user
        List<FileMetadata> files = storageService.getFilesByUser(loggedInUser);
        model.addAttribute("files", files);
        return "dashboard";
    }

    // Handles file upload.
    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please login to upload files.");
            return "redirect:/";
        }

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a file to upload.");
            return "redirect:/dashboard";
        }

        try {
            storageService.store(file, loggedInUser);
            redirectAttributes.addFlashAttribute("successMessage",
                    "You successfully uploaded " + file.getOriginalFilename() + "!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Failed to upload " + file.getOriginalFilename() + ": " + e.getMessage());
        }
        return "redirect:/dashboard";
    }

    // Handles file download.
    @GetMapping("/download/{uniqueFilename}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String uniqueFilename,
                                                 HttpSession session,
                                                 RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            // This redirect won't work for ResponseEntity, but good for logging/handling
            // In a real app, this would be handled by Spring Security or a custom interceptor
            System.err.println("Attempted download by unauthenticated user.");
            return ResponseEntity.status(401).build(); // Unauthorized
        }

        try {
            Resource file = storageService.loadAsResource(uniqueFilename);
            // In a real app, you'd verify if the loggedInUser has access to this file
            // For simplicity, we assume if you know the uniqueFilename, you can download.
            // A more secure approach would be to pass fileId and look up metadata.

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + file.getFilename() + "\"")
                    .body(file);
        } catch (Exception e) {
            System.err.println("Error downloading file: " + e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // Handles file deletion.
    @PostMapping("/delete/{fileId}")
    public String deleteFile(@PathVariable Long fileId,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please login to delete files.");
            return "redirect:/";
        }

        try {
            storageService.delete(fileId, loggedInUser);
            redirectAttributes.addFlashAttribute("successMessage", "File deleted successfully!");
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Authorization error: " + e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete file: " + e.getMessage());
        }
        return "redirect:/dashboard";
    }
}