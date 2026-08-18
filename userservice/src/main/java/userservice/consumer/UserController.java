package userservice.consumer;

import org.springframework.web.bind.annotation.*;
import userservice.entities.UserInfoDto;
import userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Tag(name = "User Profile", description = "Get and update user profile")
@RestController
@RequiredArgsConstructor
public class UserController
{

    @Autowired
    private UserService userService;

    @GetMapping("/user/v1/getUser")
    public ResponseEntity<UserInfoDto> getUser(@RequestBody UserInfoDto userInfoDto){
        try{
            UserInfoDto user = userService.getUser(userInfoDto);
            return new ResponseEntity<>(user, HttpStatus.OK);
        }catch (Exception ex){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
    // ✅ NAYA — GET /user/v1/me?userId=xxx
    @Operation(summary = "Get User Profile",
            description = "Get logged in user profile. Requires Bearer JWT token.")
    @Parameter(name = "userId", in = ParameterIn.QUERY,
            example = "f72b9ce8-d7a4-4ca6-b4d7-5eae1cf197ac",
            description = "User UUID from login response")
    @ApiResponse(responseCode = "200", description = "User profile",
            content = @Content(schema = @Schema(example = """
                {
                  "userId": "f72b9ce8-d7a4-4ca6-b4d7-5eae1cf197ac",
                  "firstName": "Kishan",
                  "lastName": "Singh",
                  "email": "kishan@gmail.com",
                  "phoneNumber": 9876543210
                }
              """)))
    @SecurityRequirement(name = "Bearer Auth")
    @GetMapping("/user/v1/me")
    public ResponseEntity<UserInfoDto> getMe(@RequestParam String userId) {
        try {
            UserInfoDto user = userService.getUserById(userId);
            return new ResponseEntity<>(user, HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }


    @PostMapping("/user/v1/createUpdate")
    public ResponseEntity<UserInfoDto> createUpdateUser(UserInfoDto userInfoDto){
        try{
            UserInfoDto user = userService.createOrUpdateUser(userInfoDto);
            return new ResponseEntity<>(user, HttpStatus.OK);
        }catch (Exception ex){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // ✅ NAYA — PUT /user/v1/update?userId=xxx
    @Operation(summary = "Update Profile",
            description = "Update user firstName, lastName, phoneNumber.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(schema = @Schema(example = """
                {
                  "firstName": "Kishan",
                  "lastName": "Singh",
                  "phoneNumber": 9876543210
                }
              """)))
    @SecurityRequirement(name = "Bearer Auth")
    @PutMapping("/user/v1/update")
    public ResponseEntity<UserInfoDto> updateUser(
            @RequestParam String userId,
            @RequestBody UserInfoDto updateDto) {
        try {
            UserInfoDto updated = userService.updateUser(userId, updateDto);
            return new ResponseEntity<>(updated, HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Boolean> checkHealth(){
        return new ResponseEntity<>(true, HttpStatus.OK);
    }

}