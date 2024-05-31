package roomescape.infrastructure.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import roomescape.service.exception.PaymentException;
import roomescape.service.request.PaymentApproveDto;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(PaymentManager.class)
class PaymentManagerTest {

    @Autowired
    private PaymentManager paymentManager;

    @Autowired
    private MockRestServiceServer server;

    @Autowired
    private ObjectMapper objectMapper;

    @DisplayName("결제 승인을 요청하고 올바르게 응답을 반환한다.")
    @Test
    void approve() throws IOException {
        PaymentApproveDto paymentApproveDto = new PaymentApproveDto("paymentKey", "orderId", 1000L);
        String paymentApproveJson = objectMapper.writeValueAsString(paymentApproveDto);
        this.server.expect(requestTo("https://api.tosspayments.com/v1/payments/confirm"))
                .andExpect(content().json(paymentApproveJson))
                .andRespond(withSuccess(paymentApproveJson, MediaType.APPLICATION_JSON));

        PaymentApproveDto actualResponse = paymentManager.approve(paymentApproveDto);

        assertThat(actualResponse).isEqualTo(paymentApproveDto);
        this.server.verify();
    }

    @DisplayName("결제 승인 실패 시 PaymentException이 발생한다.")
    @Test
    void invalidPaymentApproveRequest() throws IOException {
        PaymentApproveDto paymentApproveDto = new PaymentApproveDto("invalidKey", "orderId", 1000L);
        String paymentApproveJson = objectMapper.writeValueAsString(paymentApproveDto);
        String responseJson = objectMapper.writeValueAsString(new RuntimeException("올바르지 않은 결제 요청입니다."));
        this.server.expect(requestTo("https://api.tosspayments.com/v1/payments/confirm"))
                .andExpect(content().json(paymentApproveJson))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).body(responseJson));

        assertThatThrownBy(() -> paymentManager.approve(paymentApproveDto))
                .isInstanceOf(PaymentException.class)
                .hasMessage("올바르지 않은 결제 요청입니다.");
        this.server.verify();
    }
}
