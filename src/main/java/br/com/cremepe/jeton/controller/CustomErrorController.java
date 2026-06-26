package br.com.cremepe.jeton.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        // Obtém o código de status
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object errorMessage = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

        int statusCode = 500;
        if (status != null) {
            statusCode = Integer.parseInt(status.toString());
        }

        // Adiciona atributos para a view genérica
        model.addAttribute("status", statusCode);
        model.addAttribute("error", HttpStatus.valueOf(statusCode).getReasonPhrase());
        model.addAttribute("message", errorMessage != null ? errorMessage.toString() : "");

        // Retorna a view específica, se existir, senão a genérica
        switch (statusCode) {
            case 400:
                return "error/400";
            case 403:
                return "error/403";
            case 404:
                return "error/404";
            case 405:
                return "error/405";
            case 500:
                return "error/500";
            default:
                return "error/error";
        }
    }
}