/** GeoJSON Polygon defining an organization's constituency boundary */
export interface GeoPolygon {
  type: 'Polygon';
  coordinates: number[][][]; // Standard GeoJSON coordinate array
}

/** Geographic constituency managed by the organization */
export interface OrgConstituency {
  name: string;
  coordinates: GeoPolygon;
}

/** Top-level organization entity */
export interface Organization {
  id: string;                  // MongoDB ObjectId as string
  name: string;
  createdAt: number;           // Unix timestamp (milliseconds)
  constituency: OrgConstituency;
}
