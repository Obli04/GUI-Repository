package beans.store.services;

import java.util.ArrayList;
import java.util.List;

import beans.store.models.Article;

/**
 * Service class for handling store-related operations.
 * @author - Arthur PHOMMACHANH xphomma00
 */
public class StoreService {

    private List<Article> articles = new ArrayList<>();

    public StoreService() {
        articles.add(new Article(1, "Laptop", 999.99, 10));
        articles.add(new Article(2, "Smartphone", 499.99, 20));
        articles.add(new Article(3, "Headphones", 149.99, 30));
    }

    public List<Article> getArticles() {
        return articles;
    }

    public boolean purchaseArticle(int articleId, int quantity) {
        for (Article article : articles) {
            if (article.getId() == articleId && article.getQuantity() >= quantity) {
                article.setQuantity(article.getQuantity() - quantity);
                return true;
            }
        }
        return false;
    }
}
