package br.com.cremepe.jeton.advice;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class SecurityExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(RedirectAttributes ra) {
        ra.addFlashAttribute("erro", "Você não tem permissão para acessar este recurso.");
        return "redirect:/index";
    }
}