/** Internal officer notes attached to a task (not visible to citizens) */
export interface TaskNote {
  id: string;              // MongoDB ObjectId as string
  taskId: string;
  officerUserName: string;
  noteText: string;
  createdAt: number;       // Unix timestamp (milliseconds)
  updatedAt: number;
}
