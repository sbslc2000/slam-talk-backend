package sync.slamtalk.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sync.slamtalk.chat.dto.ChatErrorResponseCode;
import sync.slamtalk.chat.dto.Request.ChatCreateDTO;
import sync.slamtalk.chat.dto.Request.ChatMessageDTO;
import sync.slamtalk.chat.dto.Response.ChatRoomDTO;
import sync.slamtalk.chat.entity.ChatRoom;
import sync.slamtalk.chat.entity.Messages;
import sync.slamtalk.chat.entity.RoomType;
import sync.slamtalk.chat.entity.UserChatRoom;
import sync.slamtalk.chat.redis.RedisService;
import sync.slamtalk.chat.repository.ChatRoomRepository;
import sync.slamtalk.chat.repository.MessagesRepository;
import sync.slamtalk.chat.repository.UserChatRoomRepository;
import sync.slamtalk.common.BaseException;
import sync.slamtalk.common.ErrorResponseCode;
import sync.slamtalk.map.entity.BasketballCourt;
import sync.slamtalk.map.repository.BasketballCourtRepository;
import sync.slamtalk.user.UserRepository;
import sync.slamtalk.user.entity.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService{
    private final ChatRoomRepository chatRoomRepository;
    private final MessagesRepository messagesRepository;
    private final UserChatRoomRepository userChatRoomRepository;
    private final UserRepository userRepository;
    private final BasketballCourtRepository basketballCourtRepository;

    // 채팅방 생성(농구장:미리해놓기, 그외 모두 SUBSCRIBE 시에)
    @Override
    public long createChatRoom(ChatCreateDTO chatCreateDTO) {
        // chatRoom create
        if(chatCreateDTO.getRoomType().equals(RoomType.DIRECT)){
            ChatRoom chatRoom = ChatRoom.builder()
                    .roomType(typeOfRoom(chatCreateDTO))
                    .build();
            ChatRoom save = chatRoomRepository.save(chatRoom);
        }

        if(chatCreateDTO.getRoomType().equals(RoomType.MATCHING)){
            ChatRoom chatRoom = ChatRoom.builder()
                    .roomType(typeOfRoom(chatCreateDTO))
                    .name(chatCreateDTO.getName()) // "호랑이팀 vs 거북이팀" 이런식으로
                    .build();
            ChatRoom save = chatRoomRepository.save(chatRoom);
        }

        if(chatCreateDTO.getRoomType().equals(RoomType.BASKETBALL)){
            ChatRoom chatRoom = ChatRoom.builder()
                    .roomType(typeOfRoom(chatCreateDTO))
                    .name(chatCreateDTO.getName())
                    .build();
            ChatRoom save = chatRoomRepository.save(chatRoom);

        }
        if(chatCreateDTO.getRoomType().equals(RoomType.TOGETHER)){
            ChatRoom chatRoom = ChatRoom.builder()
                    .roomType(typeOfRoom(chatCreateDTO))
                    .name(chatCreateDTO.getName()) // 게시글 이름?
                    .build();
            ChatRoom save = chatRoomRepository.save(chatRoom);
        }


        ChatRoom chatRoom = ChatRoom.builder()
                .roomType(typeOfRoom(chatCreateDTO))
                .name(nameOfRoom(chatCreateDTO))
                .build();
        ChatRoom save = chatRoomRepository.save(chatRoom);
        return save.getId();
    }


    // 채팅방 입장(STOMP: SUBSCRIBE)
    // 매 접속시 실행!
    @Override
    public Long setUserChatRoom(Long userId, Long chatRoomId) {

        // userChatRoom 에 이미 있으면 바로 종료
        Optional<UserChatRoom> userChatRoomOptional = userChatRoomRepository.findByUserChatroom(userId, chatRoomId);
        if(userChatRoomOptional.isPresent()){
            UserChatRoom userChatRoom = userChatRoomOptional.get();
            userChatRoom.updateIsFirst(false);
            log.debug("Exist already");
            return 1L;// 이미 접속한 적이 있다는 flag
        }

        // user 가져오기
        Optional<User> userOptional = userRepository.findById(userId);

        // chatRoom 가져오기
        // UserChatRoom 추가하기
        Optional<ChatRoom> chatRoomOptional = chatRoomRepository.findById(chatRoomId);
        if(chatRoomOptional.isPresent()) {
            ChatRoom chatRoom = chatRoomOptional.get();

            // UserChatRoom 생성
            UserChatRoom userChatRoom = UserChatRoom.builder()
                    .readIndex(0L) // 초기화
                    .isFirst(true) // 초기화
                    .roomType(chatRoom.getRoomType())
                    .chat(chatRoom)
                    .user(userOptional.get())
                    .build();
            // 저장
            userChatRoomRepository.save(userChatRoom);
            // ChatRoom 에도 userchatRoom 추가
            chatRoom.addUserChatRoom(userChatRoom);
        }
        log.debug("처음 접속");
        return 0L;// 처음 접속했다는 flag
    }


    // 채팅방에 메세지 저장(STOMP: SEND)
    @Override
    public void saveMessage(ChatMessageDTO chatMessageDTO) {
        long chatRoomId = Long.parseLong(chatMessageDTO.getRoomId());
        Optional<ChatRoom> chatRoom = chatRoomRepository.findById(chatRoomId);
        if(chatRoom.isPresent()){ // chatRoom 이 존재하면
            ChatRoom Room = chatRoom.get();
            // Message create
            Messages messages = Messages.builder()
                    .chatRoom(Room)
                    .senderId(chatMessageDTO.getSenderId())
                    .senderNickname(chatMessageDTO.getSenderNickname())
                    .content(chatMessageDTO.getContent())
                    .creation_time(chatMessageDTO.getTimestamp().toString())
                    .build();
            messagesRepository.save(messages);
        }else{
            throw new BaseException(ErrorResponseCode.CHAT_FAIL);
        }
    }

    // 존재하는 방인지 확인
    @Override
    public Optional<ChatRoom> isExistChatRoom(Long chatRoomId) {
        Optional<ChatRoom> chatRoom = chatRoomRepository.findById(chatRoomId);
        if (chatRoom.isPresent()){
            return chatRoom;
        }
        return Optional.empty();

    }

    // 사용자 채팅방에 존재 하는 방인지 확인
    @Override
    public Optional<UserChatRoom> isExistUserChatRoom(Long userId,Long chatRoomId) {

        Optional<UserChatRoom> userChatRoom = userChatRoomRepository.findByUserChatroom(userId, chatRoomId);
        if(userChatRoom.isPresent()){
            return userChatRoom;
        }
        return Optional.empty();
    }


    // 채팅리스트 가져오기
    @Override
    public List<ChatRoomDTO> getChatLIst(Long userId) {
        List<ChatRoomDTO> chatRooms = new ArrayList<>();

        // 유저가 가지고 있는 채팅방 모두 가져오기
        log.debug("userId:{}",userId);
        List<UserChatRoom> chatRoom = userChatRoomRepository.findByUser_Id(userId);
        if(chatRoom.isEmpty()){
            log.debug("nothing");
            return null;
        }

        // 유저가 가지고 있는 채팅방 리스트를 돌면서 가져오기
        for(UserChatRoom ucr : chatRoom){

            boolean isDelete = ucr.getIsDeleted().booleanValue();
            log.debug("isDelete:{}",isDelete);


            // 삭제 되지 않은 채팅방만 가져옴
            if(!isDelete){

                String profile = null;

                // 1:1 인 경우 상대방 프로필
                // 1:1, 팀매칭만 상대방 프로필 나머지(같이하기, 농구장은 디폴트 프로필)
                if(ucr.getRoomType().equals(RoomType.DIRECT) || ucr.getRoomType().equals(RoomType.MATCHING)){
                    List<UserChatRoom> optionalList = userChatRoomRepository.findByChat_Id(ucr.getChat().getId());
                    for(UserChatRoom x : optionalList){
                        if(!x.getUser().getId().equals(userId) && x.getIsDeleted().equals(false)){
                            // 자기 자신이 아니고, 삭제가 되지 않은 경우
                            profile = x.getImageUrl();
                        }
                    }
                }

                ChatRoomDTO dto = ChatRoomDTO.builder()
                        .roomId(ucr.getId().toString())
                        .roomType(ucr.getRoomType().toString())
                        .imgUrl(profile)
                        .name(ucr.getChat().getName())
                        .roomId(ucr.getChat().getId().toString())
                        .build();

                // 마지막 메세지
                PageRequest pageRequest = PageRequest.of(0, 1);
                Page<Messages> latestByChatRoomId = messagesRepository.findLatestByChatRoomId(ucr.getChat().getId(), pageRequest);
                Stream<Messages> messagesStream = latestByChatRoomId.get();
                Messages messages = messagesStream.collect(Collectors.toList()).get(0);

                dto.setLast_message(messages.getContent());
                chatRooms.add(dto);
            }
        }
        return chatRooms;
    }


    // 특정 방에서 주고 받은 모든 메세지 가져오기
    @Override
    public List<ChatMessageDTO> getChatMessage(Long chatRoomId, Long messageId) {
        List<ChatMessageDTO> ansList = new ArrayList<>();

        // 특정 방 메세지 중 현재 messageId 보다 큰 Id값을 가진 메세지들 가져오기
        List<Messages> newMessages = messagesRepository.findByChatRoomIdAndIdGreaterThan(chatRoomId, messageId);


        // messageRepository 에서 가져온 메세지로 dto 생성하기
        for(Messages m : newMessages){

            // 작성자 이미지 가져오기
            Long senderId = m.getSenderId();
            Optional<User> optionalUser = userRepository.findById(senderId);
            String imageUrl = "";
            if(optionalUser.isPresent()){
                User user = optionalUser.get();
                imageUrl = user.getImageUrl();
            }else{
                imageUrl = "null";
            }
            ChatMessageDTO messageDTO = ChatMessageDTO.builder()
                    .messageId(m.getId().toString())
                    .roomId(m.getChatRoom().getId().toString())
                    .senderId(m.getSenderId())
                    .senderNickname(m.getSenderNickname())
                    .imgUrl(imageUrl)
                    .content(m.getContent())
                    .timestamp(m.getCreation_time())
                    .build();
            ansList.add(messageDTO);
        }
        return ansList;
    }


    // 특정 방에 저장된 메세지 중 가장 마지막 메세지 가져옴
    @Override
    public Messages getLastMessageFromChatRoom(Long chatRoomId) {

        List<Messages> allByChatRoom = messagesRepository.findAllByChatRoom(chatRoomId);
        return allByChatRoom.get(0);
    }

    // userChatRoom 에 readIndex 저장하기
    @Override
    public void saveReadIndex(Long userId,Long chatRoomId,Long readIndex) {

        Optional<UserChatRoom> matchingChatRoom = userChatRoomRepository.findByUserChatroom(userId, chatRoomId);

        // 해당하는 userchatRoom 의 readIndex 에 readIndex 를 업데이트
        if(matchingChatRoom.isPresent()){
            UserChatRoom userChatRoom = matchingChatRoom.get();
            userChatRoom.updateReadIndex(readIndex);
        }
    }


    // 특정방을 나갈 때 userChatRoom softDelete
    @Override
    public Optional<UserChatRoom> exitRoom(Long userId, Long chatRoomId) {
        Optional<UserChatRoom> optionalUserChatRoom = userChatRoomRepository.findByUserChatroom(userId, chatRoomId);

        // 사용자 채팅방 가져와서 softDelete = true 처리
        if(optionalUserChatRoom.isPresent()){
            UserChatRoom userChatRoom = optionalUserChatRoom.get();
            userChatRoom.delete();
            return optionalUserChatRoom;
        }

        return Optional.empty();
    }





    // ChatRoom 타입
    private RoomType typeOfRoom(ChatCreateDTO dto){
        if(dto.getRoomType().startsWith("D")){
            return RoomType.DIRECT;
        }
        if(dto.getRoomType().startsWith("B")){
            return RoomType.BASKETBALL;
        }
        if(dto.getRoomType().startsWith("M")){
            return RoomType.MATCHING;
        }
        return RoomType.TOGETHER;
    }


    // ChatRoom 이름
    private String nameOfRoom(ChatCreateDTO dto){
        // 같이하기
        if(dto.getRoomType().startsWith("T")){
            return RoomType.TOGETHER.getKey();
        }
        // 1:1, 농구장, 팀매칭 -> 이름 하나
        return dto.getName();
    }


    /**
     * 팀매칭/상대팀 매칭완료 시 채팅방 생성 후 해당 채팅방 id에 User를 매핑 시켜주는 메서드
     *
     * @param chatRoomId 채팅방Id
     * @param usersId 채팅방에 포함될 유저ID LIST
     * */
    @Override
    @Transactional
    public void setUserListChatRoom(Long chatRoomId, List<Long> usersId) {
        log.debug("팀매칭 완료 시 채팅방 유저 생성 로직");

        // chatRoom 가져오기
        // UserChatRoom 추가하기
        Optional<ChatRoom> chatRoomOptional = chatRoomRepository.findById(chatRoomId);
        if(chatRoomOptional.isPresent()) {
            ChatRoom chatRoom = chatRoomOptional.get();

            List<User> users = userRepository.findAllById(usersId);

            List<UserChatRoom> userChatRooms = users.stream().map(user -> {
                // UserChatRoom 생성
                UserChatRoom userChatRoom = UserChatRoom.builder()
                        .readIndex(0L) // 초기화
                        .isFirst(true) // 초기화
                        .chat(chatRoom)
                        .user(user)
                        .build();

                // ChatRoom 에도 userchatRoom 추가
                chatRoom.addUserChatRoom(userChatRoom);
                return userChatRoom;
            }).toList();

            // 저장
            userChatRoomRepository.saveAll(userChatRooms);
        } else{
            log.debug("채팅방이 존재하지 않습니다.");
            // 채팅방이 존재하지 않을때 REST API 커스텀 에러 반환
            throw new BaseException(ErrorResponseCode.CHAT_FAIL);
        }
    }
}
