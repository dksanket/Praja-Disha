/** Defines a manager-reportee relationship between officers within an organization */
export interface ReportingHierarchy {
  id: string;               // MongoDB ObjectId as string
  orgId: string;
  managerUserName: string;  // Links to Officer.officerUserName
  reporteeUserName: string; // Links to Officer.officerUserName
  assignedAt: number;       // Unix timestamp (milliseconds)
}
