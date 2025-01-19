package beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.bar.BarChartDataSet;
import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.bar.BarChartOptions;
import org.primefaces.model.charts.optionconfig.title.Title;
import org.primefaces.model.charts.pie.PieChartDataSet;
import org.primefaces.model.charts.pie.PieChartModel;

import beans.entities.Transaction;
import beans.entities.User;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

/** 
 * TransactionBean is a managed bean responsible for managing user transactions, 
 * particularly for fetching and returning all transactions done by a user.
 * 
 * @author xromang00
 */
@Named
@SessionScoped
public class TransactionBean implements Serializable {
    
    @PersistenceContext
    private EntityManager em;
    
    private List<Transaction> transactions;
    private BarChartModel barModel;
    private PieChartModel pieModel;
    
    @Inject
    private UserBean userBean;
    
    public BarChartModel getBarModel() {
        if (barModel == null) {
            createBarModel();
        }
        return barModel;
    }
    
    public PieChartModel getPieModel() {
        if (pieModel == null) {
            createPieModel();
        }
        return pieModel;
    }
    
    /**
     * Retrieves a list of transactions for a specific user.
     * 
     * @param userId the ID of the user whose transactions are to be retrieved.
     * @return a list of transactions associated with the specified user.
     */
    public List<Transaction> getUserTransactions(Long userId) {
        TypedQuery<Transaction> query = em.createQuery(
            "SELECT t FROM Transaction t WHERE " +
            "(t.sender.id = :userId OR t.receiver.id = :userId OR " +
            "(t.sender IS NULL AND t.receiver.id = :userId)) " +
            "ORDER BY t.transactionDate DESC", Transaction.class);
        query.setParameter("userId", userId);
        return query.getResultList();
    }
    

    /**/public void createBarModel() {
        barModel = new BarChartModel();
        ChartData data = new ChartData();
        
        // Get total spent and received for current month
        TypedQuery<Object[]> query = em.createQuery(
            "SELECT " +
            "SUM(CASE " +
            "    WHEN t.type = 'Withdraw' AND t.receiver.id = :userId THEN -t.value " +
            "    WHEN t.type = 'Transfer' AND t.sender.id = :userId THEN -t.value " +
            "    ELSE 0.0 END) as spent, " +
            "SUM(CASE " +
            "    WHEN t.type = 'Deposit' THEN t.value " +
            "    WHEN t.type = 'Transfer' AND t.receiver.id = :userId THEN t.value " +
            "    ELSE 0.0 END) as received " +
            "FROM Transaction t " +
            "WHERE EXTRACT(MONTH FROM t.transactionDate) = EXTRACT(MONTH FROM CURRENT_DATE) " +
            "AND EXTRACT(YEAR FROM t.transactionDate) = EXTRACT(YEAR FROM CURRENT_DATE) " +
            "AND (t.sender.id = :userId OR t.receiver.id = :userId)", Object[].class);
        
        query.setParameter("userId", userBean.getCurrentUser().getId());
        List<Object[]> results = query.getResultList();
        
        
        // Prepare data for the chart
        List<Number> amounts = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        if (!results.isEmpty() && results.get(0)[0] != null) {
            Double spent = ((Number) results.get(0)[0]).doubleValue();
            Double received = ((Number) results.get(0)[1]).doubleValue();
            amounts.add(spent);
            amounts.add(received);
            labels.add("Money Spent");
            labels.add("Money Received");
        }

        BarChartDataSet barDataSet = new BarChartDataSet();
        barDataSet.setLabel("Monthly Cash Flow");
        barDataSet.setData(amounts);
        
        // Set colors for spent (red) and received (green)
        List<String> bgColors = Arrays.asList(
            "rgba(255, 99, 132, 0.2)",   // Red for spent
            "rgba(75, 192, 192, 0.2)"    // Green for received
        );
        List<String> borderColors = Arrays.asList(
            "rgb(255, 99, 132)",         // Red border
            "rgb(75, 192, 192)"          // Green border
        );
        
        barDataSet.setBackgroundColor(bgColors);
        barDataSet.setBorderColor(borderColors);
        barDataSet.setBorderWidth(1);

        data.addChartDataSet(barDataSet);
        data.setLabels(labels);

        barModel.setData(data);
        
        BarChartOptions options = new BarChartOptions();
        barModel.setOptions(options);
        
        options.setResponsive(true);
        options.setMaintainAspectRatio(false);
        
        Title title = new Title();
        title.setDisplay(true);
        title.setText("Monthly Cash Flow Overview");
        title.setFontSize(16);
        options.setTitle(title);
    }/**/

