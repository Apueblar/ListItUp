package com.listitup.api.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();
        
        if (status != null) {
            try {
                statusCode = Integer.parseInt(status.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        
        String errorMsg = "Something went wrong";
        try {
            HttpStatus httpStatus = HttpStatus.valueOf(statusCode);
            errorMsg = httpStatus.getReasonPhrase();
        } catch (IllegalArgumentException ignored) {
        }

        model.addAttribute("status", statusCode);
        model.addAttribute("error", errorMsg);
        return "error";
    }
}
