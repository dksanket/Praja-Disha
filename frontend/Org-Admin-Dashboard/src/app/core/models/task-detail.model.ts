/** Sub-task entry in the task detail view */
export interface SubTask {
  id: string;
  parentId?: string;
  title: string;
  role: string;
  department?: string;
  icon: string;
  status: string;
  statusClass: string;
  assignee: string;
  timestamp: string;
}

/** Officer comment displayed on the task detail page */
export interface DetailedComment {
  userName: string;
  initials: string;
  timestamp: string;
  text: string;
  isSelf: boolean;
}

/** Internal officer note (not visible to citizens) */
export interface DetailedNote {
  userName: string;
  timestamp: string;
  text: string;
}

/** Activity log entry tracking state changes on a task */
export interface DetailedActivity {
  timestamp: string;
  action: string;
  performedBy: string;
  remarks: string;
}

/** Full detail payload for the Org Admin task inspector page */
export interface TaskDetailPayload {
  id: string;
  title: string;
  priority: string;
  groupId: string;
  parentTaskId: string | null;
  orgId: string;
  createdAt: string;
  reportedBy: string;
  reporterType: string;
  description: string;
  voiceUrl: string;
  voiceDuration: string;
  category: string;
  language: string;
  location: {
    address: string;
    lat: string;
    lng: string;
  };
  imageUrl: string;
  mapUrl: string;
  mediaUrls?: string[];
  subTasks: SubTask[];
  comments: DetailedComment[];
  notes: DetailedNote[];
  activities: DetailedActivity[];
}