    // DEBUG
    /*public void createBarModel() {
        barModel = new BarChartModel();
        ChartData data = new ChartData();
        
        // Query prelievi
        TypedQuery<Double> withdrawQuery = em.createQuery(
            "SELECT COALESCE(SUM(t.value), 0.0) FROM Transaction t " +
            "WHERE t.type = 'Withdraw' " +
            "AND t.receiver.id = :userId " +
            "AND EXTRACT(MONTH FROM t.transactionDate) = EXTRACT(MONTH FROM CURRENT_DATE) " +
            "AND EXTRACT(YEAR FROM t.transactionDate) = EXTRACT(YEAR FROM CURRENT_DATE)", 
            Double.class);
        withdrawQuery.setParameter("userId", userBean.getCurrentUser().getId());
        Double withdrawals = withdrawQuery.getSingleResult();
    
        // Query depositi
        TypedQuery<Double> depositQuery = em.createQuery(
            "SELECT COALESCE(SUM(t.value), 0.0) FROM Transaction t " +
            "WHERE t.type = 'Deposit' " +
            "AND t.receiver.id = :userId " +
            "AND EXTRACT(MONTH FROM t.transactionDate) = EXTRACT(MONTH FROM CURRENT_DATE) " +
            "AND EXTRACT(YEAR FROM t.transactionDate) = EXTRACT(YEAR FROM CURRENT_DATE)", 
            Double.class);
        depositQuery.setParameter("userId", userBean.getCurrentUser().getId());
        Double deposits = depositQuery.getSingleResult();
    
        // Query trasferimenti
        TypedQuery<Object[]> transferQuery = em.createQuery(
            "SELECT COALESCE(SUM(CASE WHEN t.sender.id = :userId THEN t.value ELSE 0.0 END), 0.0), " +
            "COALESCE(SUM(CASE WHEN t.receiver.id = :userId THEN t.value ELSE 0.0 END), 0.0) " +
            "FROM Transaction t " +
            "WHERE t.type = 'Transfer' " +
            "AND (t.sender.id = :userId OR t.receiver.id = :userId) " +
            "AND EXTRACT(MONTH FROM t.transactionDate) = EXTRACT(MONTH FROM CURRENT_DATE) " +
            "AND EXTRACT(YEAR FROM t.transactionDate) = EXTRACT(YEAR FROM CURRENT_DATE)", 
            Object[].class);
        transferQuery.setParameter("userId", userBean.getCurrentUser().getId());
        Object[] transfers = transferQuery.getSingleResult();
        
        Double transfersOut = (Double) transfers[0];
        Double transfersIn = (Double) transfers[1];
    
        // Calcolo totali
        Double totalSpent = withdrawals + transfersOut;
        Double totalReceived = deposits + transfersIn;
    
        // Preparazione dati per il grafico
        List<Number> amounts = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        amounts.add(withdrawals);
        amounts.add(transfersOut);
        amounts.add(deposits);
        amounts.add(transfersIn);
        // amounts.add(totalSpent);
        // amounts.add(totalReceived);
        labels.add("Withdrawals");
        labels.add("Transfers Out");
        labels.add("Deposits");
        labels.add("Transfers In");
        // labels.add("Money Spent");
        // labels.add("Money Received");
    
        // Configurazione BarChartDataSet
        BarChartDataSet barDataSet = new BarChartDataSet();
        barDataSet.setData(amounts);
        barDataSet.setLabel("Transaction Summary");
        
        // Colori per le barre
        List<String> bgColor = new ArrayList<>();
        bgColor.add("rgba(255, 99, 132, 0.2)"); // Rosso per spese
        bgColor.add("rgba(75, 192, 192, 0.2)"); // Verde per entrate
        barDataSet.setBackgroundColor(bgColor);
        
        List<String> borderColor = new ArrayList<>();
        borderColor.add("rgb(255, 99, 132)");
        borderColor.add("rgb(75, 192, 192)");
        barDataSet.setBorderColor(borderColor);
        barDataSet.setBorderWidth(1);
    
        // Aggiunta dati al modello
        data.addChartDataSet(barDataSet);
        data.setLabels(labels);
        barModel.setData(data);
    
        // Configurazione opzioni
        BarChartOptions options = new BarChartOptions();
        CartesianScales cScales = new CartesianScales();
        CartesianLinearAxes linearAxes = new CartesianLinearAxes();
        linearAxes.setOffset(true);
        cScales.addYAxesData(linearAxes);
        options.setScales(cScales);
        
        Title title = new Title();
        title.setDisplay(true);
        title.setText("Monthly Transaction Summary");
        options.setTitle(title);
    
        barModel.setOptions(options);
    }*/
    
