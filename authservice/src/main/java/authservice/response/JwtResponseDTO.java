package authservice.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JwtResponseDTO {

    private String accessToken;
    private String token;
    private String userId;
}


//this is class dto is for backned to client data client ko data bhej ne ke liye