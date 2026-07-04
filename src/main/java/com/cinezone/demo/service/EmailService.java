package com.cinezone.demo.service;

import com.cinezone.demo.model.entity.Booking;
import com.cinezone.demo.model.entity.Ticket;
import com.cinezone.demo.model.entity.BookingSnack;
import com.cinezone.demo.model.entity.Complaint;
import com.cinezone.demo.repository.TicketRepository;
import com.cinezone.demo.repository.BookingSnackRepository;
import com.cinezone.demo.util.QrGeneratorUtil;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.core.io.ByteArrayResource;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TicketRepository ticketRepository;
    private final BookingSnackRepository bookingSnackRepository;
    private final QrGeneratorUtil qrGeneratorUtil;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.username}")
    private String fromEmail;

    public void sendTicketEmail(Booking booking) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String toEmail = booking.getUser() != null ? booking.getUser().getCorreo() : "noreply@cinezone.com";
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("¡Tu entrada para CineZone está lista! - " + booking.getCodigoUnico());

            String htmlContent = generateHtmlTicket(booking);
            helper.setText(htmlContent, true);

            // Generate QR Bytes and attach as inline
            String nroCompra = booking.getCodigoUnico().toString();
            String pelicula = booking.getShowtime() != null ? booking.getShowtime().getMovie().getTitulo() : "Sólo Dulcería";
            List<Ticket> tickets = ticketRepository.findByBookingId(booking.getId());
            String butacasStr = tickets.stream().map(t -> String.valueOf(t.getSeat().getFila()) + t.getSeat().getNumero()).collect(Collectors.joining(", "));
            if (butacasStr.isEmpty()) butacasStr = "Sin butacas";
            String qrContent = String.format("{\"boleta\":\"%s\", \"info\":\"%s\", \"asientos\":\"%s\"}", nroCompra, pelicula, butacasStr);
            byte[] qrBytes = qrGeneratorUtil.generateQrCodeBytes(qrContent);
            helper.addInline("qrCode", new ByteArrayResource(qrBytes), "image/png");

            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
            System.err.println("Error al enviar el correo a la boleta: " + booking.getCodigoUnico());
        }
    }

    private String generateHtmlTicket(Booking booking) {
        // Obtenemos los tickets y snacks
        List<Ticket> tickets = ticketRepository.findByBookingId(booking.getId());
        List<BookingSnack> snacks = bookingSnackRepository.findByBookingId(booking.getId());

        String pelicula = booking.getShowtime() != null ? booking.getShowtime().getMovie().getTitulo() : "Sólo Dulcería";
        String nroCompra = booking.getCodigoUnico().toString();
        String cliente = booking.getUser() != null ? booking.getUser().getNombre() + " " + booking.getUser().getApellido() : "Cliente Cinezone";
        String sede = booking.getShowtime() != null ? booking.getShowtime().getAuditorium().getCinema().getNombre() : "CineZone";
        String fecha = booking.getShowtime() != null ? booking.getShowtime().getFechaHora().toLocalDate().toString() : "-";
        String hora = booking.getShowtime() != null ? booking.getShowtime().getFechaHora().toLocalTime().toString() : "-";
        String sala = booking.getShowtime() != null ? booking.getShowtime().getAuditorium().getNombre() : "-";

        String butacasStr = tickets.stream().map(t -> String.valueOf(t.getSeat().getFila()) + t.getSeat().getNumero()).collect(Collectors.joining(", "));
        if (butacasStr.isEmpty()) butacasStr = "Sin butacas";

        // Generar QR Tag via CID
        String qrImgTag = "<img src=\"cid:qrCode\" style=\"width:100%; height:100%; object-fit:contain;\" />";

        // Construir filas de entradas
        StringBuilder entradasHtml = new StringBuilder();
        double subtotalEntradas = 0;
        for (Ticket t : tickets) {
            entradasHtml.append("<div class=\"item-row\">")
                    .append("<span class=\"item-desc\">Entrada ").append(t.getTipoEntrada()).append("</span>")
                    .append("<span class=\"item-qty\">Cant. 1</span>")
                    .append("<span class=\"item-price\">S/ ").append(t.getPrecioPagado()).append("</span>")
                    .append("</div>");
            subtotalEntradas += t.getPrecioPagado().doubleValue();
        }

        // Construir filas de snacks
        StringBuilder snacksHtml = new StringBuilder();
        double subtotalSnacks = 0;
        for (BookingSnack s : snacks) {
            snacksHtml.append("<div class=\"item-row\">")
                    .append("<span class=\"item-desc\">").append(s.getProduct().getNombre()).append("</span>")
                    .append("<span class=\"item-qty\">Cant. ").append(s.getCantidad()).append("</span>")
                    .append("<span class=\"item-price\">S/ ").append(s.getPrecioTotal()).append("</span>")
                    .append("</div>");
            subtotalSnacks += s.getPrecioTotal().doubleValue();
        }

        // HTML Base (Sin JS, con valores inyectados directamente)
        return "<!DOCTYPE html>\n" +
                "<html lang=\"es\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\" />\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n" +
                "  <title>Boleta CineZone</title>\n" +
                "  <style>\n" +
                "    * { box-sizing: border-box; margin: 0; padding: 0; }\n" +
                "    body { background: #1a1a1a; font-family: Arial, Helvetica, sans-serif; display: flex; justify-content: center; padding: 30px 16px; }\n" +
                "    .ticket { background: #fff; width: 480px; border-radius: 6px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,.5); margin: 0 auto; }\n" +
                "    .header { background: #111; padding: 20px 28px 18px; text-align: center; border-bottom: 3px solid #c8860a; }\n" +
                "    .movie-title { font-size: 22px; font-weight: 900; color: #fff; margin-bottom: 6px; letter-spacing: 0.3px; }\n" +
                "    .order-number { font-size: 13px; color: #aaa; }\n" +
                "    .order-number strong { color: #c8860a; }\n" +
                "    .qr-section { padding: 22px 28px; display: flex; justify-content: center; border-bottom: 1px solid #e8e8e8; background: #fafafa; }\n" +
                "    .qr-placeholder { width: 160px; height: 160px; border: 2px dashed #bbb; border-radius: 6px; display: flex; align-items: center; justify-content: center; flex-direction: column; padding: 12px; }\n" +
                "    .notice-wrap { padding: 14px 20px 0; }\n" +
                "    .notice { background: #fff8f0; border: 1px solid #f0d090; border-radius: 6px; padding: 10px 14px; display: flex; gap: 10px; font-size: 12px; color: #555; }\n" +
                "    .client-section { padding: 18px 28px 14px; text-align: center; border-bottom: 1px solid #e8e8e8; }\n" +
                "    .client-name { font-size: 15px; font-weight: 700; color: #111; }\n" +
                "    .session-grid { display: grid; grid-template-columns: 1fr 1fr; padding: 14px 28px; gap: 12px 8px; border-bottom: 2px solid #e8e8e8; background: #fafafa; }\n" +
                "    .session-item { display: flex; align-items: center; gap: 8px; }\n" +
                "    .session-item .label { font-size: 10px; color: #888; display: block; text-transform: uppercase; }\n" +
                "    .session-item .value { font-size: 13px; font-weight: 700; color: #111; }\n" +
                "    .sala-badge { background: #111; color: #c8860a; font-size: 11px; font-weight: 700; padding: 2px 10px; border-radius: 3px; display: inline-block; border: 1px solid #c8860a; }\n" +
                "    .seats-section { padding: 12px 28px; border-bottom: 1px solid #e8e8e8; display: flex; align-items: center; gap: 10px; }\n" +
                "    .seats-text { font-size: 13px; color: #111; }\n" +
                "    .items-section { padding: 0 28px; }\n" +
                "    .items-category { padding: 12px 0 4px; display: flex; align-items: center; gap: 8px; border-bottom: 1px solid #f0f0f0; }\n" +
                "    .cat-name { font-size: 14px; font-weight: 700; color: #111; }\n" +
                "    .item-row { display: flex; justify-content: space-between; padding: 8px 0; font-size: 13px; color: #333; border-bottom: 1px solid #f5f5f5; }\n" +
                "    .subtotal-row { display: flex; justify-content: flex-end; padding: 8px 0; font-size: 13px; font-weight: 700; color: #111; border-bottom: 1px solid #e8e8e8; }\n" +
                "    .total-section { padding: 14px 28px; display: flex; justify-content: flex-end; align-items: center; gap: 16px; border-bottom: 2px solid #111; }\n" +
                "    .total-amount { font-size: 24px; font-weight: 900; color: #c8860a; }\n" +
                "    .footer { background: #111; border-top: 3px solid #c8860a; padding: 10px 28px; font-size: 11px; color: #aaa; text-align: center; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<div class=\"ticket\">\n" +
                "  <div class=\"header\">\n" +
                "    <div class=\"movie-title\">" + pelicula + "</div>\n" +
                "    <div class=\"order-number\">Nro. de Compra: <strong>" + nroCompra + "</strong></div>\n" +
                "  </div>\n" +
                "  <div class=\"qr-section\">\n" +
                "    <div class=\"qr-placeholder\">\n" +
                "      " + qrImgTag + "\n" +
                "    </div>\n" +
                "  </div>\n" +
                "  <div class=\"client-section\">\n" +
                "    <div class=\"client-name\">" + cliente + "</div>\n" +
                "  </div>\n" +
                "  <div class=\"session-grid\">\n" +
                "    <div class=\"session-item\">\n" +
                "      <div><span class=\"label\">Sede</span><span class=\"value\">" + sede + "</span></div>\n" +
                "    </div>\n" +
                "    <div class=\"session-item\">\n" +
                "      <div><span class=\"label\">Fecha</span><span class=\"value\">" + fecha + "</span></div>\n" +
                "    </div>\n" +
                "    <div class=\"session-item\">\n" +
                "      <div><span class=\"label\">Hora</span><span class=\"value\">" + hora + "</span></div>\n" +
                "    </div>\n" +
                "    <div class=\"session-item\">\n" +
                "      <div><span class=\"label\">Sala</span><span class=\"sala-badge\">" + sala + "</span></div>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "  <div class=\"seats-section\">\n" +
                "    <div class=\"seats-text\"><strong>Tus butacas:</strong> " + butacasStr + "</div>\n" +
                "  </div>\n" +
                "  <div class=\"items-section\">\n" +
                "    <div class=\"items-category\"><span class=\"cat-name\">Entradas</span></div>\n" +
                "    " + entradasHtml.toString() + "\n" +
                "    <div class=\"subtotal-row\">SubTotal: S/ " + String.format("%.2f", subtotalEntradas) + "</div>\n" +
                "    <div class=\"items-category\" style=\"margin-top:4px;\"><span class=\"cat-name\">Dulcería</span></div>\n" +
                "    " + snacksHtml.toString() + "\n" +
                "    <div class=\"subtotal-row\">SubTotal: S/ " + String.format("%.2f", subtotalSnacks) + "</div>\n" +
                "  </div>\n" +
                "  <div class=\"total-section\">\n" +
                "    <span class=\"total-label\">Total :</span>\n" +
                "    <span class=\"total-amount\">S/ " + String.format("%.2f", booking.getMontoTotal()) + "</span>\n" +
                "  </div>\n" +
                "  <div class=\"footer\">CineZone S.A · Perú</div>\n" +
                "</div>\n" +
                "</body>\n" +
                "</html>";
    }

    public void sendComplaintReplyEmail(Complaint complaint) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(complaint.getEmail());
            helper.setSubject("Respuesta a su " + complaint.getTipoReclamo() + " - CineZone");

            String htmlContent = generateHtmlComplaintReply(complaint);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
            System.err.println("Error al enviar el correo de respuesta al reclamo: " + complaint.getId());
        }
    }

    private String generateHtmlComplaintReply(Complaint complaint) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"es\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\" />\n" +
                "  <style>\n" +
                "    body { font-family: Arial, sans-serif; background-color: #f4f4f5; color: #18181b; padding: 20px; }\n" +
                "    .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }\n" +
                "    .header { background-color: #020617; padding: 20px; text-align: center; color: #ffffff; }\n" +
                "    .header h1 { margin: 0; font-size: 24px; font-weight: 900; }\n" +
                "    .header h1 span { color: #f59e0b; }\n" +
                "    .content { padding: 30px; line-height: 1.6; }\n" +
                "    .content p { margin-bottom: 15px; }\n" +
                "    .response-box { background-color: #fef3c7; border-left: 4px solid #f59e0b; padding: 15px; margin: 20px 0; border-radius: 0 8px 8px 0; }\n" +
                "    .footer { background-color: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #64748b; border-top: 1px solid #e2e8f0; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class=\"container\">\n" +
                "    <div class=\"header\">\n" +
                "      <h1>CINE<span>ZONE</span></h1>\n" +
                "    </div>\n" +
                "    <div class=\"content\">\n" +
                "      <p>Estimado/a <strong>" + complaint.getNombreCompleto() + "</strong>,</p>\n" +
                "      <p>Hemos recibido y revisado su " + complaint.getTipoReclamo().toLowerCase() + " enviado el " + complaint.getFechaReclamo().toLocalDate() + ".</p>\n" +
                "      <p>A continuación, la respuesta de nuestro equipo de atención al cliente:</p>\n" +
                "      <div class=\"response-box\">\n" +
                "        " + (complaint.getRespuestaAdmin() != null ? complaint.getRespuestaAdmin().replace("\n", "<br/>") : "") + "\n" +
                "      </div>\n" +
                "      <p>Si tiene alguna otra consulta o requiere enviar información adicional, por favor responda a este correo.</p>\n" +
                "      <p>Atentamente,<br/><strong>El equipo de CineZone</strong></p>\n" +
                "    </div>\n" +
                "    <div class=\"footer\">\n" +
                "      CineZone Entertainment &copy; 2026. Todos los derechos reservados.<br/>\n" +
                "      Este es un correo automático, por favor no responda directamente a esta dirección.\n" +
                "    </div>\n" +
                "  </div>\n" +
                "</body>\n" +
                "</html>";
    }
    @org.springframework.beans.factory.annotation.Value("${cors.allowed-origins:http://localhost:3000}")
    private String frontendUrl;

    public void sendPasswordResetEmail(String toEmail, String token, String name) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Recuperación de Contraseña - CineZone");

            // URL del frontend dinámico
            String baseUrl = frontendUrl.contains(",") ? frontendUrl.split(",")[0] : frontendUrl;
            String resetUrl = baseUrl + "/reset-password?token=" + token;

            String htmlContent = "<!DOCTYPE html>\n" +
                    "<html lang=\"es\">\n" +
                    "<head>\n" +
                    "  <meta charset=\"UTF-8\" />\n" +
                    "  <style>\n" +
                    "    body { font-family: Arial, sans-serif; background-color: #f4f4f5; color: #18181b; padding: 20px; }\n" +
                    "    .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }\n" +
                    "    .header { background-color: #020617; padding: 20px; text-align: center; color: #ffffff; }\n" +
                    "    .header h1 { margin: 0; font-size: 24px; font-weight: 900; }\n" +
                    "    .header h1 span { color: #f59e0b; }\n" +
                    "    .content { padding: 30px; line-height: 1.6; text-align: center; }\n" +
                    "    .btn { display: inline-block; background-color: #f59e0b; color: #fff; text-decoration: none; padding: 12px 24px; border-radius: 6px; font-weight: bold; margin-top: 20px; }\n" +
                    "  </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "  <div class=\"container\">\n" +
                    "    <div class=\"header\">\n" +
                    "      <h1>CINE<span>ZONE</span></h1>\n" +
                    "    </div>\n" +
                    "    <div class=\"content\">\n" +
                    "      <h2>Hola, " + name + "</h2>\n" +
                    "      <p>Hemos recibido una solicitud para restablecer tu contraseña.</p>\n" +
                    "      <p>Haz clic en el siguiente botón para crear una nueva contraseña:</p>\n" +
                    "      <a href=\"" + resetUrl + "\" class=\"btn\">Restablecer Contraseña</a>\n" +
                    "      <p style=\"margin-top: 20px; font-size: 12px; color: #777;\">Si no solicitaste este cambio, puedes ignorar este correo.</p>\n" +
                    "    </div>\n" +
                    "  </div>\n" +
                    "</body>\n" +
                    "</html>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
            System.err.println("Error al enviar el correo de recuperación a: " + toEmail);
        }
    }
    public void sendVerificationEmail(String toEmail, String code, String name) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Verifica tu correo electrónico");

            String htmlContent = "<!DOCTYPE html>\n" +
                    "<html lang=\"es\">\n" +
                    "<head>\n" +
                    "  <meta charset=\"UTF-8\" />\n" +
                    "  <style>\n" +
                    "    body { font-family: Arial, sans-serif; background-color: #f4f4f5; color: #18181b; padding: 20px; }\n" +
                    "    .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }\n" +
                    "    .header { background-color: #020617; padding: 20px; text-align: center; color: #ffffff; }\n" +
                    "    .header h1 { margin: 0; font-size: 24px; font-weight: 900; }\n" +
                    "    .header h1 span { color: #f59e0b; }\n" +
                    "    .content { padding: 30px; line-height: 1.6; text-align: center; }\n" +
                    "    .code-box { display: inline-block; background-color: #fef3c7; color: #b45309; font-size: 32px; letter-spacing: 5px; font-weight: bold; padding: 15px 30px; border-radius: 8px; margin: 20px 0; border: 2px dashed #f59e0b; }\n" +
                    "  </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "  <div class=\"container\">\n" +
                    "    <div class=\"header\">\n" +
                    "      <h1>CINE<span>ZONE</span></h1>\n" +
                    "    </div>\n" +
                    "    <div class=\"content\">\n" +
                    "      <h2>Hola, " + name + "</h2>\n" +
                    "      <p>Gracias por registrarte en CineZone.</p>\n" +
                    "      <p>Para activar tu cuenta, por favor ingresa el siguiente código de verificación. Este código expirará en 10 minutos.</p>\n" +
                    "      <div class=\"code-box\">" + code + "</div>\n" +
                    "      <p style=\"margin-top: 20px; font-size: 12px; color: #777;\">Si no creaste una cuenta, puedes ignorar este correo.</p>\n" +
                    "    </div>\n" +
                    "  </div>\n" +
                    "</body>\n" +
                    "</html>";

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
            System.err.println("Error al enviar el correo de verificación a: " + toEmail);
        }
    }
}
