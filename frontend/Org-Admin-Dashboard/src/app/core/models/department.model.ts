/** GeoJSON Polygon defining a department's jurisdiction */
export interface GeoPolygon {
  type: 'Polygon';
  coordinates: number[][][]; // Standard GeoJSON coordinate array
}

/** Geographic constituency served by the department */
export interface DepartmentConstituency {
  name: string;
  coordinates: GeoPolygon;
}

/** Represents a government department, which can be nested under a parent */
export interface Department {
  id: string;                        // MongoDB ObjectId as string
  orgId: string;
  name: string;
  parentDepartmentId: string | null; // Null for root-level ministries
  roleDescription: string;
  constituency: DepartmentConstituency;
  customPromptExtension: string;     // Custom AI routing rules for this department
}
