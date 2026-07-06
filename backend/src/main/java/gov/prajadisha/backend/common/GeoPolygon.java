package gov.prajadisha.backend.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GeoJSON Polygon. coordinates = array of linear rings, each a closed loop of [lng, lat] pairs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeoPolygon {
    private String type = "Polygon";
    private List<List<List<Double>>> coordinates;
}
