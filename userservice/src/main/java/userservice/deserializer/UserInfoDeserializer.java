package userservice.deserializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.apache.kafka.common.serialization.Deserializer;
import userservice.entities.UserInfoDto;

import java.util.Map;

public class UserInfoDeserializer implements Deserializer<UserInfoDto> {

    // ✅ Static ObjectMapper — har message pe new mat banao
    private static final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {}

    @Override
    public UserInfoDto deserialize(String topic, byte[] data) {
        if (data == null) return null;
        try {
            return mapper.readValue(data, UserInfoDto.class);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Deserialization failed for topic: " + topic);
            return null;
        }
    }

    @Override
    public void close() {}
}