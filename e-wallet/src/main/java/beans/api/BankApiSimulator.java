package beans.api;

import java.time.LocalDateTime;

import beans.DepositBean;
import beans.services.PaymentInfo;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/bank-api")
public class BankApiSimulator {
    
    @Inject
    private DepositBean depositBean;
    
    @POST
    @Path("/simulate-payment")
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
            return Response.ok().build();
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST)
                         .entity(e.getMessage())
                         .build();
        }
    }
} 