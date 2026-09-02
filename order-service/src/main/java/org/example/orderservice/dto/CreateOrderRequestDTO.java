package org.example.orderservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "Sipariş oluşturma istek şeması")
public class CreateOrderRequestDTO {

    @NotNull
    @Schema(description = "Siparişi veren müşterinin benzersiz ID'si", example = "101")
    private Long customerId;

    @NotNull
    @Schema(description = "Satın alınacak ürünün benzersiz ID'si", example = "5002")
    private Long productId;

    @NotNull
    @Min(1)
    @Schema(description = "Satın alınacak ürün adedi", example = "3")
    private Integer quantity;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Schema(description = "Siparişin toplam fiyat tutarı", example = "1250.50")
    private BigDecimal totalPrice;
}
