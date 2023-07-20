package com.example.sqlgeneratory.controller;

import com.example.sqlgeneratory.exception.SQLGenerationException;
import com.example.sqlgeneratory.service.SQLGenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Controller
public class HomeController {

    private final SQLGenerationService sqlGenerationService;

    @Autowired
    public HomeController(SQLGenerationService sqlGenerationService) {
        this.sqlGenerationService = sqlGenerationService;
    }

    @GetMapping("/home")
    public String home(Model model) {
        System.out.println("in home");
        // Add current date to the model
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
        String currentDate = dateFormat.format(new Date());
        model.addAttribute("currentDate", currentDate);
        return "index";
    }

    @PostMapping("/generate-sql")
    public ResponseEntity<ByteArrayResource> generateSQL(@RequestParam("excelFile") MultipartFile excelFile,
                                                         @RequestParam("srNumber") String srNumber,
                                                         Model model) {
        try {
            System.out.println("in generateSQL");
            // Validate Excel file name
            boolean isToday = validateExcelFileName(excelFile.getOriginalFilename());

          //  if (!isToday) {
             //   String warningMessage = "Warning: The Excel file name doesn't start with today's date.";
            //    model.addAttribute("warningMessage", warningMessage);
            //    return ResponseEntity.badRequest().build();
          //  } else {
                // Generate SQL
                byte[] sqlBytes = sqlGenerationService.generateSQL(excelFile, srNumber);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                headers.setContentDispositionFormData("attachment", "PSCPEN_SR_"+ srNumber + ".zip");

                ByteArrayResource resource = new ByteArrayResource(sqlBytes);

                return ResponseEntity.ok()
                        .headers(headers)
                        .contentLength(sqlBytes.length)
                        .body(resource);
        //    }
        } catch (SQLGenerationException e) {
            String errorMessage = "Error generating SQL files: " + e.getMessage();
            model.addAttribute("errorMessage", errorMessage);
            return ResponseEntity.badRequest().build();
        }
    }

    private boolean validateExcelFileName(String fileName) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
        String currentDate = dateFormat.format(new Date());
        return fileName.startsWith(currentDate);
    }
}
