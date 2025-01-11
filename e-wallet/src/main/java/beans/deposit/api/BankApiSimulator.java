package beans.deposit.api;

import java.time.LocalDateTime;

import beans.deposit.DepositBean;
import beans.deposit.services.PaymentInfo;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST API endpoint that simulates a bank's payment notification system.
 * Handles incoming payment requests from the bank simulator and processes them
 *
 * @author Danilo Spera
 */
@Path("/bank-api")
public class BankApiSimulator {
    
    /** Deposit bean for handling payment notifications */
    @Inject
    private DepositBean depositBean;
    
    /**
     * Processes a simulated payment request from the bank simulator.
     * Converts the payment data and forwards it to the deposit system.
     *
     * @param payment The payment information received from bank simulator
     * @return Response with success or error message in JSON format
     */
    @POST
    @Path("/simulate-payment")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response simulatePayment(SimulatedPayment payment) {
        try {
            PaymentInfo paymentInfo = new PaymentInfo(
                payment.getSenderAccount(),
                payment.getReceiverAccount(),
                payment.getAmount(),
                LocalDateTime.now(),
                payment.getVariableSymbol()
            );
            
            depositBean.handlePaymentNotification(paymentInfo);
            
            return Response.ok()
                          .entity("{\"status\":\"success\",\"message\":\"Payment processed successfully\"}")
                          .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                          .entity("{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}")
                          .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                          .entity("{\"status\":\"error\",\"message\":\"Internal server error: " + e.getMessage() + "\"}")
                          .build();
        }
    }
} 