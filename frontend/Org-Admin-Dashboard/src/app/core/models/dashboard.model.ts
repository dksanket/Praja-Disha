/** Represents an officer or department assignment visualization */
export interface TaskAssignment {
  type: 'icon' | 'avatar' | 'dept';
  value?: string; // Material icon name or image URL
  label: string; // Tooltip name/label
}

/** Represents a task shown in the Central Triage table */
export interface DashboardTask {
  id: string; // Task ID (e.g., TSK-8921)
  title: string; // Short context or title
  priority: 'P0' | 'P1' | 'P2'; // Operational priority levels
  assignments: TaskAssignment[]; // Team members or departments assigned
  dueDate: string; // Formatted due date text
  dueDateCritical: boolean; // Flag to render due date with error/critical styling
  status: string; // Formatted status text (e.g. "AI-Assigned: Review Pending")
  statusType: 'ai-pending' | 'in-progress' | 'drafting' | 'completed'; // Visual semantic grouping
  indicatorColorClass?: string; // Optional class for visual accent bar color
  groupId?: string; // Group ID for grouping duplicate tickets
  isDuplicateGroup?: boolean; // True if this task has duplicates in the list
}

/** Represents summary counters in the bento grid quick filters */
export interface DashboardStats {
  awaitingAiReviewCount: number;
  dueTodayCount: number;
  myDeptCount: number;
}
