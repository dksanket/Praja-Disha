/** Public-facing comment on a task; can be posted by an officer or a citizen */
export interface TaskComment {
  id: string;           // MongoDB ObjectId as string
  taskId: string;
  userName: string;     // officerUserName or citizenUserName
  isOfficer: boolean;   // Differentiates officer vs citizen commenters
  imageUrls: string[];
  commentText: string;
  createdAt: number;    // Unix timestamp (milliseconds)
  updatedAt: number;
}
