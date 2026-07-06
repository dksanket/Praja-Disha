/** Represents the assignment of a task (or sub-task) to a specific officer */
export interface TaskAssignment {
  id: string;                          // MongoDB ObjectId as string
  parentAssignmentId: string | null;   // Non-null when this is a sub-task assignment
  orgId: string;
  officerUserName: string;
  departmentId: string;
  assignedByOfficerUserName: string;
  localStatus: string;                 // Status maintained by this specific assignee
  titleOverride: string;               // Overridden title for sub-task context
  assignedAt: number;                  // Unix timestamp (milliseconds)
  updatedAt: number;
  requiredTime: number;                // Estimated time to complete in days
}
