package sync.slamtalk.map.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL) // null 값이 있는 필드는 제외
public class BasketballCourtDto {
    private Long courtId;
    private String courtName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String courtType;
    private String indoorOutdoor;
    private String courtSize;
    private Integer hoopCount;
    private Boolean nightLighting;
    private String openingHours;
    private String fee;
    private Boolean parkingAvailable;
    private String additionalInfo;
    private String photoUrl;

    //농구장 전체 정보 dto
    public BasketballCourtDto(Long courtId, String courtName, BigDecimal latitude, BigDecimal longitude,
                              String courtType, String indoorOutdoor, String courtSize, Integer hoopCount,
                              Boolean nightLighting, String openingHours, String fee, Boolean parkingAvailable,
                              String additionalInfo, String photoUrl) {
        this.courtId = courtId;
        this.courtName = courtName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.courtType = courtType;
        this.indoorOutdoor = indoorOutdoor;
        this.courtSize = courtSize;
        this.hoopCount = hoopCount;
        this.nightLighting = nightLighting;
        this.openingHours = openingHours;
        this.fee = fee;
        this.parkingAvailable = parkingAvailable;
        this.additionalInfo = additionalInfo;
        this.photoUrl = photoUrl;
    }

    // 농구장 간략 정보 dto
    public BasketballCourtDto(Long courtId, String courtName, BigDecimal latitude, BigDecimal longitude) {
        this.courtId = courtId;
        this.courtName = courtName;
        this.latitude = latitude;
        this.longitude = longitude;
    }
}