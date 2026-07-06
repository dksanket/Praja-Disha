/** Audit log entry for a task lifecycle event */
export interface TaskActivity {
  id: string;           // MongoDB ObjectId as string
  taskId: string;
  timestamp: number;    // Unix timestamp (milliseconds)
  /** Action type, e.g. "AI_ASSIGNED" | "STATUS_CHANGED" | "DELEGATED" */
  action: string;
  /** Alphanumeric username of the actor, e.g. "system_ai" or "kiran_kumar" */
  performedBy: string;
  remarks: string;
}
