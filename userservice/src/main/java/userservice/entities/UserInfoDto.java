package userservice.entities;

import lombok.*;

// ✅ Sirf yeh — koi @JsonNaming nahi, koi @JsonProperty nahi
// ObjectMapper mein SnakeCaseStrategy set kar di hai deserializer mein
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoDto {

    private String userId;
    private String firstName;
    private String lastName;
    private Long phoneNumber;
    private String email;
    private String profilePic;

    public UserInfo transformToUserInfo() {
        return UserInfo.builder()
                .userId(userId)
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber(phoneNumber)
                .email(email)
                .profilePic(profilePic)
                .build();
    }
}