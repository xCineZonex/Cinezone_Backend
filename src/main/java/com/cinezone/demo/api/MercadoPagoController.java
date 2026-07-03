package com.cinezone.demo.api;

import com.cinezone.demo.model.entity.Booking;
import com.cinezone.demo.repository.BookingRepository;
import com.cinezone.demo.service.BookingService;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mercadopago")
@RequiredArgsConstructor
public class MercadoPagoController {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    @org.springframework.beans.factory.annotation.Value("${cors.allowed-origins:http://localhost:3000}")
    private String frontendUrl;

    @PostMapping("/preferencia/{bookingId}")
    public ResponseEntity<?> crearPreferencia(@PathVariable UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Boleta no encontrada"));

        try {
            PreferenceItemRequest itemRequest = PreferenceItemRequest.builder()
                    .id(booking.getId().toString())
                    .title("Entradas Cinezone")
                    .description("Compra de entradas y dulces en Cinezone")
                    .categoryId("entertainment")
                    .quantity(1)
                    .currencyId("PEN")
                    .unitPrice(booking.getMontoTotal())
                    .build();

            // Limpiamos la URL del frontend por si viene con coma o espacios (a veces cors allowed origins tiene varios separados por coma)
            String baseUrl = frontendUrl.split(",")[0].trim();

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(baseUrl + "/checkout/boleta?bookingId=" + booking.getId().toString())
                    .pending(baseUrl + "/checkout/pago?status=pending")
                    .failure(baseUrl + "/checkout/pago?status=failure")
                    .build();

            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(Collections.singletonList(itemRequest))
                    .backUrls(backUrls)
                    .autoReturn("approved")
                    .externalReference(booking.getId().toString())
                    .build();

            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(preferenceRequest);

            Map<String, String> response = new HashMap<>();
            response.put("initPoint", preference.getInitPoint());
            response.put("preferenceId", preference.getId());
            
            return ResponseEntity.ok(response);

        } catch (MPApiException e) {
            String errorMsg = "MercadoPago API Error: " + e.getApiResponse().getContent();
            System.err.println(errorMsg);
            return ResponseEntity.status(500).body(Collections.singletonMap("error", errorMsg));
        } catch (MPException e) {
            String errorMsg = "Error interno de MercadoPago SDK: " + e.getMessage();
            return ResponseEntity.status(500).body(Collections.singletonMap("error", errorMsg));
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestParam(name = "data.id", required = false) String dataId, 
                                                @RequestParam(name = "type", required = false) String type,
                                                @RequestBody(required = false) Map<String, Object> body) {
        
        // In a real scenario, you fetch the payment by dataId using PaymentClient,
        // read the external_reference, and update the booking.
        // For simplicity, if we get a payment update, we could just say OK.
        // The actual confirmation will be checked by the frontend polling or by webhook.
        if ("payment".equals(type) && dataId != null) {
            try {
                com.mercadopago.client.payment.PaymentClient paymentClient = new com.mercadopago.client.payment.PaymentClient();
                com.mercadopago.resources.payment.Payment payment = paymentClient.get(Long.parseLong(dataId));

                if ("approved".equals(payment.getStatus())) {
                    String externalRef = payment.getExternalReference();
                    if (externalRef != null) {
                        try {
                            UUID bookingId = UUID.fromString(externalRef);
                            // Confirm the purchase in the system
                            bookingService.confirmPurchase(bookingId);
                        } catch (Exception e) {
                            // ignore invalid UUID or non-existent booking silently for webhook
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore errors to respond 200 OK to MercadoPago
            }
        }
        return ResponseEntity.ok("OK");
    }
}
