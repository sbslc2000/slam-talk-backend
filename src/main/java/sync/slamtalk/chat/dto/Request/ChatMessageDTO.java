package sync.slamtalk.chat.dto.Request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;


@Getter
@Setter
@AllArgsConstructor
@Builder(access = AccessLevel.PUBLIC)
public class ChatMessageDTO implements Serializable {
    @Nullable
    private String roomId; // 채팅방 아이디(채팅방 식별자)
    private String senderId; // 메세지를 보낸 사용자의 고유 식별자
    @Nullable
    private String content; // 메세지 내용
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp; // 메세지를 보낸 시간


    // 생성될 때 현재 시간을 저장
    public ChatMessageDTO(){
        this.timestamp = LocalDateTime.now();
    }
}