package userservice.service;

import userservice.entities.UserInfo;
import userservice.entities.UserInfoDto;
import userservice.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

@Service
@RequiredArgsConstructor
public class UserService
{
    @Autowired
    private final UserRepository userRepository;

    public UserInfoDto createOrUpdateUser(UserInfoDto userInfoDto){
        UnaryOperator<UserInfo> updatingUser = user -> {
            return userRepository.save(userInfoDto.transformToUserInfo());
        };

        Supplier<UserInfo> createUser = () -> {
            return userRepository.save(userInfoDto.transformToUserInfo());
        };

        UserInfo userInfo = userRepository.findByUserId(userInfoDto.getUserId())
                .map(updatingUser)
                .orElseGet(createUser);
        return new UserInfoDto(
                userInfo.getUserId(),
                userInfo.getFirstName(),
                userInfo.getLastName(),
                userInfo.getPhoneNumber(),
                userInfo.getEmail(),
                userInfo.getProfilePic()
        );
    }

    public UserInfoDto getUser(UserInfoDto userInfoDto) throws Exception{
        Optional<UserInfo> userInfoDtoOpt = userRepository.findByUserId(userInfoDto.getUserId());
        if(userInfoDtoOpt.isEmpty()){
            throw new Exception("User not found");
        }
        UserInfo userInfo = userInfoDtoOpt.get();
        return new UserInfoDto(
                userInfo.getUserId(),
                userInfo.getFirstName(),
                userInfo.getLastName(),
                userInfo.getPhoneNumber(),
                userInfo.getEmail(),
                userInfo.getProfilePic()
        );
    }
    // ✅ NAYA — userId se directly fetch
    public UserInfoDto getUserById(String userId) {
        UserInfo userInfo = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return new UserInfoDto(
                userInfo.getUserId(), userInfo.getFirstName(), userInfo.getLastName(),
                userInfo.getPhoneNumber(), userInfo.getEmail(), userInfo.getProfilePic()
        );
    }
    // ✅ NAYA — profile update (partial)
    public UserInfoDto updateUser(String userId, UserInfoDto updateDto) {
        UserInfo userInfo = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (updateDto.getFirstName() != null) userInfo.setFirstName(updateDto.getFirstName());
        if (updateDto.getLastName() != null) userInfo.setLastName(updateDto.getLastName());
        if (updateDto.getPhoneNumber() != null) userInfo.setPhoneNumber(updateDto.getPhoneNumber());
        if (updateDto.getProfilePic() != null) userInfo.setProfilePic(updateDto.getProfilePic());

        userRepository.save(userInfo);

        return new UserInfoDto(
                userInfo.getUserId(), userInfo.getFirstName(), userInfo.getLastName(),
                userInfo.getPhoneNumber(), userInfo.getEmail(), userInfo.getProfilePic()
        );
    }

}