    public void createPieModel() {
        pieModel = new PieChartModel();
        ChartData data = new ChartData();

        // Get all transactions and group them by type
        TypedQuery<Object[]> queryW = em.createQuery(
            "SELECT t.type, COUNT(t) FROM Transaction t WHERE t.type = 'Withdraw' AND t.receiver.id = :userId GROUP BY t.type", 
            Object[].class);
        queryW.setParameter("userId", userBean.getCurrentUser().getId());
        List<Object[]> resultsW = queryW.getResultList();

        TypedQuery<Object[]> queryD = em.createQuery(
            "SELECT t.type, COUNT(t) FROM Transaction t WHERE t.type = 'Deposit' AND t.receiver.id = :userId GROUP BY t.type", 
            Object[].class);
        queryD.setParameter("userId", userBean.getCurrentUser().getId());
        List<Object[]> resultsD = queryD.getResultList();

        TypedQuery<Object[]> queryT = em.createQuery(
            "SELECT t.type, COUNT(t) FROM Transaction t WHERE t.category = 'User Transfer' AND (t.receiver.id = :userId OR t.sender.id = :userId) GROUP BY t.type", 
            Object[].class);
        queryT.setParameter("userId", userBean.getCurrentUser().getId());
        List<Object[]> resultsT = queryT.getResultList();

        TypedQuery<Object[]> queryM = em.createQuery(
            "SELECT t.category, COUNT(t) FROM Transaction t WHERE t.category = 'Money Request' AND (t.receiver.id = :userId OR t.sender.id = :userId) GROUP BY t.category", 
            Object[].class);
        queryM.setParameter("userId", userBean.getCurrentUser().getId());
        List<Object[]> resultsM = queryM.getResultList();

        
        // Prepare data for the chart        
        PieChartDataSet dataSet = new PieChartDataSet();
        List<Number> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        // Calculate total for percentage
        long total = 0;
        for (Object[] result : resultsW) {
            Long count = ((Number) result[1]).longValue();
            total += count;
        }
        for (Object[] result : resultsD) {
            Long count = ((Number) result[1]).longValue();
            total += count;
        }
        for (Object[] result : resultsT) {
            Long count = ((Number) result[1]).longValue();
            total += count;
        }
        for (Object[] result : resultsM) {
            Long count = ((Number) result[1]).longValue();
            total += count;
        }

        // Prepare data for the chart with actual counts and percentage labels
        for (Object[] result : resultsW) {
            String type = (String) result[0];
            Long count = ((Number) result[1]).longValue();
            double percentage = (count.doubleValue() / total) * 100;
            values.add(count); // Use actual count instead of percentage
            labels.add(type + " (" + String.format("%.1f", percentage) + "%)");
        }
        for (Object[] result : resultsD) {
            String category = (String) result[0];
            Long count = ((Number) result[1]).longValue();
            double percentage = (count.doubleValue() / total) * 100;
            values.add(count); // Use actual count instead of percentage
            labels.add(category + " (" + String.format("%.1f", percentage) + "%)");
        }
        for (Object[] result : resultsT) {
            String category = (String) result[0];
            Long count = ((Number) result[1]).longValue();
            double percentage = (count.doubleValue() / total) * 100;
            values.add(count); // Use actual count instead of percentage
            labels.add(category + " (" + String.format("%.1f", percentage) + "%)");
        }
        for (Object[] result : resultsM) {
            String category = (String) result[0];
            Long count = ((Number) result[1]).longValue();
            double percentage = (count.doubleValue() / total) * 100;
            values.add(count); // Use actual count instead of percentage
            labels.add(category + " (" + String.format("%.1f", percentage) + "%)");
        }

        dataSet.setData(values);
        dataSet.setBackgroundColor(Arrays.asList(
            "rgb(75, 192, 192)",    // Red for first type
            "rgb(255, 99, 132)",    // Blue for second type
            "rgb(255, 206, 86)",     // Yellow for third type
            "rgb(54, 162, 235)"    // Green for fourth type
        ));
        dataSet.setHoverBackgroundColor(Arrays.asList(
            "rgba(75, 192, 192, 0.7)",    // Red for first type
            "rgba(255, 99, 132, 0.7)",    // Blue for second type
            "rgba(255, 206, 86, 0.7)",     // Yellow for third type
            "rgba(54, 162, 235, 0.7)"    // Green for fourth type
        ));

        data.addChartDataSet(dataSet);
        data.setLabels(labels);

        pieModel.setData(data);
    }
    

    public String getSenderDisplayName(Transaction transaction, User currentUser) {
        switch (transaction.getType()) {
            case "Withdraw":
                return "";
            case "Deposit":
                return transaction.getNameOfSender();
            case "Declined":
                return transaction.getSender().getId().equals(currentUser.getId()) ? "You" : transaction.getSender().getFirstName().concat(" " + transaction.getSender().getSecondName());
            default:
                boolean isUserTransfer = transaction.getCategory().equals("User Transfer") || transaction.getCategory().equals("Money Request");
                boolean isCurrentUserSender = transaction.getSender().getId().equals(currentUser.getId());
                return (isUserTransfer && isCurrentUserSender) ? "You" : transaction.getSender().getFirstName().concat(" " + transaction.getSender().getSecondName());
        }
    }

    /**
     * Gets the list of transactions.
     * 
     * @return the list of transactions.
     */
    public List<Transaction> getTransactions() {
        return transactions;
    }
    
    /**
     * Sets the list of transactions.
     * 
     * @param transactions the list of transactions to set.
     */
    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }
} 