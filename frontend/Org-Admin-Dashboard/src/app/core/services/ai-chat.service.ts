import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { ChatMessage } from '../models/ai-chat.model';

/**
 * AiChatService — manages the conversation thread state for the AI Assistant.
 * Simulates AI typing indicators and returns pre-baked responses containing
 * both raw URLs and Markdown-style links to test the LinkifyDirective.
 */
@Injectable({
  providedIn: 'root',
})
export class AiChatService {
  private readonly messagesSubject = new BehaviorSubject<ChatMessage[]>([]);
  readonly messages$ = this.messagesSubject.asObservable();

  private readonly isAiTypingSubject = new BehaviorSubject<boolean>(false);
  readonly isAiTyping$ = this.isAiTypingSubject.asObservable();

  constructor() {
    this.initializeWelcomeMessage();
  }

  private initializeWelcomeMessage(): void {
    const welcomeMessage: ChatMessage = {
      id: 'msg-welcome',
      sender: 'ai',
      text: "Hello! I'm Praja Disha AI. I have access to your latest surveys, community feedback, and infrastructure reports.\n\nHow can I help you analyze your local government data today? You can ask me things like:",
      timestamp: Date.now(),
      suggestions: [
        'Summarize the main concerns from the Q3 town hall survey.',
        'Show me the trend in road maintenance requests over the last 6 months.',
        'Compare resident satisfaction rates between District A and District B.',
      ],
    };
    this.messagesSubject.next([welcomeMessage]);
  }

  /**
   * Returns the stream of messages.
   */
  getMessages(): Observable<ChatMessage[]> {
    return this.messages$;
  }

  /**
   * Appends a user message to the thread and triggers a simulated AI response.
   */
  sendMessage(text: string): void {
    if (!text.trim()) return;

    // 1. Append User Message
    const userMsg: ChatMessage = {
      id: `msg-user-${Date.now()}`,
      sender: 'user',
      text: text.trim(),
      timestamp: Date.now(),
    };

    const currentMsgs = this.messagesSubject.value;
    this.messagesSubject.next([...currentMsgs, userMsg]);

    // 2. Set AI Typing indicator
    this.isAiTypingSubject.next(true);

    // 3. Simulate AI reply after a 1.2-second delay
    setTimeout(() => {
      let aiText = '';
      const query = text.trim().toLowerCase();

      if (query.includes('summarize the main concerns') || query.includes('q3 town hall')) {
        aiText = "Based on the Q3 town hall survey (450 residents polled), the top three concerns are:\n\n1. **Road Maintenance (38% of complaints)**: Significant potholes and slow repair times on Main Street and Maple Avenue. See [Road Maintenance Dashboard](http://localhost:4200/manage-team).\n2. **Public Transit Frequency (27%)**: Requests for additional bus routes during peak commuter hours. See [Transit Quality Guidelines](https://example.com/transit-guidelines).\n3. **Park Safety & Lighting (19%)**: Poor illumination in Central Park after sunset.\n\nWould you like me to pull the raw comments regarding any of these topics?";
      } else if (query.includes('trend in road maintenance') || query.includes('last 6 months')) {
        aiText = "Here is the trend for road maintenance requests (January - June 2026):\n\n* **January**: 85 requests\n* **February**: 92 requests\n* **March**: 110 requests (Spring thaw surge)\n* **April**: 135 requests (Peak reporting period)\n* **May**: 95 requests (Repair crew deployment)\n* **June**: 70 requests (Post-repaired status)\n\nOverall, request volume spiked in April but has decreased by 48% over the last two months due to the accelerated road repair initiative. You can read the full report here: http://localhost:4200/reports/road-maintenance.";
      } else if (query.includes('compare resident satisfaction') || query.includes('district a and district b')) {
        aiText = "In the latest municipal review, resident satisfaction rates show the following breakdown:\n\n* **District A (North/Industrial)**:\n  * Overall Satisfaction: 68%\n  * Main strengths: High safety ratings, reliable waste collection.\n  * Main issues: Noise levels, road quality.\n\n* **District B (South/Residential)**:\n  * Overall Satisfaction: 82%\n  * Main strengths: Excellent public parks, active community centers.\n  * Main issues: High local property taxes, limited parking near retail zone.\n\nDistrict B has a 14% higher overall satisfaction rating, primarily driven by park quality and community facilities. Detailed charts are available at: http://localhost:4200/analytics/satisfaction.";
      } else {
        aiText = `I've analyzed your question: "${text}" and scanned the municipal records. I can confirm we have relevant logs and survey data matching your query. However, in this prototype interface, I can only provide structured answers to the example questions. Feel free to click one of the suggested prompts above or type one of them to see a live analysis demonstration!`;
      }

      const aiMsg: ChatMessage = {
        id: `msg-ai-${Date.now()}`,
        sender: 'ai',
        text: aiText,
        timestamp: Date.now(),
      };

      this.isAiTypingSubject.next(false);
      this.messagesSubject.next([...this.messagesSubject.value, aiMsg]);
    }, 1200);
  }
}
