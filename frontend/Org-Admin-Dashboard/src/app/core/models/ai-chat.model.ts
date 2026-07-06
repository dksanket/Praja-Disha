/**
 * Representation of a single chat message in the conversation thread.
 */
export interface ChatMessage {
  id: string;
  sender: 'ai' | 'user';
  text: string;
  timestamp: number;
  suggestions?: string[];
}
