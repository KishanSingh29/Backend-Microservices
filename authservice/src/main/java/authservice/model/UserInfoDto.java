package authservice.model;

import lombok.*;

// SIRF yeh rakho — koi @JsonNaming, koi @JsonProperty nahi
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserInfoDto {

    private String username;
    private String password;

    // Profile fields jo Kafka pe jayenge
    private String userId;
    private String firstName;
    private String lastName;
    private Long phoneNumber;
    private String email;
    private String profilePic;
}