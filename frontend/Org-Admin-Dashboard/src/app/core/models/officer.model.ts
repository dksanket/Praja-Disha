/** Represents a government officer within an organization */
export interface Officer {
  id: string;                 // MongoDB ObjectId as string
  orgIds: string[];           // Organizations the officer belongs to
  officerUserName: string;    // Unique alphanumeric business key (e.g., "kiran_kumar")
  name: string;
  email: string;
  phone: string;
  departmentIds: string[];    // Officer can hold roles across multiple departments
  isActive: boolean;
  managerUserNames?: string[]; // Usernames of managers this officer reports to (local reference for prototype)
  createdAt: number;          // Unix timestamp (milliseconds)
}
