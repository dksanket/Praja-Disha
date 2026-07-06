package gov.prajadisha.backend.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GeoJSON Point. coordinates = [Longitude, Latitude].
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeoPoint {
    private String type = "Point";
    private List<Double> coordinates; // [lng, lat]

    public static GeoPoint of(double lng, double lat) {
        return new GeoPoint("Point", List.of(lng, lat));
    }
}
