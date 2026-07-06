/** Represents a citizen user of the platform */
export interface Citizen {
  id: string;                   // MongoDB ObjectId as string
  citizenUserName: string;      // Unique identifier, e.g. "rahul_sharma99" or phone string
  name: string;
  email: string;
  phone: string;
  displayPictureUrl: string;    // Base64 compressed image string or cloud URL
  transitPoints: number;        // Accumulated validation points for public transport
  createdAt: number;            // Unix timestamp (milliseconds)
  updatedAt: number;
}
