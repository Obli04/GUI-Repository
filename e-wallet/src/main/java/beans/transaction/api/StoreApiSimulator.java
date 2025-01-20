package beans.transaction.api;

import beans.transaction.services.TransactionHandler;
import beans.transaction.api.SimulatedTransaction;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST API endpoint for simulating store transactions.
 * @author Arthur PHOMMACHANH - xphomma00
 */
@Path("/store-api")
public class StoreApiSimulator {

    @Inject
    private TransactionHandler transactionHandler;

    /**
     * Processes a simulated store transaction.
     *
     * @param transaction The transaction data received from the frontend.
     * @return Response indicating success or failure.
     */
    @POST
    @Path("/process-transaction")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response processTransaction(SimulatedTransaction transaction) {
        try {
            transactionHandler.handleTransaction(transaction);
            return Response.ok("{\"status\":\"success\",\"message\":\"Transaction processed successfully\"}")
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
