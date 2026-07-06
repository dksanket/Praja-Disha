/** A single task category entry defined by the organization */
export interface OrgCategory {
  id: string;
  name: string;
  description: string;
}

/** Organization-level configuration for categories, priorities, statuses, and AI prompt rules */
export interface OrganizationConfig {
  id: string;                    // MongoDB ObjectId as string
  orgId: string;
  categories: OrgCategory[];
  priorities: string[];          // e.g. ["P0", "P1", "P2", "P3"]
  statuses: string[];            // e.g. ["OPEN", "RESOLVED", "REJECTED"]
  updatedAt: number;             // Unix timestamp (milliseconds)
  customPromptExtension: string; // Custom AI routing rules at the org level
}
