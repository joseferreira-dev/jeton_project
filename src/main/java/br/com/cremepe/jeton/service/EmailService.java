package br.com.cremepe.jeton.service;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarSenhaProvisoria(String destinatario, String nome, String senhaProvisoria) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(destinatario);
            helper.setSubject("Jeton - Nova senha provisória");
            helper.setText(
                    String.format("""
                            Olá, %s!

                            Você solicitou a recuperação de senha do sistema Jeton.
                            Sua nova senha provisória é:

                            %s

                            Acesse o sistema com essa senha e, em seguida, altere-a no seu perfil.

                            Atenciosamente,
                            Equipe Jeton
                            """, nome, senhaProvisoria));
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Erro ao enviar e-mail: " + e.getMessage(), e);
        }
    }
}
