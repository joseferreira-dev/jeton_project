package br.com.cremepe.jeton.advice;

import br.com.cremepe.jeton.dto.ErroApiDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // Verifica se é endpoint da API REST
        if (uri.startsWith("/api/")) {
            return true;
        }
        // Opcional: verificar cabeçalho Accept
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }

    @ExceptionHandler(RuntimeException.class)
    public Object handleRuntimeException(RuntimeException ex, HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        String mensagem = ex.getMessage() != null ? ex.getMessage() : "Erro inesperado no servidor.";
        if (isApiRequest(request)) {
            ErroApiDTO erro = new ErroApiDTO(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Erro interno",
                    mensagem,
                    request.getRequestURI());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
        } else {
            redirectAttributes.addFlashAttribute("erro", mensagem);
            return "redirect:" + getRedirectUrl(request);
        }
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Object handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        String mensagem = "Violação de integridade dos dados. Verifique se há registros dependentes.";
        // Extrai mensagem mais amigável se possível
        Throwable rootCause = ex.getRootCause();
        if (rootCause != null && rootCause.getMessage() != null) {
            if (rootCause.getMessage().contains("Duplicate entry")) {
                mensagem = "Já existe um registro com este valor único.";
            } else if (rootCause.getMessage().contains("foreign key constraint fails")) {
                mensagem = "Não é possível excluir/alterar este registro pois existem vínculos com outros registros.";
            } else {
                mensagem = "Erro de integridade: " + rootCause.getMessage();
            }
        }
        if (isApiRequest(request)) {
            ErroApiDTO erro = new ErroApiDTO(
                    HttpStatus.CONFLICT.value(),
                    "Conflito de dados",
                    mensagem,
                    request.getRequestURI());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(erro);
        } else {
            redirectAttributes.addFlashAttribute("erro", mensagem);
            return "redirect:" + getRedirectUrl(request);
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        Map<String, String> erros = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            erros.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        String mensagem = "Erro de validação nos campos enviados.";
        if (isApiRequest(request)) {
            ErroApiDTO erro = new ErroApiDTO(
                    HttpStatus.BAD_REQUEST.value(),
                    "Requisição inválida",
                    mensagem,
                    request.getRequestURI());
            erro.setErrosValidacao(erros);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
        } else {
            redirectAttributes.addFlashAttribute("erro", mensagem);
            redirectAttributes.addFlashAttribute("errosValidacao", erros);
            return "redirect:" + getRedirectUrl(request);
        }
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Object handleNotFound(NoResourceFoundException ex, HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        if (isApiRequest(request)) {
            ErroApiDTO erro = new ErroApiDTO(
                    HttpStatus.NOT_FOUND.value(),
                    "Recurso não encontrado",
                    "O recurso solicitado não existe.",
                    request.getRequestURI());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro);
        } else {
            return "erro/404";
        }
    }

    private String getRedirectUrl(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            return referer;
        }
        return "/index";
    }
}