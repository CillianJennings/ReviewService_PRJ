package ie.atu.reviewservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReviewResponse {
    private Long id;
    private String restaurantName;
    private String userName;
    private int rating;
    private String reviewText;
}
